/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.internal;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.hibernate.AssertionFailure;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.MappingException;
import org.hibernate.NonUniqueObjectException;
import org.hibernate.PersistentObjectException;
import org.hibernate.TransientObjectException;
import org.hibernate.action.spi.AfterTransactionCompletionProcess;
import org.hibernate.bytecode.enhance.spi.interceptor.BytecodeLazyAttributeInterceptor;
import org.hibernate.bytecode.enhance.spi.interceptor.EnhancementAsProxyLazinessInterceptor;
import org.hibernate.bytecode.enhance.spi.interceptor.LazyAttributeLoadingInterceptor;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.loading.internal.LoadContexts;
import org.hibernate.engine.spi.AssociationKey;
import org.hibernate.engine.spi.BatchFetchQueue;
import org.hibernate.engine.spi.CachedNaturalIdValueSource;
import org.hibernate.engine.spi.CollectionEntry;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.EntityUniqueKey;
import org.hibernate.engine.spi.ManagedEntity;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.spi.EventSource;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.collections.ConcurrentReferenceHashMap;
import org.hibernate.internal.util.collections.IdentityMap;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.stat.internal.StatsHelper;
import org.hibernate.stat.spi.StatisticsImplementor;
import org.hibernate.type.CollectionType;

import org.jboss.logging.Logger;

/**
 * A <strong>stateful</strong> implementation of the {@link PersistenceContext} contract meaning that we maintain this
 * state throughout the life of the persistence context.
 * <p/>
 * IMPL NOTE: There is meant to be a one-to-one correspondence between a {@link org.hibernate.internal.SessionImpl}
 * and a PersistentContext.  Event listeners and other Session collaborators then use the PersistentContext to drive
 * their processing.
 *
 * @author Steve Ebersole
 * @author Sanne Grinovero
 */
public class StatefulPersistenceContext implements PersistenceContext {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			StatefulPersistenceContext.class.getName()
	);

	private static final int INIT_COLL_SIZE = 8;

	/*
		Eagerly Initialized Fields
		the following fields are used in all circumstances, and are not worth (or not suited) to being converted into lazy
	 */
	private SharedSessionContractImplementor session;
	private EntityEntryContext entityEntryContext;

	/*
		Everything else below should be carefully initialized only on first need;
		this optimisation is very effective as null checks are free, while allocation costs
		are very often the dominating cost of an application using ORM.
		This is not general advice, but it's worth the added maintenance burden in this case
		as this is a very central component of our library.
	 */

	// Loaded entity instances, by EntityKey
	private HashMap<EntityKey, Object> entitiesByKey;

	// Loaded entity instances, by EntityUniqueKey
	private HashMap<EntityUniqueKey, Object> entitiesByUniqueKey;

	// Entity proxies, by EntityKey
	private ConcurrentReferenceHashMap<EntityKey, Object> proxiesByKey;

	// Snapshots of current database state for entities
	// that have *not* been loaded
	private HashMap<EntityKey, Object> entitySnapshotsByKey;

	// Identity map of array holder ArrayHolder instances, by the array instance
	private IdentityHashMap<Object, PersistentCollection> arrayHolders;

	// Identity map of CollectionEntry instances, by the collection wrapper
	private IdentityMap<PersistentCollection, CollectionEntry> collectionEntries;

	// Collection wrappers, by the CollectionKey
	private HashMap<CollectionKey, PersistentCollection> collectionsByKey;

	// Set of EntityKeys of deleted objects
	private HashSet<EntityKey> nullifiableEntityKeys;

	// properties that we have tried to load, and not found in the database
	private HashSet<AssociationKey> nullAssociations;

	// A list of collection wrappers that were instantiating during result set
	// processing, that we will need to initialize at the end of the query
	private ArrayList<PersistentCollection> nonlazyCollections;

	// A container for collections we load up when the owning entity is not
	// yet loaded ... for now, this is purely transient!
	private HashMap<CollectionKey,PersistentCollection> unownedCollections;

	// Parent entities cache by their child for cascading
	// May be empty or not contains all relation
	private IdentityHashMap<Object,Object> parentsByChild;

	private int cascading;
	private int loadCounter;
	private int removeOrphanBeforeUpdatesCounter;
	private boolean flushing;

	private boolean defaultReadOnly;
	private boolean hasNonReadOnlyEntities;

	private LoadContexts loadContexts;
	private BatchFetchQueue batchFetchQueue;

	/**
	 * Constructs a PersistentContext, bound to the given session.
	 *
	 * @param session The session "owning" this context.
	 */
	public StatefulPersistenceContext(SharedSessionContractImplementor session) {
		this.session = session;
		this.entityEntryContext = new EntityEntryContext( this );
	}

	private ConcurrentMap<EntityKey, Object> getOrInitializeProxiesByKey() {
		if ( proxiesByKey == null ) {
			//noinspection unchecked
			proxiesByKey = new ConcurrentReferenceHashMap<>(
					INIT_COLL_SIZE,
					.75f,
					1,
					ConcurrentReferenceHashMap.ReferenceType.STRONG,
					ConcurrentReferenceHashMap.ReferenceType.WEAK,
					null
			);
		}
		return proxiesByKey;
	}

	@Override
	public boolean isStateless() {
		return false;
	}

	@Override
	public SharedSessionContractImplementor getSession() {
		return session;
	}

	@Override
	public LoadContexts getLoadContexts() {
		if ( loadContexts == null ) {
			loadContexts = new LoadContexts( this );
		}
		return loadContexts;
	}

	@Override
	public void addUnownedCollection(CollectionKey key, PersistentCollection collection) {
		if ( unownedCollections == null ) {
			unownedCollections = new HashMap<>( INIT_COLL_SIZE );
		}
		unownedCollections.put( key, collection );
	}

	@Override
	public PersistentCollection useUnownedCollection(CollectionKey key) {
		return ( unownedCollections == null ) ? null : unownedCollections.remove( key );
	}

	@Override
	public BatchFetchQueue getBatchFetchQueue() {
		if ( batchFetchQueue == null ) {
			batchFetchQueue = new BatchFetchQueue( this );
		}
		return batchFetchQueue;
	}

	@Override
	public void clear() {
		if ( proxiesByKey != null ) {
			proxiesByKey.forEach( (k,o) -> {
				if ( o != null) {
					((HibernateProxy) o).getHibernateLazyInitializer().unsetSession();
				}
			} );
		}

		for ( Entry<Object, EntityEntry> objectEntityEntryEntry : entityEntryContext.reentrantSafeEntityEntries() ) {
			if ( objectEntityEntryEntry.getKey() instanceof PersistentAttributeInterceptable ) {
				final PersistentAttributeInterceptor interceptor = ( (PersistentAttributeInterceptable) objectEntityEntryEntry.getKey() ).$$_hibernate_getInterceptor();
				if ( interceptor instanceof LazyAttributeLoadingInterceptor ) {
					( (LazyAttributeLoadingInterceptor) interceptor ).unsetSession();
				}
			}
		}

		final SharedSessionContractImplementor session = getSession();
		if ( collectionEntries != null ) {
			IdentityMap.onEachKey( collectionEntries, k -> k.unsetSession( session ) );
		}

		arrayHolders = null;
		entitiesByKey = null;
		entitiesByUniqueKey = null;
		entityEntryContext.clear();
		parentsByChild = null;
		entitySnapshotsByKey = null;
		collectionsByKey = null;
		nonlazyCollections = null;
		collectionEntries = null;
		unownedCollections = null;
		proxiesByKey = null;
		nullifiableEntityKeys = null;
		if ( batchFetchQueue != null ) {
			batchFetchQueue.clear();
		}
		// defaultReadOnly is unaffected by clear()
		hasNonReadOnlyEntities = false;
		if ( loadContexts != null ) {
			loadContexts.cleanup();
		}
		naturalIdXrefDelegate = null;
	}

	@Override
	public boolean isDefaultReadOnly() {
		return defaultReadOnly;
	}

	@Override
	public void setDefaultReadOnly(boolean defaultReadOnly) {
		this.defaultReadOnly = defaultReadOnly;
	}

	@Override
	public boolean hasNonReadOnlyEntities() {
		return hasNonReadOnlyEntities;
	}

	@Override
	public void setEntryStatus(EntityEntry entry, Status status) {
		entry.setStatus( status );
		setHasNonReadOnlyEnties( status );
	}

	private void setHasNonReadOnlyEnties(Status status) {
		if ( status==Status.DELETED || status==Status.MANAGED || status==Status.SAVING ) {
			hasNonReadOnlyEntities = true;
		}
	}

	@Override
	public void afterTransactionCompletion() {
		cleanUpInsertedKeysAfterTransaction();
		// Downgrade locks
		entityEntryContext.downgradeLocks();
	}

	/**
	 * Get the current state of the entity as known to the underlying
	 * database, or null if there is no corresponding row
	 * <p/>
	 * {@inheritDoc}
	 */
	@Override
	public Object[] getDatabaseSnapshot(Serializable id, EntityPersister persister) throws HibernateException {
		final EntityKey key = session.generateEntityKey( id, persister );
		final Object cached = entitySnapshotsByKey == null ? null : entitySnapshotsByKey.get( key );
		if ( cached != null ) {
			return cached == NO_ROW ? null : (Object[]) cached;
		}
		else {
			final Object[] snapshot = persister.getDatabaseSnapshot( id, session );
			if ( entitySnapshotsByKey == null ) {
				entitySnapshotsByKey = new HashMap<>( INIT_COLL_SIZE );
			}
			entitySnapshotsByKey.put( key, snapshot == null ? NO_ROW : snapshot );
			return snapshot;
		}
	}

	@Override
	public Object[] getNaturalIdSnapshot(Serializable id, EntityPersister persister) throws HibernateException {
		if ( !persister.hasNaturalIdentifier() ) {
			return null;
		}

		persister = locateProperPersister( persister );

		// let's first see if it is part of the natural id cache...
		final Object[] cachedValue = naturalIdHelper.findCachedNaturalId( persister, id );
		if ( cachedValue != null ) {
			return cachedValue;
		}

		// check to see if the natural id is mutable/immutable
		if ( persister.getEntityMetamodel().hasImmutableNaturalId() ) {
			// an immutable natural-id is not retrieved during a normal database-snapshot operation...
			final Object[] dbValue = persister.getNaturalIdentifierSnapshot( id, session );
			naturalIdHelper.cacheNaturalIdCrossReferenceFromLoad(
					persister,
					id,
					dbValue
			);
			return dbValue;
		}
		else {
			// for a mutable natural id there is a likelihood that the information will already be
			// snapshot-cached.
			final int[] props = persister.getNaturalIdentifierProperties();
			final Object[] entitySnapshot = getDatabaseSnapshot( id, persister );
			if ( entitySnapshot == NO_ROW || entitySnapshot == null ) {
				return null;
			}

			final Object[] naturalIdSnapshotSubSet = new Object[ props.length ];
			for ( int i = 0; i < props.length; i++ ) {
				naturalIdSnapshotSubSet[i] = entitySnapshot[ props[i] ];
			}
			naturalIdHelper.cacheNaturalIdCrossReferenceFromLoad(
					persister,
					id,
					naturalIdSnapshotSubSet
			);
			return naturalIdSnapshotSubSet;
		}
	}

	private EntityPersister locateProperPersister(EntityPersister persister) {
		return session.getFactory().getMetamodel().entityPersister( persister.getRootEntityName() );
	}

	@Override
	public Object[] getCachedDatabaseSnapshot(EntityKey key) {
		final Object snapshot = entitySnapshotsByKey == null ? null : entitySnapshotsByKey.get( key );
		if ( snapshot == NO_ROW ) {
			throw new IllegalStateException(
					"persistence context reported no row snapshot for "
							+ MessageHelper.infoString( key.getEntityName(), key.getIdentifier() )
			);
		}
		return (Object[]) snapshot;
	}

	@Override
	public void addEntity(EntityKey key, Object entity) {
		if ( entitiesByKey == null ) {
			entitiesByKey = new HashMap<>( INIT_COLL_SIZE );
		}
		entitiesByKey.put( key, entity );
		final BatchFetchQueue fetchQueue = this.batchFetchQueue;
		if ( fetchQueue != null ) {
			fetchQueue.removeBatchLoadableEntityKey( key );
		}
	}

	@Override
	public Object getEntity(EntityKey key) {
		return entitiesByKey == null ? null : entitiesByKey.get( key );
	}

	@Override
	public boolean containsEntity(EntityKey key) {
		return entitiesByKey == null ? false : entitiesByKey.containsKey( key );
	}

	@Override
	public Object removeEntity(EntityKey key) {
		final Object entity;
		if ( entitiesByKey != null ) {
			entity = entitiesByKey.remove( key );
			if ( entitiesByUniqueKey != null ) {
				final Iterator itr = entitiesByUniqueKey.values().iterator();
				while ( itr.hasNext() ) {
					if ( itr.next() == entity ) {
						itr.remove();
					}
				}
			}
		}
		else {
			entity = null;
		}

		// Clear all parent cache
		parentsByChild = null;
		if ( entitySnapshotsByKey != null ) {
			entitySnapshotsByKey.remove( key );
		}
		if ( nullifiableEntityKeys != null ) {
			nullifiableEntityKeys.remove( key );
		}
		final BatchFetchQueue fetchQueue = this.batchFetchQueue;
		if ( fetchQueue != null ) {
			fetchQueue.removeBatchLoadableEntityKey( key );
			fetchQueue.removeSubselect( key );
		}
		return entity;
	}

	@Override
	public Object getEntity(EntityUniqueKey euk) {
		return entitiesByUniqueKey == null ? null : entitiesByUniqueKey.get( euk );
	}

	@Override
	public void addEntity(EntityUniqueKey euk, Object entity) {
		if ( entitiesByUniqueKey == null ) {
			entitiesByUniqueKey = new HashMap<>( INIT_COLL_SIZE );
		}
		entitiesByUniqueKey.put( euk, entity );
	}

	@Override
	public EntityEntry getEntry(Object entity) {
		return entityEntryContext.getEntityEntry( entity );
	}

	@Override
	public EntityEntry removeEntry(Object entity) {
		return entityEntryContext.removeEntityEntry( entity );
	}

	@Override
	public boolean isEntryFor(Object entity) {
		return entityEntryContext.hasEntityEntry( entity );
	}

	@Override
	public CollectionEntry getCollectionEntry(PersistentCollection coll) {
		return collectionEntries == null ? null : collectionEntries.get( coll );
	}

	@Override
	public EntityEntry addEntity(
			final Object entity,
			final Status status,
			final Object[] loadedState,
			final EntityKey entityKey,
			final Object version,
			final LockMode lockMode,
			final boolean existsInDatabase,
			final EntityPersister persister,
			final boolean disableVersionIncrement) {
		addEntity( entityKey, entity );
		return addEntry(
				entity,
				status,
				loadedState,
				null,
				entityKey.getIdentifier(),
				version,
				lockMode,
				existsInDatabase,
				persister,
				disableVersionIncrement
		);
	}

	@Override
	public EntityEntry addEntry(
			final Object entity,
			final Status status,
			final Object[] loadedState,
			final Object rowId,
			final Serializable id,
			final Object version,
			final LockMode lockMode,
			final boolean existsInDatabase,
			final EntityPersister persister,
			final boolean disableVersionIncrement) {
		assert lockMode != null;

		final EntityEntry e;

		/*
			IMPORTANT!!!

			The following instanceof checks and castings are intentional.

			DO NOT REFACTOR to make calls through the EntityEntryFactory interface, which would result
			in polymorphic call sites which will severely impact performance.

			When a virtual method is called via an interface the JVM needs to resolve which concrete
			implementation to call.  This takes CPU cycles and is a performance penalty.  It also prevents method
			inlining which further degrades performance.  Casting to an implementation and making a direct method call
			removes the virtual call, and allows the methods to be inlined.  In this critical code path, it has a very
			large impact on performance to make virtual method calls.
		*/
		if (persister.getEntityEntryFactory() instanceof MutableEntityEntryFactory) {
			//noinspection RedundantCast
			e = ( (MutableEntityEntryFactory) persister.getEntityEntryFactory() ).createEntityEntry(
					status,
					loadedState,
					rowId,
					id,
					version,
					lockMode,
					existsInDatabase,
					persister,
					disableVersionIncrement,
					this
			);
		}
		else {
			//noinspection RedundantCast
			e = ( (ImmutableEntityEntryFactory) persister.getEntityEntryFactory() ).createEntityEntry(
					status,
					loadedState,
					rowId,
					id,
					version,
					lockMode,
					existsInDatabase,
					persister,
					disableVersionIncrement,
					this
			);
		}

		entityEntryContext.addEntityEntry( entity, e );

		setHasNonReadOnlyEnties( status );
		return e;
	}

	public EntityEntry addReferenceEntry(
			final Object entity,
			final Status status) {

		((ManagedEntity)entity).$$_hibernate_getEntityEntry().setStatus( status );
		entityEntryContext.addEntityEntry( entity, ((ManagedEntity)entity).$$_hibernate_getEntityEntry() );

		setHasNonReadOnlyEnties( status );
		return ((ManagedEntity)entity).$$_hibernate_getEntityEntry();
	}

	@Override
	public boolean containsCollection(PersistentCollection collection) {
		return collectionEntries != null && collectionEntries.containsKey( collection );
	}

	@Override
	public boolean containsProxy(Object entity) {
		return proxiesByKey != null && proxiesByKey.containsValue( entity );
	}

	@Override
	public boolean reassociateIfUninitializedProxy(Object value) throws MappingException {
		if ( ! Hibernate.isInitialized( value ) ) {

			// could be a proxy....
			if ( value instanceof HibernateProxy ) {
				final HibernateProxy proxy = (HibernateProxy) value;
				final LazyInitializer li = proxy.getHibernateLazyInitializer();
				reassociateProxy( li, proxy );
				return true;
			}

			// or an uninitialized enhanced entity ("bytecode proxy")...
			if ( value instanceof PersistentAttributeInterceptable ) {
				final PersistentAttributeInterceptable bytecodeProxy = (PersistentAttributeInterceptable) value;
				final BytecodeLazyAttributeInterceptor interceptor = (BytecodeLazyAttributeInterceptor) bytecodeProxy.$$_hibernate_getInterceptor();
				if ( interceptor != null ) {
					interceptor.setSession( getSession() );
				}
				return true;
			}

		}

		return false;
	}

	@Override
	public void reassociateProxy(Object value, Serializable id) throws MappingException {
		if ( value instanceof HibernateProxy ) {
			LOG.debugf( "Setting proxy identifier: %s", id );
			final HibernateProxy proxy = (HibernateProxy) value;
			final LazyInitializer li = proxy.getHibernateLazyInitializer();
			li.setIdentifier( id );
			reassociateProxy( li, proxy );
		}
	}

	/**
	 * Associate a proxy that was instantiated by another session with this session
	 *
	 * @param li The proxy initializer.
	 * @param proxy The proxy to reassociate.
	 */
	private void reassociateProxy(LazyInitializer li, HibernateProxy proxy) {
		if ( li.getSession() != this.getSession() ) {
			final EntityPersister persister = session.getFactory().getMetamodel().entityPersister( li.getEntityName() );
			final EntityKey key = session.generateEntityKey( li.getInternalIdentifier(), persister );
		  	// any earlier proxy takes precedence
			getOrInitializeProxiesByKey().putIfAbsent( key, proxy );
			proxy.getHibernateLazyInitializer().setSession( session );
		}
	}

	@Override
	public Object unproxy(Object maybeProxy) throws HibernateException {
		if ( maybeProxy instanceof HibernateProxy ) {
			final HibernateProxy proxy = (HibernateProxy) maybeProxy;
			final LazyInitializer li = proxy.getHibernateLazyInitializer();
			if ( li.isUninitialized() ) {
				throw new PersistentObjectException(
						"object was an uninitialized proxy for " + li.getEntityName()
				);
			}
			//unwrap the object and return
			return li.getImplementation();
		}
		else {
			return maybeProxy;
		}
	}

	@Override
	public Object unproxyAndReassociate(Object maybeProxy) throws HibernateException {
		if ( maybeProxy instanceof HibernateProxy ) {
			final HibernateProxy proxy = (HibernateProxy) maybeProxy;
			final LazyInitializer li = proxy.getHibernateLazyInitializer();
			reassociateProxy( li, proxy );
			//initialize + unwrap the object and return it
			return li.getImplementation();
		}
		else if ( maybeProxy instanceof PersistentAttributeInterceptable ) {
			final PersistentAttributeInterceptable interceptable = (PersistentAttributeInterceptable) maybeProxy;
			final PersistentAttributeInterceptor interceptor = interceptable.$$_hibernate_getInterceptor();
			if ( interceptor instanceof EnhancementAsProxyLazinessInterceptor ) {
				( (EnhancementAsProxyLazinessInterceptor) interceptor ).forceInitialize( maybeProxy, null );
			}
			return maybeProxy;
		}
		else {
			return maybeProxy;
		}
	}

	@Override
	public void checkUniqueness(EntityKey key, Object object) throws HibernateException {
		final Object entity = getEntity( key );
		if ( entity == object ) {
			throw new AssertionFailure( "object already associated, but no entry was found" );
		}
		if ( entity != null ) {
			throw new NonUniqueObjectException( key.getIdentifier(), key.getEntityName() );
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object narrowProxy(Object proxy, EntityPersister persister, EntityKey key, Object object)
			throws HibernateException {

		final Class concreteProxyClass = persister.getConcreteProxyClass();
		final boolean alreadyNarrow = concreteProxyClass.isInstance( proxy );

		if ( !alreadyNarrow ) {
			LOG.narrowingProxy( concreteProxyClass );

			// If an impl is passed, there is really no point in creating a proxy.
			// It would just be extra processing.  Just return the impl
			if ( object != null ) {
				removeProxyByKey( key );
				return object;
			}

			// Similarly, if the original HibernateProxy is initialized, there
			// is again no point in creating a proxy.  Just return the impl
			final HibernateProxy originalHibernateProxy = (HibernateProxy) proxy;
			if ( !originalHibernateProxy.getHibernateLazyInitializer().isUninitialized() ) {
				final Object impl = originalHibernateProxy.getHibernateLazyInitializer().getImplementation();
				// can we return it?
				if ( concreteProxyClass.isInstance( impl ) ) {
					removeProxyByKey( key );
					return impl;
				}
			}


			// Otherwise, create the narrowed proxy
			final HibernateProxy narrowedProxy = (HibernateProxy) persister.createProxy( key.getIdentifier(), session );

			// set the read-only/modifiable mode in the new proxy to what it was in the original proxy
			final boolean readOnlyOrig = originalHibernateProxy.getHibernateLazyInitializer().isReadOnly();
			narrowedProxy.getHibernateLazyInitializer().setReadOnly( readOnlyOrig );

			return narrowedProxy;
		}
		else {

			if ( object != null ) {
				final LazyInitializer li = ( (HibernateProxy) proxy ).getHibernateLazyInitializer();
				li.setImplementation( object );
			}
			return proxy;
		}
	}

	private Object removeProxyByKey(final EntityKey key) {
		if ( proxiesByKey != null ) {
			return proxiesByKey.remove( key );
		}
		return null;
	}

	@Override
	public Object proxyFor(EntityPersister persister, EntityKey key, Object impl) throws HibernateException {
		if ( !persister.hasProxy() ) {
			return impl;
		}
		final Object proxy = getProxy( key );
		return ( proxy != null ) ? narrowProxy( proxy, persister, key, impl ) : impl;
	}

	@Override
	public Object proxyFor(Object impl) throws HibernateException {
		final EntityEntry e = getEntry( impl );
		if ( e == null ) {
			return impl;
		}
		return proxyFor( e.getPersister(), e.getEntityKey(), impl );
	}

	@Override
	public void addEnhancedProxy(EntityKey key, PersistentAttributeInterceptable entity) {
		if ( entitiesByKey == null ) {
			entitiesByKey = new HashMap<>( INIT_COLL_SIZE );
		}
		entitiesByKey.put( key, entity );
	}

	@Override
	public Object getCollectionOwner(Serializable key, CollectionPersister collectionPersister) throws MappingException {
		// todo : we really just need to add a split in the notions of:
		//		1) collection key
		//		2) collection owner key
		// these 2 are not always the same.  Same is true in the case of ToOne associations with property-ref...
		final EntityPersister ownerPersister = collectionPersister.getOwnerEntityPersister();
		if ( ownerPersister.getIdentifierType().getReturnedClass().isInstance( key ) ) {
			return getEntity( session.generateEntityKey( key, collectionPersister.getOwnerEntityPersister() ) );
		}

		// we have a property-ref type mapping for the collection key.  But that could show up a few ways here...
		//
		//		1) The incoming key could be the entity itself...
		if ( ownerPersister.isInstance( key ) ) {
			final Serializable owenerId = ownerPersister.getIdentifier( key, session );
			if ( owenerId == null ) {
				return null;
			}
			return getEntity( session.generateEntityKey( owenerId, ownerPersister ) );
		}

		final CollectionType collectionType = collectionPersister.getCollectionType();

		//		2) The incoming key is most likely the collection key which we need to resolve to the owner key
		//			find the corresponding owner instance
		//			a) try by EntityUniqueKey
		if ( collectionType.getLHSPropertyName() != null ) {
			final Object owner = getEntity(
					new EntityUniqueKey(
							ownerPersister.getEntityName(),
							collectionType.getLHSPropertyName(),
							key,
							collectionPersister.getKeyType(),
							ownerPersister.getEntityMode(),
							session.getFactory()
					)
			);
			if ( owner != null ) {
				return owner;
			}

			//		b) try by EntityKey, which means we need to resolve owner-key -> collection-key
			//			IMPL NOTE : yes if we get here this impl is very non-performant, but PersistenceContext
			//					was never designed to handle this case; adding that capability for real means splitting
			//					the notions of:
			//						1) collection key
			//						2) collection owner key
			// 					these 2 are not always the same (same is true in the case of ToOne associations with
			// 					property-ref).  That would require changes to (at least) CollectionEntry and quite
			//					probably changes to how the sql for collection initializers are generated
			//
			//			We could also possibly see if the referenced property is a natural id since we already have caching
			//			in place of natural id snapshots.  BUt really its better to just do it the right way ^^ if we start
			// 			going that route
			final Serializable ownerId = ownerPersister.getIdByUniqueKey( key, collectionType.getLHSPropertyName(), session );
			return getEntity( session.generateEntityKey( ownerId, ownerPersister ) );
		}

		// as a last resort this is what the old code did...
		return getEntity( session.generateEntityKey( key, collectionPersister.getOwnerEntityPersister() ) );
	}

	@Override
	public Object getLoadedCollectionOwnerOrNull(PersistentCollection collection) {
		final CollectionEntry ce = getCollectionEntry( collection );
		if ( ce == null || ce.getLoadedPersister() == null ) {
			return null;
		}

		Object loadedOwner = null;
		// TODO: an alternative is to check if the owner has changed; if it hasn't then
		// return collection.getOwner()
		final Serializable entityId = getLoadedCollectionOwnerIdOrNull( ce );
		if ( entityId != null ) {
			loadedOwner = getCollectionOwner( entityId, ce.getLoadedPersister() );
		}
		return loadedOwner;
	}

	@Override
	public Serializable getLoadedCollectionOwnerIdOrNull(PersistentCollection collection) {
		return getLoadedCollectionOwnerIdOrNull( getCollectionEntry( collection ) );
	}

	/**
	 * Get the ID for the entity that owned this persistent collection when it was loaded
	 *
	 * @param ce The collection entry
	 * @return the owner ID if available from the collection's loaded key; otherwise, returns null
	 */
	private Serializable getLoadedCollectionOwnerIdOrNull(CollectionEntry ce) {
		if ( ce == null || ce.getLoadedKey() == null || ce.getLoadedPersister() == null ) {
			return null;
		}
		// TODO: an alternative is to check if the owner has changed; if it hasn't then
		// get the ID from collection.getOwner()
		return ce.getLoadedPersister().getCollectionType().getIdOfOwnerOrNull( ce.getLoadedKey(), session );
	}

	@Override
	public void addUninitializedCollection(CollectionPersister persister, PersistentCollection collection, Serializable id) {
		final CollectionEntry ce = new CollectionEntry( collection, persister, id, flushing );
		addCollection( collection, ce, id );
		if ( persister.getBatchSize() > 1 ) {
			getBatchFetchQueue().addBatchLoadableCollection( collection, ce );
		}
	}

	@Override
	public void addUninitializedDetachedCollection(CollectionPersister persister, PersistentCollection collection) {
		final CollectionEntry ce = new CollectionEntry( persister, collection.getKey() );
		addCollection( collection, ce, collection.getKey() );
		if ( persister.getBatchSize() > 1 ) {
			getBatchFetchQueue().addBatchLoadableCollection( collection, ce );
		}
	}

	@Override
	public void addNewCollection(CollectionPersister persister, PersistentCollection collection)
			throws HibernateException {
		addCollection( collection, persister );
	}

	/**
	 * Add a collection to the cache, with a given collection entry.
	 *
	 * @param coll The collection for which we are adding an entry.
	 * @param entry The entry representing the collection.
	 * @param key The key of the collection's entry.
	 */
	private void addCollection(PersistentCollection coll, CollectionEntry entry, Serializable key) {
		getOrInitializeCollectionEntries().put( coll, entry );
		final CollectionKey collectionKey = new CollectionKey( entry.getLoadedPersister(), key );
		final PersistentCollection old = addCollectionByKey( collectionKey, coll );
		if ( old != null ) {
			if ( old == coll ) {
				throw new AssertionFailure( "bug adding collection twice" );
			}
			// or should it actually throw an exception?
			old.unsetSession( session );
			if ( collectionEntries != null ) {
				collectionEntries.remove( old );
			}
			// watch out for a case where old is still referenced
			// somewhere in the object graph! (which is a user error)
		}
	}

	private IdentityMap<PersistentCollection, CollectionEntry> getOrInitializeCollectionEntries() {
		if ( this.collectionEntries == null ) {
			this.collectionEntries = IdentityMap.instantiateSequenced( INIT_COLL_SIZE );
		}
		return this.collectionEntries;
	}

	/**
	 * Add a collection to the cache, creating a new collection entry for it
	 *
	 * @param collection The collection for which we are adding an entry.
	 * @param persister The collection persister
	 */
	private void addCollection(PersistentCollection collection, CollectionPersister persister) {
		final CollectionEntry ce = new CollectionEntry( persister, collection );
		getOrInitializeCollectionEntries().put( collection, ce );
	}

	@Override
	public void addInitializedDetachedCollection(CollectionPersister collectionPersister, PersistentCollection collection)
			throws HibernateException {
		if ( collection.isUnreferenced() ) {
			//treat it just like a new collection
			addCollection( collection, collectionPersister );
		}
		else {
			final CollectionEntry ce = new CollectionEntry( collection, session.getFactory() );
			addCollection( collection, ce, collection.getKey() );
		}
	}

	@Override
	public CollectionEntry addInitializedCollection(CollectionPersister persister, PersistentCollection collection, Serializable id)
			throws HibernateException {
		final CollectionEntry ce = new CollectionEntry( collection, persister, id, flushing );
		ce.postInitialize( collection );
		addCollection( collection, ce, id );
		return ce;
	}

	@Override
	public PersistentCollection getCollection(CollectionKey collectionKey) {
		return collectionsByKey == null ? null : collectionsByKey.get( collectionKey );
	}

	@Override
	public void addNonLazyCollection(PersistentCollection collection) {
		if ( nonlazyCollections == null ) {
			nonlazyCollections = new ArrayList<>( INIT_COLL_SIZE );
		}
		nonlazyCollections.add( collection );
	}

	@Override
	public void initializeNonLazyCollections() throws HibernateException {
		initializeNonLazyCollections( PersistentCollection::forceInitialization );
	}

	protected void initializeNonLazyCollections(Consumer<PersistentCollection> initializeAction ) {
		if ( loadCounter == 0 ) {
			LOG.trace( "Initializing non-lazy collections" );

			//do this work only at the very highest level of the load
			//don't let this method be called recursively
			loadCounter++;
			try {
				int size;
				while ( nonlazyCollections != null && ( size = nonlazyCollections.size() ) > 0 ) {
					//note that each iteration of the loop may add new elements
					initializeAction.accept( nonlazyCollections.remove( size - 1 ) );
				}
			}
			finally {
				loadCounter--;
				clearNullProperties();
			}
		}
	}

	@Override
	public PersistentCollection getCollectionHolder(Object array) {
		return arrayHolders == null ? null : arrayHolders.get( array );
	}

	@Override
	public void addCollectionHolder(PersistentCollection holder) {
		//TODO:refactor + make this method private
		if ( arrayHolders == null ) {
			arrayHolders = new IdentityHashMap<>( INIT_COLL_SIZE );
		}
		arrayHolders.put( holder.getValue(), holder );
	}

	@Override
	public PersistentCollection removeCollectionHolder(Object array) {
		return arrayHolders != null ? arrayHolders.remove( array ) : null;
	}

	@Override
	public Serializable getSnapshot(PersistentCollection coll) {
		return getCollectionEntry( coll ).getSnapshot();
	}

	@Override
	public CollectionEntry getCollectionEntryOrNull(Object collection) {
		PersistentCollection coll;
		if ( collection instanceof PersistentCollection ) {
			coll = (PersistentCollection) collection;
			//if (collection==null) throw new TransientObjectException("Collection was not yet persistent");
		}
		else {
			coll = getCollectionHolder( collection );
			if ( coll == null && collectionEntries != null ) {
				//it might be an unwrapped collection reference!
				//try to find a wrapper (slowish)
				final Iterator<PersistentCollection> wrappers = collectionEntries.keyIterator();
				while ( wrappers.hasNext() ) {
					final PersistentCollection pc = wrappers.next();
					if ( pc.isWrapper( collection ) ) {
						coll = pc;
						break;
					}
				}
			}
		}

		return (coll == null) ? null : getCollectionEntry( coll );
	}

	@Override
	public Object getProxy(EntityKey key) {
		return proxiesByKey == null ? null : proxiesByKey.get( key );
	}

	@Override
	public void addProxy(EntityKey key, Object proxy) {
		getOrInitializeProxiesByKey().put( key, proxy );
	}

	@Override
	public Object removeProxy(EntityKey key) {
		final BatchFetchQueue fetchQueue = this.batchFetchQueue;
		if ( fetchQueue != null ) {
			fetchQueue.removeBatchLoadableEntityKey( key );
			fetchQueue.removeSubselect( key );
		}
		return removeProxyByKey( key );
	}

	@Override
	public HashSet getNullifiableEntityKeys() {
		if ( nullifiableEntityKeys == null ) {
			nullifiableEntityKeys = new HashSet<>();
		}
		return nullifiableEntityKeys;
	}

	/**
	 * @deprecated this will be removed: it provides too wide access, making it hard to optimise the internals
	 * for specific access needs. Consider using #iterateEntities instead.
	 * @return
	 */
	@Deprecated
	@Override
	public Map getEntitiesByKey() {
		return entitiesByKey == null ? Collections.emptyMap() : entitiesByKey;
	}

	@Override
	public Iterator managedEntitiesIterator() {
		if ( entitiesByKey == null ) {
			return Collections.emptyIterator();
		}
		else {
			return entitiesByKey.values().iterator();
		}
	}

	@Override
	public int getNumberOfManagedEntities() {
		return entityEntryContext.getNumberOfManagedEntities();
	}

	@Override
	public Map getEntityEntries() {
		return null;
	}

	/**
	 * @deprecated We should not expose this directly: the other accessors that have been created as a replacement
	 * have better chances of skipping initializing this map, which is a good performance improvement.
	 * @return the map of managed collection entries.
	 */
	@Override
	@Deprecated
	public Map getCollectionEntries() {
		return getOrInitializeCollectionEntries();
	}

	@Override
	public void forEachCollectionEntry(BiConsumer<PersistentCollection, CollectionEntry> action, boolean concurrent) {
		if ( collectionEntries != null ) {
			if ( concurrent ) {
				for ( Map.Entry<PersistentCollection,CollectionEntry> entry : IdentityMap.concurrentEntries( collectionEntries ) ) {
					action.accept( entry.getKey(), entry.getValue() );
				}
			}
			else {
				collectionEntries.forEach( action );
			}
		}
	}

	@Override
	public Map getCollectionsByKey() {
		if ( collectionsByKey == null ) {
			return Collections.emptyMap();
		}
		else {
			return collectionsByKey;
		}
	}

	@Override
	public int getCascadeLevel() {
		return cascading;
	}

	@Override
	public int incrementCascadeLevel() {
		return ++cascading;
	}

	@Override
	public int decrementCascadeLevel() {
		return --cascading;
	}

	@Override
	public boolean isFlushing() {
		return flushing;
	}

	@Override
	public void setFlushing(boolean flushing) {
		final boolean afterFlush = this.flushing && ! flushing;
		this.flushing = flushing;
		if ( afterFlush ) {
			getNaturalIdHelper().cleanupFromSynchronizations();
		}
	}

	public boolean isRemovingOrphanBeforeUpates() {
		return removeOrphanBeforeUpdatesCounter > 0;
	}

	public void beginRemoveOrphanBeforeUpdates() {
		if ( getCascadeLevel() < 1 ) {
			throw new IllegalStateException( "Attempt to remove orphan when not cascading." );
		}
		if ( removeOrphanBeforeUpdatesCounter >= getCascadeLevel() ) {
			throw new IllegalStateException(
					String.format(
							"Cascade level [%d] is out of sync with removeOrphanBeforeUpdatesCounter [%d] before incrementing removeOrphanBeforeUpdatesCounter",
							getCascadeLevel(),
							removeOrphanBeforeUpdatesCounter
					)
			);
		}
		removeOrphanBeforeUpdatesCounter++;
	}

	public void endRemoveOrphanBeforeUpdates() {
		if ( getCascadeLevel() < 1 ) {
			throw new IllegalStateException( "Finished removing orphan when not cascading." );
		}
		if ( removeOrphanBeforeUpdatesCounter > getCascadeLevel() ) {
			throw new IllegalStateException(
					String.format(
							"Cascade level [%d] is out of sync with removeOrphanBeforeUpdatesCounter [%d] before decrementing removeOrphanBeforeUpdatesCounter",
							getCascadeLevel(),
							removeOrphanBeforeUpdatesCounter
					)
			);
		}
		removeOrphanBeforeUpdatesCounter--;
	}

	/**
	 * Call this before beginning a two-phase load
	 */
	@Override
	public void beforeLoad() {
		loadCounter++;
	}

	/**
	 * Call this after finishing a two-phase load
	 */
	@Override
	public void afterLoad() {
		loadCounter--;
	}

	@Override
	public boolean isLoadFinished() {
		return loadCounter == 0;
	}

	@Override
	public String toString() {
		final String entityKeySet = entitiesByKey == null ? "[]" :  entitiesByKey.keySet().toString();
		final String collectionsKeySet = collectionsByKey == null ? "[]" : collectionsByKey.keySet().toString();
		return "PersistenceContext[entityKeys=" + entityKeySet + ", collectionKeys=" + collectionsKeySet + "]";
	}

	@Override
	public Entry<Object,EntityEntry>[] reentrantSafeEntityEntries() {
		return entityEntryContext.reentrantSafeEntityEntries();
	}

	@Override
	public Serializable getOwnerId(String entityName, String propertyName, Object childEntity, Map mergeMap) {
		final String collectionRole = entityName + '.' + propertyName;
		final EntityPersister persister = session.getFactory().getMetamodel().entityPersister( entityName );
		final CollectionPersister collectionPersister = session.getFactory().getMetamodel().collectionPersister( collectionRole );

	    // try cache lookup first
		final Object parent = getParentsByChild( childEntity );
		if ( parent != null ) {
			final EntityEntry entityEntry = entityEntryContext.getEntityEntry( parent );
			//there maybe more than one parent, filter by type
			if ( persister.isSubclassEntityName( entityEntry.getEntityName() )
					&& isFoundInParent( propertyName, childEntity, persister, collectionPersister, parent ) ) {
				return getEntry( parent ).getId();
			}
			else {
				// remove wrong entry
				removeChildParent( childEntity );
			}
		}

		//not found in case, proceed
		// iterate all the entities currently associated with the persistence context.
		for ( Entry<Object,EntityEntry> me : reentrantSafeEntityEntries() ) {
			final EntityEntry entityEntry = me.getValue();
			// does this entity entry pertain to the entity persister in which we are interested (owner)?
			if ( persister.isSubclassEntityName( entityEntry.getEntityName() ) ) {
				final Object entityEntryInstance = me.getKey();

				//check if the managed object is the parent
				boolean found = isFoundInParent(
						propertyName,
						childEntity,
						persister,
						collectionPersister,
						entityEntryInstance
				);

				if ( !found && mergeMap != null ) {
					//check if the detached object being merged is the parent
					final Object unmergedInstance = mergeMap.get( entityEntryInstance );
					final Object unmergedChild = mergeMap.get( childEntity );
					if ( unmergedInstance != null && unmergedChild != null ) {
						found = isFoundInParent(
								propertyName,
								unmergedChild,
								persister,
								collectionPersister,
								unmergedInstance
						);
						LOG.debugf(
								"Detached object being merged (corresponding with a managed entity) has a collection that [%s] the detached child.",
								( found ? "contains" : "does not contain" )
						);
					}
				}

				if ( found ) {
					return entityEntry.getId();
				}

			}
		}

		// if we get here, it is possible that we have a proxy 'in the way' of the merge map resolution...
		// 		NOTE: decided to put this here rather than in the above loop as I was nervous about the performance
		//		of the loop-in-loop especially considering this is far more likely the 'edge case'
		if ( mergeMap != null ) {
			for ( Object o : mergeMap.entrySet() ) {
				final Entry mergeMapEntry = (Entry) o;
				if ( mergeMapEntry.getKey() instanceof HibernateProxy ) {
					final HibernateProxy proxy = (HibernateProxy) mergeMapEntry.getKey();
					if ( persister.isSubclassEntityName( proxy.getHibernateLazyInitializer().getEntityName() ) ) {
						boolean found = isFoundInParent(
								propertyName,
								childEntity,
								persister,
								collectionPersister,
								mergeMap.get( proxy )
						);
						LOG.debugf(
								"Detached proxy being merged has a collection that [%s] the managed child.",
								(found ? "contains" : "does not contain")
						);
						if ( !found ) {
							found = isFoundInParent(
									propertyName,
									mergeMap.get( childEntity ),
									persister,
									collectionPersister,
									mergeMap.get( proxy )
							);
							LOG.debugf(
									"Detached proxy being merged has a collection that [%s] the detached child being merged..",
									(found ? "contains" : "does not contain")
							);
						}
						if ( found ) {
							return proxy.getHibernateLazyInitializer().getInternalIdentifier();
						}
					}
				}
			}
		}

		return null;
	}

	private Object getParentsByChild(Object childEntity) {
		if ( parentsByChild != null ) {
			return parentsByChild.get( childEntity );
		}
		return null;
	}

	private boolean isFoundInParent(
			String property,
			Object childEntity,
			EntityPersister persister,
			CollectionPersister collectionPersister,
			Object potentialParent) {
		final Object collection = persister.getPropertyValue( potentialParent, property );
		return collection != null
				&& Hibernate.isInitialized( collection )
				&& collectionPersister.getCollectionType().contains( collection, childEntity, session );
	}

	@Override
	public Object getIndexInOwner(String entity, String property, Object childEntity, Map mergeMap) {
		final MetamodelImplementor metamodel = session.getFactory().getMetamodel();
		final EntityPersister persister = metamodel.entityPersister( entity );
		final CollectionPersister cp = metamodel.collectionPersister( entity + '.' + property );

	    // try cache lookup first
		final Object parent = getParentsByChild( childEntity );
		if ( parent != null ) {
			final EntityEntry entityEntry = entityEntryContext.getEntityEntry( parent );
			//there maybe more than one parent, filter by type
			if ( persister.isSubclassEntityName( entityEntry.getEntityName() ) ) {
				Object index = getIndexInParent( property, childEntity, persister, cp, parent );

				if ( index == null && mergeMap != null ) {
					final Object unMergedInstance = mergeMap.get( parent );
					final Object unMergedChild = mergeMap.get( childEntity );
					if ( unMergedInstance != null && unMergedChild != null ) {
						index = getIndexInParent( property, unMergedChild, persister, cp, unMergedInstance );
						LOG.debugf(
								"A detached object being merged (corresponding to a parent in parentsByChild) has an indexed collection that [%s] the detached child being merged. ",
								( index != null ? "contains" : "does not contain" )
						);
					}
				}
				if ( index != null ) {
					return index;
				}
			}
			else {
				// remove wrong entry
				removeChildParent( childEntity );
			}
		}

		//Not found in cache, proceed
		for ( Entry<Object, EntityEntry> me : reentrantSafeEntityEntries() ) {
			final EntityEntry ee = me.getValue();
			if ( persister.isSubclassEntityName( ee.getEntityName() ) ) {
				final Object instance = me.getKey();

				Object index = getIndexInParent( property, childEntity, persister, cp, instance );
				if ( index==null && mergeMap!=null ) {
					final Object unMergedInstance = mergeMap.get( instance );
					final Object unMergedChild = mergeMap.get( childEntity );
					if ( unMergedInstance != null && unMergedChild!=null ) {
						index = getIndexInParent( property, unMergedChild, persister, cp, unMergedInstance );
						LOG.debugf(
								"A detached object being merged (corresponding to a managed entity) has an indexed collection that [%s] the detached child being merged. ",
								(index != null ? "contains" : "does not contain" )
						);
					}
				}

				if ( index != null ) {
					return index;
				}
			}
		}
		return null;
	}

	private Object getIndexInParent(
			String property,
			Object childEntity,
			EntityPersister persister,
			CollectionPersister collectionPersister,
			Object potentialParent){
		final Object collection = persister.getPropertyValue( potentialParent, property );
		if ( collection != null && Hibernate.isInitialized( collection ) ) {
			return collectionPersister.getCollectionType().indexOf( collection, childEntity );
		}
		else {
			return null;
		}
	}

	@Override
	public void addNullProperty(EntityKey ownerKey, String propertyName) {
		if ( nullAssociations == null ) {
			nullAssociations = new HashSet<>( INIT_COLL_SIZE );
		}
		nullAssociations.add( new AssociationKey( ownerKey, propertyName ) );
	}

	@Override
	public boolean isPropertyNull(EntityKey ownerKey, String propertyName) {
		return nullAssociations != null && nullAssociations.contains( new AssociationKey( ownerKey, propertyName ) );
	}

	private void clearNullProperties() {
		nullAssociations = null;
	}

	@Override
	public boolean isReadOnly(Object entityOrProxy) {
		if ( entityOrProxy == null ) {
			throw new AssertionFailure( "object must be non-null." );
		}
		boolean isReadOnly;
		if ( entityOrProxy instanceof HibernateProxy ) {
			isReadOnly = ( (HibernateProxy) entityOrProxy ).getHibernateLazyInitializer().isReadOnly();
		}
		else {
			final EntityEntry ee =  getEntry( entityOrProxy );
			if ( ee == null ) {
				throw new TransientObjectException( "Instance was not associated with this persistence context" );
			}
			isReadOnly = ee.isReadOnly();
		}
		return isReadOnly;
	}

	@Override
	public void setReadOnly(Object object, boolean readOnly) {
		if ( object == null ) {
			throw new AssertionFailure( "object must be non-null." );
		}
		if ( isReadOnly( object ) == readOnly ) {
			return;
		}
		if ( object instanceof HibernateProxy ) {
			final HibernateProxy proxy = (HibernateProxy) object;
			setProxyReadOnly( proxy, readOnly );
			if ( Hibernate.isInitialized( proxy ) ) {
				setEntityReadOnly(
						proxy.getHibernateLazyInitializer().getImplementation(),
						readOnly
				);
			}
		}
		else {
			setEntityReadOnly( object, readOnly );
			// PersistenceContext.proxyFor( entity ) returns entity if there is no proxy for that entity
			// so need to check the return value to be sure it is really a proxy
			final Object maybeProxy = getSession().getPersistenceContextInternal().proxyFor( object );
			if ( maybeProxy instanceof HibernateProxy ) {
				setProxyReadOnly( (HibernateProxy) maybeProxy, readOnly );
			}
		}
	}

	private void setProxyReadOnly(HibernateProxy proxy, boolean readOnly) {
		final LazyInitializer hibernateLazyInitializer = proxy.getHibernateLazyInitializer();
		if ( hibernateLazyInitializer.getSession() != getSession() ) {
			throw new AssertionFailure(
					"Attempt to set a proxy to read-only that is associated with a different session" );
		}
		hibernateLazyInitializer.setReadOnly( readOnly );
	}

	private void setEntityReadOnly(Object entity, boolean readOnly) {
		final EntityEntry entry = getEntry( entity );
		if ( entry == null ) {
			throw new TransientObjectException( "Instance was not associated with this persistence context" );
		}
		entry.setReadOnly( readOnly, entity );
		hasNonReadOnlyEntities = hasNonReadOnlyEntities || ! readOnly;
	}

	@Override
	public void replaceDelayedEntityIdentityInsertKeys(EntityKey oldKey, Serializable generatedId) {
		final Object entity = entitiesByKey == null ? null : entitiesByKey.remove( oldKey );
		final EntityEntry oldEntry = entityEntryContext.removeEntityEntry( entity );
		this.parentsByChild = null;

		final EntityKey newKey = session.generateEntityKey( generatedId, oldEntry.getPersister() );
		addEntity( newKey, entity );
		addEntry(
				entity,
				oldEntry.getStatus(),
				oldEntry.getLoadedState(),
				oldEntry.getRowId(),
				generatedId,
				oldEntry.getVersion(),
				oldEntry.getLockMode(),
				oldEntry.isExistsInDatabase(),
				oldEntry.getPersister(),
				oldEntry.isBeingReplicated()
		);
	}

	/**
	 * Used by the owning session to explicitly control serialization of the
	 * persistence context.
	 *
	 * @param oos The stream to which the persistence context should get written
	 * @throws IOException serialization errors.
	 */
	public void serialize(ObjectOutputStream oos) throws IOException {
		LOG.trace( "Serializing persistence-context" );

		oos.writeBoolean( defaultReadOnly );
		oos.writeBoolean( hasNonReadOnlyEntities );

		final Serializer<Map.Entry<EntityKey, Object>> entityKeySerializer = (entry, stream) -> {
			entry.getKey().serialize( stream );
			stream.writeObject( entry.getValue() );
		};

		writeMapToStream( entitiesByKey, oos, "entitiesByKey", entityKeySerializer );
		writeMapToStream(
				entitiesByUniqueKey,
				oos, "entitiesByUniqueKey", (entry, stream) -> {
					entry.getKey().serialize( stream );
					stream.writeObject( entry.getValue() );
				}
		);
		writeMapToStream( proxiesByKey, oos, "proxiesByKey", entityKeySerializer );
		writeMapToStream( entitySnapshotsByKey, oos, "entitySnapshotsByKey", entityKeySerializer );

		entityEntryContext.serialize( oos );
		writeMapToStream(
				collectionsByKey,
				oos,
				"collectionsByKey",
				(entry, stream) -> {
					entry.getKey().serialize( stream );
					stream.writeObject( entry.getValue() );
				}
		);
		writeMapToStream(
				collectionEntries,
				oos,
				"collectionEntries",
				(entry, stream) -> {
					stream.writeObject( entry.getKey() );
					entry.getValue().serialize( stream );
				}
		);
		writeMapToStream(
				arrayHolders,
				oos,
				"arrayHolders",
				(entry, stream) -> {
					stream.writeObject( entry.getKey() );
					stream.writeObject( entry.getValue() );
				}
		);
		writeCollectionToStream( nullifiableEntityKeys, oos, "nullifiableEntityKey", EntityKey::serialize );
	}

	private interface Serializer<E> {

		void serialize(E element, ObjectOutputStream oos) throws IOException;
	}

	private <K, V> void writeMapToStream(
			Map<K, V> map,
			ObjectOutputStream oos,
			String keysName,
			Serializer<Entry<K, V>> serializer) throws IOException {
		if ( map == null ) {
			oos.writeInt( 0 );
		}
		else {
			writeCollectionToStream( map.entrySet(), oos, keysName, serializer );
		}
	}

	private <E> void writeCollectionToStream(
			Collection<E> collection,
			ObjectOutputStream oos,
			String keysName,
			Serializer<E> serializer) throws IOException {
		if ( collection == null ) {
			oos.writeInt( 0 );
		}
		else {
			oos.writeInt( collection.size() );
			if ( LOG.isTraceEnabled() ) {
				LOG.trace( "Starting serialization of [" + collection.size() + "] " + keysName + " entries" );
			}
			for ( E entry : collection ) {
				serializer.serialize( entry, oos );
			}
		}
	}

	/**
	 * Used by the owning session to explicitly control deserialization of the persistence context.
	 *
	 * @param ois The stream from which the persistence context should be read
	 * @param session The owning session
	 *
	 * @return The deserialized StatefulPersistenceContext
	 *
	 * @throws IOException deserialization errors.
	 * @throws ClassNotFoundException deserialization errors.
	 */
	public static StatefulPersistenceContext deserialize(
			ObjectInputStream ois,
			SessionImplementor session) throws IOException, ClassNotFoundException {
		LOG.trace( "Deserializing persistence-context" );
		final StatefulPersistenceContext rtn = new StatefulPersistenceContext( session );
		SessionFactoryImplementor sfi = session.getFactory();

		// during deserialization, we need to reconnect all proxies and
		// collections to this session, as well as the EntityEntry and
		// CollectionEntry instances; these associations are transient
		// because serialization is used for different things.

		try {
			rtn.defaultReadOnly = ois.readBoolean();
			// todo : we can actually just determine this from the incoming EntityEntry-s
			rtn.hasNonReadOnlyEntities = ois.readBoolean();

			int count = ois.readInt();
			if ( LOG.isTraceEnabled() ) {
				LOG.trace( "Starting deserialization of [" + count + "] entitiesByKey entries" );
			}
			rtn.entitiesByKey = new HashMap<>( count < INIT_COLL_SIZE ? INIT_COLL_SIZE : count );
			for ( int i = 0; i < count; i++ ) {
				rtn.entitiesByKey.put( EntityKey.deserialize( ois, sfi ), ois.readObject() );
			}

			count = ois.readInt();
			if ( LOG.isTraceEnabled() ) {
				LOG.trace( "Starting deserialization of [" + count + "] entitiesByUniqueKey entries" );
			}
			if ( count != 0 ) {
				rtn.entitiesByUniqueKey = new HashMap<>( count < INIT_COLL_SIZE ? INIT_COLL_SIZE : count );
				for ( int i = 0; i < count; i++ ) {
					rtn.entitiesByUniqueKey.put( EntityUniqueKey.deserialize( ois, session ), ois.readObject() );
				}
			}

			count = ois.readInt();
			if ( LOG.isTraceEnabled() ) {
				LOG.trace( "Starting deserialization of [" + count + "] proxiesByKey entries" );
			}
			for ( int i = 0; i < count; i++ ) {
				final EntityKey ek = EntityKey.deserialize( ois, sfi );
				final Object proxy = ois.readObject();
				if ( proxy instanceof HibernateProxy ) {
					( (HibernateProxy) proxy ).getHibernateLazyInitializer().setSession( session );
					rtn.getOrInitializeProxiesByKey().put( ek, proxy );
				}
				else {
					// otherwise, the proxy was pruned during the serialization process
					if ( LOG.isTraceEnabled() ) {
						LOG.trace( "Encountered pruned proxy" );
					}
				}
			}

			count = ois.readInt();
			if ( LOG.isTraceEnabled() ) {
				LOG.trace( "Starting deserialization of [" + count + "] entitySnapshotsByKey entries" );
			}
			rtn.entitySnapshotsByKey = new HashMap<>( count < INIT_COLL_SIZE ? INIT_COLL_SIZE : count );
			for ( int i = 0; i < count; i++ ) {
				rtn.entitySnapshotsByKey.put( EntityKey.deserialize( ois, sfi ), ois.readObject() );
			}

			rtn.entityEntryContext = EntityEntryContext.deserialize( ois, rtn );

			count = ois.readInt();
			if ( LOG.isTraceEnabled() ) {
				LOG.trace( "Starting deserialization of [" + count + "] collectionsByKey entries" );
			}
			rtn.collectionsByKey = new HashMap<>( count < INIT_COLL_SIZE ? INIT_COLL_SIZE : count );
			for ( int i = 0; i < count; i++ ) {
				rtn.collectionsByKey.put( CollectionKey.deserialize( ois, session ), (PersistentCollection) ois.readObject() );
			}

			count = ois.readInt();
			if ( LOG.isTraceEnabled() ) {
				LOG.trace( "Starting deserialization of [" + count + "] collectionEntries entries" );
			}
			for ( int i = 0; i < count; i++ ) {
				final PersistentCollection pc = (PersistentCollection) ois.readObject();
				final CollectionEntry ce = CollectionEntry.deserialize( ois, session );
				pc.setCurrentSession( session );
				rtn.getOrInitializeCollectionEntries().put( pc, ce );
			}

			count = ois.readInt();
			if ( LOG.isTraceEnabled() ) {
				LOG.trace( "Starting deserialization of [" + count + "] arrayHolders entries" );
			}
			if ( count != 0 ) {
				rtn.arrayHolders = new IdentityHashMap<>( count < INIT_COLL_SIZE ? INIT_COLL_SIZE : count );
				for ( int i = 0; i < count; i++ ) {
					rtn.arrayHolders.put( ois.readObject(), (PersistentCollection) ois.readObject() );
				}
			}

			count = ois.readInt();
			if ( LOG.isTraceEnabled() ) {
				LOG.trace( "Starting deserialization of [" + count + "] nullifiableEntityKey entries" );
			}
			rtn.nullifiableEntityKeys = new HashSet<>();
			for ( int i = 0; i < count; i++ ) {
				rtn.nullifiableEntityKeys.add( EntityKey.deserialize( ois, sfi ) );
			}

		}
		catch ( HibernateException he ) {
			throw new InvalidObjectException( he.getMessage() );
		}

		return rtn;
	}

	@Override
	public void addChildParent(Object child, Object parent) {
		if ( parentsByChild == null ) {
			parentsByChild = new IdentityHashMap<>( INIT_COLL_SIZE );
		}
		parentsByChild.put( child, parent );
	}

	@Override
	public void removeChildParent(Object child) {
		if ( parentsByChild != null ) {
			parentsByChild.remove( child );
		}
	}

	// INSERTED KEYS HANDLING ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private HashMap<String, HashSet<Serializable>> insertedKeysMap;

	@Override
	public void registerInsertedKey(EntityPersister persister, Serializable id) {
		// we only are worried about registering these if the persister defines caching
		if ( persister.canWriteToCache() ) {
			if ( insertedKeysMap == null ) {
				insertedKeysMap = new HashMap<>();
			}
			final String rootEntityName = persister.getRootEntityName();
			HashSet<Serializable> insertedEntityIds = insertedKeysMap.computeIfAbsent(
					rootEntityName,
					k -> new HashSet<>()
			);
			insertedEntityIds.add( id );
		}
	}

	@Override
	public boolean wasInsertedDuringTransaction(EntityPersister persister, Serializable id) {
		// again, we only really care if the entity is cached
		if ( persister.canWriteToCache() ) {
			if ( insertedKeysMap != null ) {
				final HashSet<Serializable> insertedEntityIds = insertedKeysMap.get( persister.getRootEntityName() );
				if ( insertedEntityIds != null ) {
					return insertedEntityIds.contains( id );
				}
			}
		}
		return false;
	}

	@Override
	public boolean containsNullifiableEntityKey(Supplier<EntityKey> sek) {
		if ( nullifiableEntityKeys == null || nullifiableEntityKeys.size() == 0 ) {
			return false;
		}
		else {
			final EntityKey entityKey = sek.get();
			return nullifiableEntityKeys.contains( entityKey );
		}
	}

	@Override
	public void registerNullifiableEntityKey(EntityKey key) {
		if ( nullifiableEntityKeys == null ) {
			nullifiableEntityKeys = new HashSet<>();
		}
		this.nullifiableEntityKeys.add( key );
	}

	@Override
	public boolean isNullifiableEntityKeysEmpty() {
		return ( nullifiableEntityKeys == null || nullifiableEntityKeys.size() == 0 );
	}

	@Override
	public int getCollectionEntriesSize() {
		return collectionEntries == null ? 0 : collectionEntries.size();
	}

	@Override
	public CollectionEntry removeCollectionEntry(PersistentCollection collection) {
		if ( collectionEntries == null ) {
			return null;
		}
		else {
			return collectionEntries.remove( collection );
		}
	}

	@Override
	public void clearCollectionsByKey() {
		if ( collectionsByKey != null ) {
			//A valid alternative would be to set this to null, like we do on close.
			//The difference being that in this case we expect the collection will be used again, so we bet that clear()
			//might allow us to skip having to re-allocate the collection.
			collectionsByKey.clear();
		}
	}

	@Override
	public PersistentCollection addCollectionByKey(CollectionKey collectionKey, PersistentCollection persistentCollection) {
		if ( collectionsByKey == null ) {
			collectionsByKey = new HashMap<>( INIT_COLL_SIZE );
		}
		final PersistentCollection old = collectionsByKey.put( collectionKey, persistentCollection );
		return old;
	}

	@Override
	public void removeCollectionByKey(CollectionKey collectionKey) {
		if ( collectionsByKey != null ) {
			collectionsByKey.remove( collectionKey );
		}
	}

	private void cleanUpInsertedKeysAfterTransaction() {
		if ( insertedKeysMap != null ) {
			insertedKeysMap.clear();
		}
	}

	// NATURAL ID RESOLUTION HANDLING ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private NaturalIdXrefDelegate naturalIdXrefDelegate;

	private NaturalIdXrefDelegate getNaturalIdXrefDelegate() {
		if ( naturalIdXrefDelegate == null ) {
			this.naturalIdXrefDelegate = new NaturalIdXrefDelegate( this );
		}
		return naturalIdXrefDelegate;
	}

	private final NaturalIdHelper naturalIdHelper = new NaturalIdHelper() {
		@Override
		public void cacheNaturalIdCrossReferenceFromLoad(
				EntityPersister persister,
				Serializable id,
				Object[] naturalIdValues) {
			if ( !persister.hasNaturalIdentifier() ) {
				// nothing to do
				return;
			}

			persister = locateProperPersister( persister );

			// 'justAddedLocally' is meant to handle the case where we would get double stats journaling
			//	from a single load event.  The first put journal would come from the natural id resolution;
			// the second comes from the entity loading.  In this condition, we want to avoid the multiple
			// 'put' stats incrementing.
			final boolean justAddedLocally = getNaturalIdXrefDelegate().cacheNaturalIdCrossReference( persister, id, naturalIdValues );

			if ( justAddedLocally && persister.hasNaturalIdCache() ) {
				managedSharedCacheEntries( persister, id, naturalIdValues, null, CachedNaturalIdValueSource.LOAD );
			}
		}

		@Override
		public void manageLocalNaturalIdCrossReference(
				EntityPersister persister,
				Serializable id,
				Object[] state,
				Object[] previousState,
				CachedNaturalIdValueSource source) {
			if ( !persister.hasNaturalIdentifier() ) {
				// nothing to do
				return;
			}

			persister = locateProperPersister( persister );
			final Object[] naturalIdValues = extractNaturalIdValues( state, persister );

			// cache
			getNaturalIdXrefDelegate().cacheNaturalIdCrossReference( persister, id, naturalIdValues );
		}

		@Override
		public void manageSharedNaturalIdCrossReference(
				EntityPersister persister,
				final Serializable id,
				Object[] state,
				Object[] previousState,
				CachedNaturalIdValueSource source) {
			if ( !persister.hasNaturalIdentifier() ) {
				// nothing to do
				return;
			}

			if ( !persister.hasNaturalIdCache() ) {
				// nothing to do
				return;
			}

			persister = locateProperPersister( persister );
			final Object[] naturalIdValues = extractNaturalIdValues( state, persister );
			final Object[] previousNaturalIdValues = previousState == null ? null : extractNaturalIdValues( previousState, persister );

			managedSharedCacheEntries( persister, id, naturalIdValues, previousNaturalIdValues, source );
		}

		private void managedSharedCacheEntries(
				EntityPersister persister,
				final Serializable id,
				Object[] naturalIdValues,
				Object[] previousNaturalIdValues,
				CachedNaturalIdValueSource source) {
			final NaturalIdDataAccess naturalIdCacheAccessStrategy = persister.getNaturalIdCacheAccessStrategy();
			final Object naturalIdCacheKey = naturalIdCacheAccessStrategy.generateCacheKey( naturalIdValues, persister, session );

			final SessionFactoryImplementor factory = session.getFactory();
			final StatisticsImplementor statistics = factory.getStatistics();

			switch ( source ) {
				case LOAD: {
					if ( CacheHelper.fromSharedCache( session, naturalIdCacheKey, naturalIdCacheAccessStrategy ) != null ) {
						// prevent identical re-cachings
						return;
					}
					final boolean put = naturalIdCacheAccessStrategy.putFromLoad(
							session,
							naturalIdCacheKey,
							id,
							null
					);

					if ( put && statistics.isStatisticsEnabled() ) {
						statistics.naturalIdCachePut(
								StatsHelper.INSTANCE.getRootEntityRole( persister ),
								naturalIdCacheAccessStrategy.getRegion().getName()
						);
					}

					break;
				}
				case INSERT: {
					final boolean put = naturalIdCacheAccessStrategy.insert( session, naturalIdCacheKey, id );
					if ( put && statistics.isStatisticsEnabled() ) {
						statistics.naturalIdCachePut(
								StatsHelper.INSTANCE.getRootEntityRole( persister ),
								naturalIdCacheAccessStrategy.getRegion().getName()
						);
					}

					( (EventSource) session ).getActionQueue().registerProcess(
							new AfterTransactionCompletionProcess() {
								@Override
								public void doAfterTransactionCompletion(boolean success, SharedSessionContractImplementor session) {
									if ( success ) {
										final boolean put = naturalIdCacheAccessStrategy.afterInsert( session, naturalIdCacheKey, id );
										if ( put && statistics.isStatisticsEnabled() ) {
											statistics.naturalIdCachePut(
													StatsHelper.INSTANCE.getRootEntityRole( persister ),
													naturalIdCacheAccessStrategy.getRegion().getName()
											);
										}
									}
									else {
										naturalIdCacheAccessStrategy.evict( naturalIdCacheKey );
									}
								}
							}
					);

					break;
				}
				case UPDATE: {
					final Object previousCacheKey = naturalIdCacheAccessStrategy.generateCacheKey( previousNaturalIdValues, persister, session );
					if ( naturalIdCacheKey.equals( previousCacheKey ) ) {
						// prevent identical re-caching, solves HHH-7309
						return;
					}
					final SoftLock removalLock = naturalIdCacheAccessStrategy.lockItem( session, previousCacheKey, null );
					naturalIdCacheAccessStrategy.remove( session, previousCacheKey);

					final SoftLock lock = naturalIdCacheAccessStrategy.lockItem( session, naturalIdCacheKey, null );
					final boolean put = naturalIdCacheAccessStrategy.update( session, naturalIdCacheKey, id );
					if ( put && statistics.isStatisticsEnabled() ) {
						statistics.naturalIdCachePut(
								StatsHelper.INSTANCE.getRootEntityRole( persister ),
								naturalIdCacheAccessStrategy.getRegion().getName()
						);
					}

					( (EventSource) session ).getActionQueue().registerProcess(
							new AfterTransactionCompletionProcess() {
								@Override
								public void doAfterTransactionCompletion(boolean success, SharedSessionContractImplementor session) {
									naturalIdCacheAccessStrategy.unlockItem( session, previousCacheKey, removalLock );
									if (success) {
										final boolean put = naturalIdCacheAccessStrategy.afterUpdate(
												session,
												naturalIdCacheKey,
												id,
												lock
										);

										if ( put && statistics.isStatisticsEnabled() ) {
											statistics.naturalIdCachePut(
													StatsHelper.INSTANCE.getRootEntityRole( persister ),
													naturalIdCacheAccessStrategy.getRegion().getName()
											);
										}
									}
									else {
										naturalIdCacheAccessStrategy.unlockItem( session, naturalIdCacheKey, lock );
									}
								}
							}
					);

					break;
				}
				default: {
					if ( LOG.isDebugEnabled() ) {
						LOG.debug( "Unexpected CachedNaturalIdValueSource [" + source + "]" );
					}
				}
			}
		}

		@Override
		public Object[] removeLocalNaturalIdCrossReference(EntityPersister persister, Serializable id, Object[] state) {
			if ( !persister.hasNaturalIdentifier() ) {
				// nothing to do
				return null;
			}

			persister = locateProperPersister( persister );
			final Object[] naturalIdValues = getNaturalIdValues( state, persister );

			final Object[] localNaturalIdValues = getNaturalIdXrefDelegate().removeNaturalIdCrossReference(
					persister,
					id,
					naturalIdValues
			);

			return localNaturalIdValues != null ? localNaturalIdValues : naturalIdValues;
		}

		@Override
		public void removeSharedNaturalIdCrossReference(EntityPersister persister, Serializable id, Object[] naturalIdValues) {
			if ( !persister.hasNaturalIdentifier() ) {
				// nothing to do
				return;
			}

			if ( ! persister.hasNaturalIdCache() ) {
				// nothing to do
				return;
			}

			// todo : couple of things wrong here:
			//		1) should be using access strategy, not plain evict..
			//		2) should prefer session-cached values if any (requires interaction from removeLocalNaturalIdCrossReference)

			persister = locateProperPersister( persister );
			final NaturalIdDataAccess naturalIdCacheAccessStrategy = persister.getNaturalIdCacheAccessStrategy();
			final Object naturalIdCacheKey = naturalIdCacheAccessStrategy.generateCacheKey( naturalIdValues, persister, session );
			naturalIdCacheAccessStrategy.evict( naturalIdCacheKey );

//			if ( sessionCachedNaturalIdValues != null
//					&& !Arrays.equals( sessionCachedNaturalIdValues, deletedNaturalIdValues ) ) {
//				final NaturalIdCacheKey sessionNaturalIdCacheKey = new NaturalIdCacheKey( sessionCachedNaturalIdValues, persister, session );
//				naturalIdCacheAccessStrategy.evict( sessionNaturalIdCacheKey );
//			}
		}

		@Override
		public Object[] findCachedNaturalId(EntityPersister persister, Serializable pk) {
			return getNaturalIdXrefDelegate().findCachedNaturalId( locateProperPersister( persister ), pk );
		}

		@Override
		public Serializable findCachedNaturalIdResolution(EntityPersister persister, Object[] naturalIdValues) {
			return getNaturalIdXrefDelegate().findCachedNaturalIdResolution( locateProperPersister( persister ), naturalIdValues );
		}

		@Override
		public Object[] extractNaturalIdValues(Object[] state, EntityPersister persister) {
			final int[] naturalIdPropertyIndexes = persister.getNaturalIdentifierProperties();
			if ( state.length == naturalIdPropertyIndexes.length ) {
				return state;
			}

			final Object[] naturalIdValues = new Object[naturalIdPropertyIndexes.length];
			for ( int i = 0; i < naturalIdPropertyIndexes.length; i++ ) {
				naturalIdValues[i] = state[naturalIdPropertyIndexes[i]];
			}
			return naturalIdValues;
		}

		@Override
		public Object[] extractNaturalIdValues(Object entity, EntityPersister persister) {
			if ( entity == null ) {
				throw new AssertionFailure( "Entity from which to extract natural id value(s) cannot be null" );
			}
			if ( persister == null ) {
				throw new AssertionFailure( "Persister to use in extracting natural id value(s) cannot be null" );
			}

			final int[] naturalIdentifierProperties = persister.getNaturalIdentifierProperties();
			final Object[] naturalIdValues = new Object[naturalIdentifierProperties.length];

			for ( int i = 0; i < naturalIdentifierProperties.length; i++ ) {
				naturalIdValues[i] = persister.getPropertyValue( entity, naturalIdentifierProperties[i] );
			}

			return naturalIdValues;
		}

		@Override
		public Collection<Serializable> getCachedPkResolutions(EntityPersister entityPersister) {
			return getNaturalIdXrefDelegate().getCachedPkResolutions( entityPersister );
		}

		@Override
		public void handleSynchronization(EntityPersister persister, Serializable pk, Object entity) {
			if ( !persister.hasNaturalIdentifier() ) {
				// nothing to do
				return;
			}

			persister = locateProperPersister( persister );

			final Object[] naturalIdValuesFromCurrentObjectState = extractNaturalIdValues( entity, persister );
			final NaturalIdXrefDelegate naturalIdXrefDelegate = getNaturalIdXrefDelegate();
			final boolean changed = ! naturalIdXrefDelegate.sameAsCached(
					persister,
					pk,
					naturalIdValuesFromCurrentObjectState
			);

			if ( changed ) {
				final Object[] cachedNaturalIdValues = naturalIdXrefDelegate.findCachedNaturalId( persister, pk );
				naturalIdXrefDelegate.cacheNaturalIdCrossReference( persister, pk, naturalIdValuesFromCurrentObjectState );
				naturalIdXrefDelegate.stashInvalidNaturalIdReference( persister, cachedNaturalIdValues );

				removeSharedNaturalIdCrossReference(
						persister,
						pk,
						cachedNaturalIdValues
				);
			}
		}

		@Override
		public void cleanupFromSynchronizations() {
			getNaturalIdXrefDelegate().unStashInvalidNaturalIdReferences();
		}

		@Override
		public void handleEviction(Object object, EntityPersister persister, Serializable identifier) {
			getNaturalIdXrefDelegate().removeNaturalIdCrossReference(
					persister,
					identifier,
					findCachedNaturalId( persister, identifier )
			);
		}
	};

	@Override
	public NaturalIdHelper getNaturalIdHelper() {
		return naturalIdHelper;
	}

	private Object[] getNaturalIdValues(Object[] state, EntityPersister persister) {
		final int[] naturalIdPropertyIndexes = persister.getNaturalIdentifierProperties();
		final Object[] naturalIdValues = new Object[naturalIdPropertyIndexes.length];

		for ( int i = 0; i < naturalIdPropertyIndexes.length; i++ ) {
			naturalIdValues[i] = state[naturalIdPropertyIndexes[i]];
		}

		return naturalIdValues;
	}
}
