/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.internal.ForeignKeys;
import org.hibernate.engine.spi.EntityUniqueKey;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.persister.entity.UniqueKeyLoadable;
import org.hibernate.proxy.HibernateProxy;

/**
 * Base for types which map associations to persistent entities.
 *
 * @author Gavin King
 */
public abstract class EntityType extends AbstractType implements AssociationType {

	private final TypeFactory.TypeScope scope;
	private final String associatedEntityName;
	protected final String uniqueKeyPropertyName;
	private final boolean eager;
	private final boolean unwrapProxy;
	private final boolean referenceToPrimaryKey;

	/**
	 * Cached because of performance
	 *
	 * @see #getIdentifierType
	 * @see #getIdentifierType
	 */
	private transient volatile Type associatedIdentifierType;

	/**
	 * Cached because of performance
	 *
	 * @see #getAssociatedEntityPersister
	 */
	private transient volatile EntityPersister associatedEntityPersister;

	private transient Class returnedClass;

	/**
	 * Constructs the requested entity type mapping.
	 *
	 * @param scope The type scope
	 * @param entityName The name of the associated entity.
	 * @param uniqueKeyPropertyName The property-ref name, or null if we
	 * reference the PK of the associated entity.
	 * @param eager Is eager fetching enabled.
	 * @param unwrapProxy Is unwrapping of proxies allowed for this association; unwrapping
	 * says to return the "implementation target" of lazy proxies; typically only possible
	 * with lazy="no-proxy".
	 *
	 * @deprecated Use {@link #EntityType(org.hibernate.type.TypeFactory.TypeScope, String, boolean, String, boolean, boolean)} instead.
	 */
	@Deprecated
	protected EntityType(
			TypeFactory.TypeScope scope,
			String entityName,
			String uniqueKeyPropertyName,
			boolean eager,
			boolean unwrapProxy) {
		this( scope, entityName, uniqueKeyPropertyName == null, uniqueKeyPropertyName, eager, unwrapProxy );
	}

	/**
	 * Constructs the requested entity type mapping.
	 *
	 * @param scope The type scope
	 * @param entityName The name of the associated entity.
	 * @param referenceToPrimaryKey True if association references a primary key.
	 * @param uniqueKeyPropertyName The property-ref name, or null if we
	 * reference the PK of the associated entity.
	 * @param eager Is eager fetching enabled.
	 * @param unwrapProxy Is unwrapping of proxies allowed for this association; unwrapping
	 * says to return the "implementation target" of lazy proxies; typically only possible
	 * with lazy="no-proxy".
	 */
	protected EntityType(
			TypeFactory.TypeScope scope,
			String entityName,
			boolean referenceToPrimaryKey,
			String uniqueKeyPropertyName,
			boolean eager,
			boolean unwrapProxy) {
		this.scope = scope;
		this.associatedEntityName = entityName;
		this.uniqueKeyPropertyName = uniqueKeyPropertyName;
		this.eager = eager;
		this.unwrapProxy = unwrapProxy;
		this.referenceToPrimaryKey = referenceToPrimaryKey;
	}

	protected EntityType(EntityType original, String superTypeEntityName) {
		this.scope = original.scope;
		this.associatedEntityName = superTypeEntityName;
		this.uniqueKeyPropertyName = original.uniqueKeyPropertyName;
		this.eager = original.eager;
		this.unwrapProxy = original.unwrapProxy;
		this.referenceToPrimaryKey = original.referenceToPrimaryKey;
	}

	protected TypeFactory.TypeScope scope() {
		return scope;
	}

	/**
	 * An entity type is a type of association type
	 *
	 * @return True.
	 */
	@Override
	public boolean isAssociationType() {
		return true;
	}

	/**
	 * Explicitly, an entity type is an entity type ;)
	 *
	 * @return True.
	 */
	@Override
	public final boolean isEntityType() {
		return true;
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	/**
	 * Generates a string representation of this type.
	 *
	 * @return string rep
	 */
	@Override
	public String toString() {
		return getClass().getName() + '(' + getAssociatedEntityName() + ')';
	}

	/**
	 * For entity types, the name correlates to the associated entity name.
	 */
	@Override
	public String getName() {
		return associatedEntityName;
	}

	/**
	 * Does this association foreign key reference the primary key of the other table?
	 * Otherwise, it references a property-ref.
	 *
	 * @return True if this association reference the PK of the associated entity.
	 */
	public boolean isReferenceToPrimaryKey() {
		return referenceToPrimaryKey;
	}

	@Override
	public String getRHSUniqueKeyPropertyName() {
		// Return null if this type references a PK.  This is important for
		// associations' use of mappedBy referring to a derived ID.
		return referenceToPrimaryKey ? null : uniqueKeyPropertyName;
	}

	@Override
	public String getLHSPropertyName() {
		return null;
	}

	public String getPropertyName() {
		return null;
	}

	/**
	 * The name of the associated entity.
	 *
	 * @return The associated entity name.
	 */
	public final String getAssociatedEntityName() {
		return associatedEntityName;
	}

	/**
	 * The name of the associated entity.
	 *
	 * @param factory The session factory, for resolution.
	 *
	 * @return The associated entity name.
	 */
	@Override
	public String getAssociatedEntityName(SessionFactoryImplementor factory) {
		return getAssociatedEntityName();
	}

	/**
	 * Retrieves the {@link Joinable} defining the associated entity.
	 *
	 * @param factory The session factory.
	 *
	 * @return The associated joinable
	 *
	 * @throws MappingException Generally indicates an invalid entity name.
	 */
	@Override
	public Joinable getAssociatedJoinable(SessionFactoryImplementor factory) throws MappingException {
		return (Joinable) getAssociatedEntityPersister( factory );
	}

	/**
	 * This returns the wrong class for an entity with a proxy, or for a named
	 * entity.  Theoretically it should return the proxy class, but it doesn't.
	 * <p/>
	 * The problem here is that we do not necessarily have a ref to the associated
	 * entity persister (nor to the session factory, to look it up) which is really
	 * needed to "do the right thing" here...
	 *
	 * @return The entity class.
	 */
	@Override
	public final Class getReturnedClass() {
		if ( returnedClass == null ) {
			returnedClass = determineAssociatedEntityClass();
		}
		return returnedClass;
	}

	private Class determineAssociatedEntityClass() {
		final String entityName = getAssociatedEntityName();
		try {
			return ReflectHelper.classForName( entityName );
		}
		catch (ClassNotFoundException cnfe) {
			return this.scope.getTypeConfiguration().getSessionFactory().getMetamodel().entityPersister( entityName ).
					getEntityTuplizer().getMappedClass();
		}
	}

	@Override
	public Object nullSafeGet(ResultSet rs, String name, SharedSessionContractImplementor session, Object owner)
			throws HibernateException, SQLException {
		return nullSafeGet( rs, new String[] {name}, session, owner );
	}

	@Override
	public final Object nullSafeGet(
			ResultSet rs,
			String[] names,
			SharedSessionContractImplementor session,
			Object owner) throws HibernateException, SQLException {
		return resolve( hydrate( rs, names, session, owner ), session, owner );
	}

	@Override
	public void nullSafeSet(PreparedStatement st, Object value, int index, boolean[] settable, SharedSessionContractImplementor session)
			throws SQLException {
		if ( settable.length > 0 ) {
			requireIdentifierOrUniqueKeyType( session.getFactory() )
					.nullSafeSet( st, getIdentifier( value, session ), index, settable, session );
		}
	}

	@Override
	public void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session)
			throws SQLException {
		requireIdentifierOrUniqueKeyType( session.getFactory() )
				.nullSafeSet( st, getIdentifier( value, session ), index, session );
	}

	/**
	 * Two entities are considered the same when their instances are the same.
	 *
	 * @param x One entity instance
	 * @param y Another entity instance
	 *
	 * @return True if x == y; false otherwise.
	 */
	@Override
	public final boolean isSame(Object x, Object y) {
		return x == y;
	}

	@Override
	public int compare(Object x, Object y) {
		return 0; //TODO: entities CAN be compared, by PK, fix this! -> only if/when we can extract the id values....
	}

	@Override
	public Object deepCopy(Object value, SessionFactoryImplementor factory) {
		return value; //special case ... this is the leaf of the containment graph, even though not immutable
	}

	@Override
	public Object replace(
			Object original,
			Object target,
			SharedSessionContractImplementor session,
			Object owner,
			Map copyCache) throws HibernateException {
		if ( original == null ) {
			return null;
		}
		Object cached = copyCache.get( original );
		if ( cached != null ) {
			return cached;
		}
		else {
			if ( original == target ) {
				return target;
			}
			if ( session.getContextEntityIdentifier( original ) == null &&
					ForeignKeys.isTransient( associatedEntityName, original, Boolean.FALSE, session ) ) {
				// original is transient; it is possible that original is a "managed" entity that has
				// not been made persistent yet, so check if copyCache contains original as a "managed" value
				// that corresponds with some "merge" value.
				if ( copyCache.containsValue( original ) ) {
					return original;
				}
				else {
					// the transient entity is not "managed"; add the merge/managed pair to copyCache
					final Object copy = session.getEntityPersister( associatedEntityName, original )
							.instantiate( null, session );
					copyCache.put( original, copy );
					return copy;
				}
			}
			else {
				Object id = getIdentifier( original, session );
				if ( id == null ) {
					throw new AssertionFailure(
							"non-transient entity has a null id: " + original.getClass()
									.getName()
					);
				}
				id = getIdentifierOrUniqueKeyType( session.getFactory() )
						.replace( id, null, session, owner, copyCache );
				return resolve( id, session, owner );
			}
		}
	}

	@Override
	public int getHashCode(Object x, SessionFactoryImplementor factory) {
		EntityPersister persister = getAssociatedEntityPersister( factory );
		if ( !persister.canExtractIdOutOfEntity() ) {
			return super.getHashCode( x );
		}

		final Serializable id;
		if ( x instanceof HibernateProxy ) {
			id = ( (HibernateProxy) x ).getHibernateLazyInitializer().getInternalIdentifier();
		}
		else {
			final Class mappedClass = persister.getMappedClass();
			if ( mappedClass.isAssignableFrom( x.getClass() ) ) {
				id = persister.getIdentifier( x );
			}
			else {
				id = (Serializable) x;
			}
		}
		return persister.getIdentifierType().getHashCode( id, factory );
	}

	@Override
	public boolean isEqual(Object x, Object y, SessionFactoryImplementor factory) {
		// associations (many-to-one and one-to-one) can be null...
		if ( x == null || y == null ) {
			return x == y;
		}

		EntityPersister persister = getAssociatedEntityPersister( factory );
		if ( !persister.canExtractIdOutOfEntity() ) {
			return super.isEqual( x, y );
		}

		final Class mappedClass = persister.getMappedClass();
		Serializable xid;
		if ( x instanceof HibernateProxy ) {
			xid = ( (HibernateProxy) x ).getHibernateLazyInitializer()
					.getInternalIdentifier();
		}
		else {
			if ( mappedClass.isAssignableFrom( x.getClass() ) ) {
				xid = persister.getIdentifier( x );
			}
			else {
				//JPA 2 case where @IdClass contains the id and not the associated entity
				xid = (Serializable) x;
			}
		}

		Serializable yid;
		if ( y instanceof HibernateProxy ) {
			yid = ( (HibernateProxy) y ).getHibernateLazyInitializer()
					.getInternalIdentifier();
		}
		else {
			if ( mappedClass.isAssignableFrom( y.getClass() ) ) {
				yid = persister.getIdentifier( y );
			}
			else {
				//JPA 2 case where @IdClass contains the id and not the associated entity
				yid = (Serializable) y;
			}
		}

		return persister.getIdentifierType()
				.isEqual( xid, yid, factory );
	}

	@Override
	public String getOnCondition(String alias, SessionFactoryImplementor factory, Map enabledFilters) {
		return getOnCondition( alias, factory, enabledFilters, null );
	}

	@Override
	public String getOnCondition(
			String alias,
			SessionFactoryImplementor factory,
			Map enabledFilters,
			Set<String> treatAsDeclarations) {
		if ( isReferenceToPrimaryKey() && ( treatAsDeclarations == null || treatAsDeclarations.isEmpty() ) ) {
			return "";
		}
		else {
			return getAssociatedJoinable( factory ).filterFragment( alias, enabledFilters, treatAsDeclarations );
		}
	}

	/**
	 * Resolve an identifier or unique key value
	 */
	@Override
	public Object resolve(Object value, SharedSessionContractImplementor session, Object owner) throws HibernateException {
		return resolve(value, session, owner, null);
	}

	@Override
	public Object resolve(Object value, SharedSessionContractImplementor session, Object owner, Boolean overridingEager) throws HibernateException {
		if ( value != null && !isNull( owner, session ) ) {
			if ( isReferenceToPrimaryKey() ) {
				return resolveIdentifier( (Serializable) value, session, overridingEager );
			}
			else if ( uniqueKeyPropertyName != null ) {
				return loadByUniqueKey( getAssociatedEntityName(), uniqueKeyPropertyName, value, session );
			}
		}

		return null;
	}

	/**
	 * Would an entity be eagerly loaded given the value provided for {@code overridingEager}?
	 *
	 * @param overridingEager can override eager from the mapping.
	 *
	 * @return If {@code overridingEager} is null, then it does not override.
	 *         If true or false then it overrides the mapping value.
	 */
	public boolean isEager(Boolean overridingEager) {
		return overridingEager != null ? overridingEager : this.eager;
	}

	@Override
	public Type getSemiResolvedType(SessionFactoryImplementor factory) {
		return getAssociatedEntityPersister( factory ).getIdentifierType();
	}

	public EntityPersister getAssociatedEntityPersister(final SessionFactoryImplementor factory) {
		final EntityPersister persister = associatedEntityPersister;
		//The following branch implements a simple lazy-initialization, but rather than the canonical
		//form it returns the local variable to avoid a second volatile read: associatedEntityPersister
		//needs to be volatile as the initialization might happen by a different thread than the readers.
		if ( persister == null ) {
			associatedEntityPersister = factory.getMetamodel().entityPersister( getAssociatedEntityName() );
			return associatedEntityPersister;
		}
		else {
			return persister;
		}
	}

	protected final Object getIdentifier(Object value, SharedSessionContractImplementor session) throws HibernateException {
		if ( isReferenceToIdentifierProperty() ) {
			return ForeignKeys.getEntityIdentifierIfNotUnsaved(
					getAssociatedEntityName(),
					value,
					session
			); //tolerates nulls
		}
		else if ( value == null ) {
			return null;
		}
		else {
			EntityPersister entityPersister = getAssociatedEntityPersister( session.getFactory() );
			Object propertyValue = entityPersister.getPropertyValue( value, uniqueKeyPropertyName );
			// We now have the value of the property-ref we reference.  However,
			// we need to dig a little deeper, as that property might also be
			// an entity type, in which case we need to resolve its identifier
			Type type = entityPersister.getPropertyType( uniqueKeyPropertyName );
			if ( type.isEntityType() ) {
				propertyValue = ( (EntityType) type ).getIdentifier( propertyValue, session );
			}

			return propertyValue;
		}
	}

	/**
	 * Generate a loggable representation of an instance of the value mapped by this type.
	 *
	 * @param value The instance to be logged.
	 * @param factory The session factory.
	 *
	 * @return The loggable string.
	 *
	 * @throws HibernateException Generally some form of resolution problem.
	 */
	@Override
	public String toLoggableString(Object value, SessionFactoryImplementor factory) {
		if ( value == null ) {
			return "null";
		}

		final EntityPersister persister = getAssociatedEntityPersister( factory );
		if ( !persister.getEntityTuplizer().isInstance( value ) ) {
			// it should be the id type...
			if ( persister.getIdentifierType().getReturnedClass().isInstance( value ) ) {
				return associatedEntityName + "#" + value;
			}
		}

		final StringBuilder result = new StringBuilder().append( associatedEntityName );

		if ( persister.hasIdentifierProperty() ) {
			final Serializable id;
			if ( value instanceof HibernateProxy ) {
				HibernateProxy proxy = (HibernateProxy) value;
				id = proxy.getHibernateLazyInitializer().getInternalIdentifier();
			}
			else {
				id = persister.getIdentifier( value );
			}

			result.append( '#' )
					.append( persister.getIdentifierType().toLoggableString( id, factory ) );
		}

		return result.toString();
	}

	/**
	 * Is the association modeled here defined as a 1-1 in the database (physical model)?
	 *
	 * @return True if a 1-1 in the database; false otherwise.
	 */
	public abstract boolean isOneToOne();

	/**
	 * Is the association modeled here a 1-1 according to the logical model?
	 *
	 * @return True if a 1-1 in the logical model; false otherwise.
	 */
	public boolean isLogicalOneToOne() {
		return isOneToOne();
	}

	/**
	 * Convenience method to locate the identifier type of the associated entity.
	 *
	 * @param factory The mappings...
	 *
	 * @return The identifier type
	 */
	Type getIdentifierType(final Mapping factory) {
		final Type type = associatedIdentifierType;
		//The following branch implements a simple lazy-initialization, but rather than the canonical
		//form it returns the local variable to avoid a second volatile read: associatedIdentifierType
		//needs to be volatile as the initialization might happen by a different thread than the readers.
		if ( type == null ) {
			associatedIdentifierType = factory.getIdentifierType( getAssociatedEntityName() );
			return associatedIdentifierType;
		}
		else {
			return type;
		}
	}

	/**
	 * Convenience method to locate the identifier type of the associated entity.
	 *
	 * @param session The originating session
	 *
	 * @return The identifier type
	 */
	Type getIdentifierType(final SharedSessionContractImplementor session) {
		final Type type = associatedIdentifierType;
		if ( type == null ) {
			associatedIdentifierType = getIdentifierType( session.getFactory() );
			return associatedIdentifierType;
		}
		else {
			return type;
		}
	}

	/**
	 * Determine the type of either (1) the identifier if we reference the
	 * associated entity's PK or (2) the unique key to which we refer (i.e.
	 * the property-ref).
	 *
	 * @param factory The mappings...
	 *
	 * @return The appropriate type.
	 *
	 * @throws MappingException Generally, if unable to resolve the associated entity name
	 * or unique key property name.
	 */
	public final Type getIdentifierOrUniqueKeyType(Mapping factory) throws MappingException {
		if ( isReferenceToIdentifierProperty() ) {
			return getIdentifierType( factory );
		}
		else {
			Type type = factory.getReferencedPropertyType( getAssociatedEntityName(), uniqueKeyPropertyName );
			if ( type.isEntityType() ) {
				type = ( (EntityType) type ).getIdentifierOrUniqueKeyType( factory );
			}
			return type;
		}
	}

	/**
	 * The name of the property on the associated entity to which our FK
	 * refers
	 *
	 * @param factory The mappings...
	 *
	 * @return The appropriate property name.
	 *
	 * @throws MappingException Generally, if unable to resolve the associated entity name
	 */
	public final String getIdentifierOrUniqueKeyPropertyName(Mapping factory)
			throws MappingException {
		return isReferenceToIdentifierProperty()
				? factory.getIdentifierPropertyName( getAssociatedEntityName() )
				: uniqueKeyPropertyName;
	}

	public boolean isReferenceToIdentifierProperty() {
		return isReferenceToPrimaryKey()
			|| uniqueKeyPropertyName == null;
	}

	/**
	 * The nullability of the property.
	 *
	 * @return The nullability of the property.
	 */
	public abstract boolean isNullable();

	/**
	 * Resolve an identifier via a load.
	 *
	 * @param id The entity id to resolve
	 * @param session The originating session.
	 *
	 * @return The resolved identifier (i.e., loaded entity).
	 *
	 * @throws org.hibernate.HibernateException Indicates problems performing the load.
	 */
	protected final Object resolveIdentifier(Serializable id, SharedSessionContractImplementor session, Boolean overridingEager) throws HibernateException {

		boolean isProxyUnwrapEnabled = unwrapProxy &&
				getAssociatedEntityPersister( session.getFactory() )
						.isInstrumented();

		Object proxyOrEntity = session.internalLoad(
				getAssociatedEntityName(),
				id,
				isEager( overridingEager ),
				isNullable()
		);

		if ( proxyOrEntity instanceof HibernateProxy ) {
			( (HibernateProxy) proxyOrEntity ).getHibernateLazyInitializer()
					.setUnwrap( isProxyUnwrapEnabled );
		}

		return proxyOrEntity;
	}

	protected final Object resolveIdentifier(Serializable id, SharedSessionContractImplementor session) throws HibernateException {
		return resolveIdentifier( id, session, null );
	}

	protected boolean isNull(Object owner, SharedSessionContractImplementor session) {
		return false;
	}

	/**
	 * Load an instance by a unique key that is not the primary key.
	 *
	 * @param entityName The name of the entity to load
	 * @param uniqueKeyPropertyName The name of the property defining the unique key.
	 * @param key The unique key property value.
	 * @param session The originating session.
	 *
	 * @return The loaded entity
	 *
	 * @throws HibernateException generally indicates problems performing the load.
	 */
	public Object loadByUniqueKey(
			String entityName,
			String uniqueKeyPropertyName,
			Object key,
			SharedSessionContractImplementor session) throws HibernateException {
		final SessionFactoryImplementor factory = session.getFactory();
		UniqueKeyLoadable persister = (UniqueKeyLoadable) factory.getMetamodel().entityPersister( entityName );

		//TODO: implement 2nd level caching?! natural id caching ?! proxies?!

		EntityUniqueKey euk = new EntityUniqueKey(
				entityName,
				uniqueKeyPropertyName,
				key,
				getIdentifierOrUniqueKeyType( factory ),
				persister.getEntityMode(),
				session.getFactory()
		);

		final PersistenceContext persistenceContext = session.getPersistenceContextInternal();
		Object result = persistenceContext.getEntity( euk );
		if ( result == null ) {
			result = persister.loadByUniqueKey( uniqueKeyPropertyName, key, session );

			// If the entity was not in the Persistence Context, but was found now,
			// add it to the Persistence Context
			if (result != null) {
				persistenceContext.addEntity(euk, result);
			}
		}

		return result == null ? null : persistenceContext.proxyFor( result );
	}

	protected Type requireIdentifierOrUniqueKeyType(Mapping mapping) {
		final Type fkTargetType = getIdentifierOrUniqueKeyType( mapping );
		if ( fkTargetType == null ) {
			throw new MappingException(
					"Unable to determine FK target Type for many-to-one or one-to-one mapping: " +
							"referenced-entity-name=[" + getAssociatedEntityName() +
							"], referenced-entity-attribute-name=[" + getLHSPropertyName() + "]"
			);
		}
		return fkTargetType;
	}
}
