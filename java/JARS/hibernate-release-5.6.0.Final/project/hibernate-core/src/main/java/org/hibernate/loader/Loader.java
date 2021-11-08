/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader;

import java.io.Serializable;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.QueryException;
import org.hibernate.ScrollMode;
import org.hibernate.Session;
import org.hibernate.StaleObjectStateException;
import org.hibernate.WrongClassException;
import org.hibernate.bytecode.enhance.spi.interceptor.EnhancementAsProxyLazinessInterceptor;
import org.hibernate.cache.spi.FilterKey;
import org.hibernate.cache.spi.QueryKey;
import org.hibernate.cache.spi.QueryResultsCache;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.entry.CacheEntry;
import org.hibernate.cache.spi.entry.ReferenceCacheEntryImpl;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.LimitHelper;
import org.hibernate.dialect.pagination.NoopLimitHandler;
import org.hibernate.engine.internal.CacheHelper;
import org.hibernate.engine.internal.TwoPhaseLoad;
import org.hibernate.engine.jdbc.ColumnNameCache;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.loading.internal.CollectionLoadContext;
import org.hibernate.engine.spi.BatchFetchQueue;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.EntityUniqueKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.RowSelection;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.engine.spi.SubselectFetch;
import org.hibernate.engine.spi.TypedValue;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.PostLoadEvent;
import org.hibernate.event.spi.PreLoadEvent;
import org.hibernate.hql.internal.HolderInstantiator;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.FetchingScrollableResultsImpl;
import org.hibernate.internal.ScrollableResultsImpl;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.loader.spi.AfterLoadAction;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Loadable;
import org.hibernate.persister.entity.UniqueKeyLoadable;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.query.spi.ScrollableResultsImplementor;
import org.hibernate.stat.spi.StatisticsImplementor;
import org.hibernate.transform.CacheableResultTransformer;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.type.AssociationType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;
import org.hibernate.type.VersionType;

/**
 * Abstract superclass of object loading (and querying) strategies. This class implements
 * useful common functionality that concrete loaders delegate to. It is not intended that this
 * functionality would be directly accessed by client code. (Hence, all methods of this class
 * are declared <tt>protected</tt> or <tt>private</tt>.) This class relies heavily upon the
 * <tt>Loadable</tt> interface, which is the contract between this class and
 * <tt>EntityPersister</tt>s that may be loaded by it.<br>
 * <br>
 * The present implementation is able to load any number of columns of entities and at most
 * one collection role per query.
 *
 * @author Gavin King
 * @see org.hibernate.persister.entity.Loadable
 */
public abstract class Loader {

	public static final String SELECT = "select";
	public static final String SELECT_DISTINCT = "select distinct";

	protected static final CoreMessageLogger LOG = CoreLogging.messageLogger( Loader.class );

	private final SessionFactoryImplementor factory;
	private volatile ColumnNameCache columnNameCache;

	private boolean isJdbc4 = true;

	public Loader(SessionFactoryImplementor factory) {
		this.factory = factory;
	}

	/**
	 * The SQL query string to be called; implemented by all subclasses
	 *
	 * @return The sql command this loader should use to get its {@link ResultSet}.
	 */
	public abstract String getSQLString();

	/**
	 * An array of persisters of entity classes contained in each row of results;
	 * implemented by all subclasses
	 *
	 * @return The entity persisters.
	 */
	protected abstract Loadable[] getEntityPersisters();

	/**
	 * An array indicating whether the entities have eager property fetching
	 * enabled for all of their properties.
	 * <p>
	 * Supersedes {@link #getEntityEagerPerPropertyFetches()}.
	 *
	 * @return Eager property fetching indicators.
	 */
	protected boolean[] getEntityEagerPropertyFetches() {
		return null;
	}

	/**
	 * An array indicating for each entity which specific properties must have eager fetching enabled.
	 * <p>
	 * Superseded by {@link #getEntityEagerPropertyFetches()}.
	 *
	 * @return Eager property fetching indicators.
	 */
	protected boolean[][] getEntityEagerPerPropertyFetches() {
		return null;
	}

	/**
	 * An array of indexes of the entity that owns a one-to-one association
	 * to the entity at the given index (-1 if there is no "owner").  The
	 * indexes contained here are relative to the result of
	 * {@link #getEntityPersisters}.
	 *
	 * @return The owner indicators (see discussion above).
	 */
	protected int[] getOwners() {
		return null;
	}

	/**
	 * An array of the owner types corresponding to the {@link #getOwners()}
	 * returns.  Indices indicating no owner would be null here.
	 *
	 * @return The types for the owners.
	 */
	protected EntityType[] getOwnerAssociationTypes() {
		return null;
	}

	/**
	 * An (optional) persister for a collection to be initialized; only
	 * collection loaders return a non-null value
	 */
	protected CollectionPersister[] getCollectionPersisters() {
		return null;
	}

	/**
	 * Get the index of the entity that owns the collection, or -1
	 * if there is no owner in the query results (ie. in the case of a
	 * collection initializer) or no collection.
	 */
	protected int[] getCollectionOwners() {
		return null;
	}

	protected int[][] getCompositeKeyManyToOneTargetIndices() {
		return null;
	}

	/**
	 * What lock options does this load entities with?
	 *
	 * @param lockOptions a collection of lock options specified dynamically via the Query interface
	 */
	//protected abstract LockOptions[] getLockOptions(Map lockOptions);
	protected abstract LockMode[] getLockModes(LockOptions lockOptions);

	/**
	 * Append <tt>FOR UPDATE OF</tt> clause, if necessary. This
	 * empty superclass implementation merely returns its first
	 * argument.
	 */
	protected String applyLocks(
			String sql,
			QueryParameters parameters,
			Dialect dialect,
			List<AfterLoadAction> afterLoadActions) throws HibernateException {
		return sql;
	}

	/**
	 * Does this query return objects that might be already cached
	 * by the session, whose lock mode may need upgrading
	 */
	protected boolean upgradeLocks() {
		return false;
	}

	/**
	 * Return false is this loader is a batch entity loader
	 */
	protected boolean isSingleRowLoader() {
		return false;
	}

	/**
	 * Get the SQL table aliases of entities whose
	 * associations are subselect-loadable, returning
	 * null if this loader does not support subselect
	 * loading
	 */
	protected String[] getAliases() {
		return null;
	}

	/**
	 * Modify the SQL, adding lock hints and comments, if necessary
	 */
	protected String preprocessSQL(
			String sql,
			QueryParameters parameters,
			SessionFactoryImplementor sessionFactory,
			List<AfterLoadAction> afterLoadActions) throws HibernateException {

		Dialect dialect = sessionFactory.getServiceRegistry().getService( JdbcServices.class ).getDialect();

		sql = applyLocks( sql, parameters, dialect, afterLoadActions );

		sql = dialect.addSqlHintOrComment(
			sql,
			parameters,
			sessionFactory.getSessionFactoryOptions().isCommentsEnabled()
		);

		return processDistinctKeyword( sql, parameters );
	}

	protected boolean shouldUseFollowOnLocking(
			QueryParameters parameters,
			Dialect dialect,
			List<AfterLoadAction> afterLoadActions) {
		if ( ( parameters.getLockOptions().getFollowOnLocking() == null && dialect.useFollowOnLocking( parameters ) ) ||
				( parameters.getLockOptions().getFollowOnLocking() != null && parameters.getLockOptions().getFollowOnLocking() ) ) {
			// currently only one lock mode is allowed in follow-on locking
			final LockMode lockMode = determineFollowOnLockMode( parameters.getLockOptions() );
			final LockOptions lockOptions = new LockOptions( lockMode );
			if ( lockOptions.getLockMode() != LockMode.UPGRADE_SKIPLOCKED ) {
				if ( lockOptions.getLockMode() != LockMode.NONE ) {
					LOG.usingFollowOnLocking();
				}
				lockOptions.setTimeOut( parameters.getLockOptions().getTimeOut() );
				lockOptions.setScope( parameters.getLockOptions().getScope() );
				afterLoadActions.add(
						new AfterLoadAction() {
							@Override
							public void afterLoad(SharedSessionContractImplementor session, Object entity, Loadable persister) {
								( (Session) session ).buildLockRequest( lockOptions ).lock(
										persister.getEntityName(),
										entity
								);
							}
						}
				);
				parameters.setLockOptions( new LockOptions() );
				return true;
			}
		}
		return false;
	}

	protected LockMode determineFollowOnLockMode(LockOptions lockOptions) {
		final LockMode lockModeToUse = lockOptions.findGreatestLockMode();

		if ( lockOptions.hasAliasSpecificLockModes() ) {
			if ( lockOptions.getLockMode() == LockMode.NONE && lockModeToUse == LockMode.NONE ) {
				return lockModeToUse;
			}
			else {
				LOG.aliasSpecificLockingWithFollowOnLocking( lockModeToUse );
			}
		}
		return lockModeToUse;
	}

	/**
	 * Execute an SQL query and attempt to instantiate instances of the class mapped by the given
	 * persister from each row of the <tt>ResultSet</tt>. If an object is supplied, will attempt to
	 * initialize that object. If a collection is supplied, attempt to initialize that collection.
	 */
	public List doQueryAndInitializeNonLazyCollections(
			final SharedSessionContractImplementor session,
			final QueryParameters queryParameters,
			final boolean returnProxies) throws HibernateException, SQLException {
		return doQueryAndInitializeNonLazyCollections(
				session,
				queryParameters,
				returnProxies,
				null
		);
	}

	public List doQueryAndInitializeNonLazyCollections(
			final SharedSessionContractImplementor session,
			final QueryParameters queryParameters,
			final boolean returnProxies,
			final ResultTransformer forcedResultTransformer)
			throws HibernateException, SQLException {
		final PersistenceContext persistenceContext = session.getPersistenceContext();
		boolean defaultReadOnlyOrig = persistenceContext.isDefaultReadOnly();
		if ( queryParameters.isReadOnlyInitialized() ) {
			// The read-only/modifiable mode for the query was explicitly set.
			// Temporarily set the default read-only/modifiable setting to the query's setting.
			persistenceContext.setDefaultReadOnly( queryParameters.isReadOnly() );
		}
		else {
			// The read-only/modifiable setting for the query was not initialized.
			// Use the default read-only/modifiable from the persistence context instead.
			queryParameters.setReadOnly( persistenceContext.isDefaultReadOnly() );
		}
		persistenceContext.beforeLoad();
		List result;
		try {
			try {
				result = doQuery( session, queryParameters, returnProxies, forcedResultTransformer );
			}
			finally {
				persistenceContext.afterLoad();
			}
			persistenceContext.initializeNonLazyCollections();
		}
		finally {
			// Restore the original default
			persistenceContext.setDefaultReadOnly( defaultReadOnlyOrig );
		}
		return result;
	}

	/**
	 * Loads a single row from the result set.  This is the processing used from the
	 * ScrollableResults where no collection fetches were encountered.
	 *
	 * @param resultSet The result set from which to do the load.
	 * @param session The session from which the request originated.
	 * @param queryParameters The query parameters specified by the user.
	 * @param returnProxies Should proxies be generated
	 *
	 * @return The loaded "row".
	 *
	 * @throws HibernateException
	 */
	public Object loadSingleRow(
			final ResultSet resultSet,
			final SharedSessionContractImplementor session,
			final QueryParameters queryParameters,
			final boolean returnProxies) throws HibernateException {

		final int entitySpan = getEntityPersisters().length;
		final List hydratedObjects = entitySpan == 0 ?
				null : new ArrayList( entitySpan );

		final Object result;
		try {
			result = getRowFromResultSet(
					resultSet,
					session,
					queryParameters,
					getLockModes( queryParameters.getLockOptions() ),
					null,
					hydratedObjects,
					new EntityKey[entitySpan],
					returnProxies
			);
		}
		catch (SQLException sqle) {
			throw factory.getJdbcServices().getSqlExceptionHelper().convert(
					sqle,
					"could not read next row of results",
					getSQLString()
			);
		}

		initializeEntitiesAndCollections(
				hydratedObjects,
				resultSet,
				session,
				queryParameters.isReadOnly( session )
		);
		session.getPersistenceContextInternal().initializeNonLazyCollections();
		return result;
	}

	private Object sequentialLoad(
			final ResultSet resultSet,
			final SharedSessionContractImplementor session,
			final QueryParameters queryParameters,
			final boolean returnProxies,
			final EntityKey keyToRead) throws HibernateException {

		final int entitySpan = getEntityPersisters().length;
		final List hydratedObjects = entitySpan == 0 ?
				null : new ArrayList( entitySpan );

		Object result = null;
		final EntityKey[] loadedKeys = new EntityKey[entitySpan];

		try {
			do {
				Object loaded = getRowFromResultSet(
						resultSet,
						session,
						queryParameters,
						getLockModes( queryParameters.getLockOptions() ),
						null,
						hydratedObjects,
						loadedKeys,
						returnProxies
				);
				if ( !keyToRead.equals( loadedKeys[0] ) ) {
					throw new AssertionFailure(
							String.format(
									"Unexpected key read for row; expected [%s]; actual [%s]",
									keyToRead,
									loadedKeys[0]
							)
					);
				}
				if ( result == null ) {
					result = loaded;
				}
			}
			while ( resultSet.next() &&
					isCurrentRowForSameEntity( keyToRead, 0, resultSet, session ) );
		}
		catch (SQLException sqle) {
			throw factory.getJdbcServices().getSqlExceptionHelper().convert(
					sqle,
					"could not doAfterTransactionCompletion sequential read of results (forward)",
					getSQLString()
			);
		}

		initializeEntitiesAndCollections(
				hydratedObjects,
				resultSet,
				session,
				queryParameters.isReadOnly( session )
		);
		session.getPersistenceContextInternal().initializeNonLazyCollections();
		return result;
	}

	private boolean isCurrentRowForSameEntity(
			final EntityKey keyToRead,
			final int persisterIndex,
			final ResultSet resultSet,
			final SharedSessionContractImplementor session) throws SQLException {
		EntityKey currentRowKey = getKeyFromResultSet(
				persisterIndex, getEntityPersisters()[persisterIndex], null, resultSet, session
		);
		return keyToRead.equals( currentRowKey );
	}

	/**
	 * Loads a single logical row from the result set moving forward.  This is the
	 * processing used from the ScrollableResults where there were collection fetches
	 * encountered; thus a single logical row may have multiple rows in the underlying
	 * result set.
	 *
	 * @param resultSet The result set from which to do the load.
	 * @param session The session from which the request originated.
	 * @param queryParameters The query parameters specified by the user.
	 * @param returnProxies Should proxies be generated
	 *
	 * @return The loaded "row".
	 *
	 * @throws HibernateException
	 */
	public Object loadSequentialRowsForward(
			final ResultSet resultSet,
			final SharedSessionContractImplementor session,
			final QueryParameters queryParameters,
			final boolean returnProxies) throws HibernateException {

		// note that for sequential scrolling, we make the assumption that
		// the first persister element is the "root entity"

		try {
			if ( resultSet.isAfterLast() ) {
				// don't even bother trying to read further
				return null;
			}

			if ( resultSet.isBeforeFirst() ) {
				resultSet.next();
			}

			// We call getKeyFromResultSet() here so that we can know the
			// key value upon which to perform the breaking logic.  However,
			// it is also then called from getRowFromResultSet() which is certainly
			// not the most efficient.  But the call here is needed, and there
			// currently is no other way without refactoring of the doQuery()/getRowFromResultSet()
			// methods
			final EntityKey currentKey = getKeyFromResultSet(
					0,
					getEntityPersisters()[0],
					null,
					resultSet,
					session
			);

			return sequentialLoad( resultSet, session, queryParameters, returnProxies, currentKey );
		}
		catch (SQLException sqle) {
			throw factory.getJdbcServices().getSqlExceptionHelper().convert(
					sqle,
					"could not perform sequential read of results (forward)",
					getSQLString()
			);
		}
	}

	/**
	 * Loads a single logical row from the result set moving forward.  This is the
	 * processing used from the ScrollableResults where there were collection fetches
	 * encountered; thus a single logical row may have multiple rows in the underlying
	 * result set.
	 *
	 * @param resultSet The result set from which to do the load.
	 * @param session The session from which the request originated.
	 * @param queryParameters The query parameters specified by the user.
	 * @param returnProxies Should proxies be generated
	 *
	 * @return The loaded "row".
	 *
	 * @throws HibernateException
	 */
	public Object loadSequentialRowsReverse(
			final ResultSet resultSet,
			final SharedSessionContractImplementor session,
			final QueryParameters queryParameters,
			final boolean returnProxies,
			final boolean isLogicallyAfterLast) throws HibernateException {

		// note that for sequential scrolling, we make the assumption that
		// the first persister element is the "root entity"

		try {
			if ( resultSet.isFirst() ) {
				// don't even bother trying to read any further
				return null;
			}

			EntityKey keyToRead = null;
			// This check is needed since processing leaves the cursor
			// after the last physical row for the current logical row;
			// thus if we are after the last physical row, this might be
			// caused by either:
			//      1) scrolling to the last logical row
			//      2) scrolling past the last logical row
			// In the latter scenario, the previous logical row
			// really is the last logical row.
			//
			// In all other cases, we should process back two
			// logical records (the current logic row, plus the
			// previous logical row).
			if ( resultSet.isAfterLast() && isLogicallyAfterLast ) {
				// position cursor to the last row
				resultSet.last();
				keyToRead = getKeyFromResultSet(
						0,
						getEntityPersisters()[0],
						null,
						resultSet,
						session
				);
			}
			else {
				// Since the result set cursor is always left at the first
				// physical row after the "last processed", we need to jump
				// back one position to get the key value we are interested
				// in skipping
				resultSet.previous();

				// sequentially read the result set in reverse until we recognize
				// a change in the key value.  At that point, we are pointed at
				// the last physical sequential row for the logical row in which
				// we are interested in processing
				boolean firstPass = true;
				final EntityKey lastKey = getKeyFromResultSet(
						0,
						getEntityPersisters()[0],
						null,
						resultSet,
						session
				);
				while ( resultSet.previous() ) {
					EntityKey checkKey = getKeyFromResultSet(
							0,
							getEntityPersisters()[0],
							null,
							resultSet,
							session
					);

					if ( firstPass ) {
						firstPass = false;
						keyToRead = checkKey;
					}

					if ( !lastKey.equals( checkKey ) ) {
						break;
					}
				}

			}

			// Read backwards until we read past the first physical sequential
			// row with the key we are interested in loading
			while ( resultSet.previous() ) {
				EntityKey checkKey = getKeyFromResultSet(
						0,
						getEntityPersisters()[0],
						null,
						resultSet,
						session
				);

				if ( !keyToRead.equals( checkKey ) ) {
					break;
				}
			}

			// Finally, read ahead one row to position result set cursor
			// at the first physical row we are interested in loading
			resultSet.next();

			// and doAfterTransactionCompletion the load
			return sequentialLoad( resultSet, session, queryParameters, returnProxies, keyToRead );
		}
		catch (SQLException sqle) {
			throw factory.getJdbcServices().getSqlExceptionHelper().convert(
					sqle,
					"could not doAfterTransactionCompletion sequential read of results (forward)",
					getSQLString()
			);
		}
	}

	protected static EntityKey getOptionalObjectKey(QueryParameters queryParameters, SharedSessionContractImplementor session) {
		final Object optionalObject = queryParameters.getOptionalObject();
		final Serializable optionalId = queryParameters.getOptionalId();
		final String optionalEntityName = queryParameters.getOptionalEntityName();

		if ( optionalObject != null && optionalEntityName != null ) {
			return session.generateEntityKey(
					optionalId, session.getEntityPersister(
							optionalEntityName,
							optionalObject
					)
			);
		}
		else {
			return null;
		}

	}

	private Object getRowFromResultSet(
			final ResultSet resultSet,
			final SharedSessionContractImplementor session,
			final QueryParameters queryParameters,
			final LockMode[] lockModesArray,
			final EntityKey optionalObjectKey,
			final List hydratedObjects,
			final EntityKey[] keys,
			boolean returnProxies) throws SQLException, HibernateException {
		return getRowFromResultSet(
				resultSet,
				session,
				queryParameters,
				lockModesArray,
				optionalObjectKey,
				hydratedObjects,
				keys,
				returnProxies,
				null
		);
	}

	private Object getRowFromResultSet(
			final ResultSet resultSet,
			final SharedSessionContractImplementor session,
			final QueryParameters queryParameters,
			final LockMode[] lockModesArray,
			final EntityKey optionalObjectKey,
			final List hydratedObjects,
			final EntityKey[] keys,
			boolean returnProxies,
			ResultTransformer forcedResultTransformer) throws SQLException, HibernateException {
		final Loadable[] persisters = getEntityPersisters();
		final int entitySpan = persisters.length;
		extractKeysFromResultSet(
				persisters,
				queryParameters,
				resultSet,
				session,
				keys,
				lockModesArray,
				hydratedObjects
		);

		registerNonExists( keys, persisters, session );

		// this call is side-effecty
		Object[] row = getRow(
				resultSet,
				persisters,
				keys,
				queryParameters.getOptionalObject(),
				optionalObjectKey,
				lockModesArray,
				hydratedObjects,
				session
		);

		readCollectionElements( row, resultSet, session );

		if ( returnProxies ) {
			// now get an existing proxy for each row element (if there is one)
			final PersistenceContext persistenceContext = session.getPersistenceContextInternal();
			for ( int i = 0; i < entitySpan; i++ ) {
				Object entity = row[i];
				Object proxy = persistenceContext.proxyFor( persisters[i], keys[i], entity );
				if ( entity != proxy ) {
					// force the proxy to resolve itself
					( (HibernateProxy) proxy ).getHibernateLazyInitializer().setImplementation( entity );
					row[i] = proxy;
				}
			}
		}

		applyPostLoadLocks( row, lockModesArray, session );

		return forcedResultTransformer == null
				? getResultColumnOrRow( row, queryParameters.getResultTransformer(), resultSet, session )
				: forcedResultTransformer.transformTuple(
				getResultRow( row, resultSet, session ),
				getResultRowAliases()
		)
				;
	}

	protected void extractKeysFromResultSet(
			Loadable[] persisters,
			QueryParameters queryParameters,
			ResultSet resultSet,
			SharedSessionContractImplementor session,
			EntityKey[] keys,
			LockMode[] lockModes,
			List hydratedObjects) throws SQLException {
		final int entitySpan = persisters.length;

		final int numberOfPersistersToProcess;
		final Serializable optionalId = queryParameters.getOptionalId();
		if ( isSingleRowLoader() && optionalId != null ) {
			keys[entitySpan - 1] = session.generateEntityKey( optionalId, persisters[entitySpan - 1] );
			// skip the last persister below...
			numberOfPersistersToProcess = entitySpan - 1;
		}
		else {
			numberOfPersistersToProcess = entitySpan;
		}

		final Object[] hydratedKeyState = new Object[numberOfPersistersToProcess];

		for ( int i = 0; i < numberOfPersistersToProcess; i++ ) {
			final Type idType = persisters[i].getIdentifierType();
			hydratedKeyState[i] = idType.hydrate(
					resultSet,
					getEntityAliases()[i].getSuffixedKeyAliases(),
					session,
					null
			);
		}

		for ( int i = 0; i < numberOfPersistersToProcess; i++ ) {
			final Type idType = persisters[i].getIdentifierType();
			if ( idType.isComponentType() && getCompositeKeyManyToOneTargetIndices() != null ) {
				// we may need to force resolve any key-many-to-one(s)
				int[] keyManyToOneTargetIndices = getCompositeKeyManyToOneTargetIndices()[i];
				// todo : better solution is to order the index processing based on target indices
				//		that would account for multiple levels whereas this scheme does not
				if ( keyManyToOneTargetIndices != null ) {
					for ( int targetIndex : keyManyToOneTargetIndices ) {
						if ( targetIndex < numberOfPersistersToProcess ) {
							final Type targetIdType = persisters[targetIndex].getIdentifierType();
							final Serializable targetId = (Serializable) targetIdType.resolve(
									hydratedKeyState[targetIndex],
									session,
									null
							);
							// todo : need a way to signal that this key is resolved and its data resolved
							keys[targetIndex] = session.generateEntityKey( targetId, persisters[targetIndex] );
						}

						// this part copied from #getRow, this section could be refactored out
						Object object = session.getEntityUsingInterceptor( keys[targetIndex] );
						if ( object != null ) {
							//its already loaded so don't need to hydrate it
							instanceAlreadyLoaded(
									resultSet,
									targetIndex,
									persisters[targetIndex],
									keys[targetIndex],
									object,
									lockModes[targetIndex],
									hydratedObjects,
									session
							);
						}
						else {
							instanceNotYetLoaded(
									resultSet,
									targetIndex,
									persisters[targetIndex],
									getEntityAliases()[targetIndex].getRowIdAlias(),
									keys[targetIndex],
									lockModes[targetIndex],
									getOptionalObjectKey( queryParameters, session ),
									queryParameters.getOptionalObject(),
									hydratedObjects,
									session
							);
						}
					}
				}
			}
			// If hydratedKeyState[i] is null, then we know the association should be null.
			// Don't bother resolving the ID if hydratedKeyState[i] is null.

			// Implementation note: if the ID is a composite ID, then resolving a null value will
			// result in instantiating an empty composite if AvailableSettings#CREATE_EMPTY_COMPOSITES_ENABLED
			// is true. By not resolving a null value for a composite ID, we avoid the overhead of instantiating
			// an empty composite, checking if it is equivalent to null (it should be), then ultimately throwing
			// out the empty value.
			final Serializable resolvedId;
			if ( hydratedKeyState[i] != null ) {
				resolvedId = (Serializable) idType.resolve( hydratedKeyState[i], session, null );
			}
			else {
				resolvedId = null;
			}
			keys[i] = resolvedId == null ? null : session.generateEntityKey( resolvedId, persisters[i] );
		}
	}

	protected void applyPostLoadLocks(Object[] row, LockMode[] lockModesArray, SharedSessionContractImplementor session) {
	}

	/**
	 * Read any collection elements contained in a single row of the result set
	 */
	private void readCollectionElements(Object[] row, ResultSet resultSet, SharedSessionContractImplementor session)
			throws SQLException, HibernateException {

		//TODO: make this handle multiple collection roles!

		final CollectionPersister[] collectionPersisters = getCollectionPersisters();
		if ( collectionPersisters != null ) {

			final CollectionAliases[] descriptors = getCollectionAliases();
			final int[] collectionOwners = getCollectionOwners();

			for ( int i = 0; i < collectionPersisters.length; i++ ) {

				final boolean hasCollectionOwners = collectionOwners != null &&
						collectionOwners[i] > -1;
				//true if this is a query and we are loading multiple instances of the same collection role
				//otherwise this is a CollectionInitializer and we are loading up a single collection or batch

				final Object owner = hasCollectionOwners ?
						row[collectionOwners[i]] :
						null; //if null, owner will be retrieved from session

				final CollectionPersister collectionPersister = collectionPersisters[i];
				final Serializable key;
				if ( owner == null ) {
					key = null;
				}
				else {
					key = collectionPersister.getCollectionType().getKeyOfOwner( owner, session );
					//TODO: old version did not require hashmap lookup:
					//keys[collectionOwner].getIdentifier()
				}

				readCollectionElement(
						owner,
						key,
						collectionPersister,
						descriptors[i],
						resultSet,
						session
				);

			}

		}
	}

	private List doQuery(
			final SharedSessionContractImplementor session,
			final QueryParameters queryParameters,
			final boolean returnProxies,
			final ResultTransformer forcedResultTransformer) throws SQLException, HibernateException {

		final RowSelection selection = queryParameters.getRowSelection();
		final int maxRows = LimitHelper.hasMaxRows( selection ) ?
				selection.getMaxRows() :
				Integer.MAX_VALUE;

		final List<AfterLoadAction> afterLoadActions = new ArrayList<AfterLoadAction>();

		final SqlStatementWrapper wrapper = executeQueryStatement( queryParameters, false, afterLoadActions, session );
		final ResultSet rs = wrapper.getResultSet();
		final Statement st = wrapper.getStatement();

// would be great to move all this below here into another method that could also be used
// from the new scrolling stuff.
//
// Would need to change the way the max-row stuff is handled (i.e. behind an interface) so
// that I could do the control breaking at the means to know when to stop

		try {
			return processResultSet(
					rs,
					queryParameters,
					session,
					returnProxies,
					forcedResultTransformer,
					maxRows,
					afterLoadActions
			);
		}
		finally {
			final JdbcCoordinator jdbcCoordinator = session.getJdbcCoordinator();
			jdbcCoordinator.getLogicalConnection().getResourceRegistry().release( st );
			jdbcCoordinator.afterStatementExecution();
		}

	}

	protected List processResultSet(
			ResultSet rs,
			QueryParameters queryParameters,
			SharedSessionContractImplementor session,
			boolean returnProxies,
			ResultTransformer forcedResultTransformer,
			int maxRows,
			List<AfterLoadAction> afterLoadActions) throws SQLException {
		final int entitySpan = getEntityPersisters().length;
		final boolean createSubselects = isSubselectLoadingEnabled();
		final List<EntityKey[]> subselectResultKeys = createSubselects ? new ArrayList<>() : null;
		final List<Object> hydratedObjects = entitySpan == 0 ? null : new ArrayList<>( entitySpan * 10 );

		final List results = getRowsFromResultSet(
				rs,
				queryParameters,
				session,
				returnProxies,
				forcedResultTransformer,
				maxRows,
				hydratedObjects,
				subselectResultKeys
		);

		initializeEntitiesAndCollections(
				hydratedObjects,
				rs,
				session,
				queryParameters.isReadOnly( session ),
				afterLoadActions
		);
		if ( createSubselects ) {
			createSubselects( subselectResultKeys, queryParameters, session );
		}
		return results;
	}

	protected List<Object> getRowsFromResultSet(
			ResultSet rs,
			QueryParameters queryParameters,
			SharedSessionContractImplementor session,
			boolean returnProxies,
			ResultTransformer forcedResultTransformer,
			int maxRows,
			List<Object> hydratedObjects,
			List<EntityKey[]> subselectResultKeys) throws SQLException {
		final int entitySpan = getEntityPersisters().length;
		final boolean createSubselects = isSubselectLoadingEnabled();
		final EntityKey optionalObjectKey = getOptionalObjectKey( queryParameters, session );
		final LockMode[] lockModesArray = getLockModes( queryParameters.getLockOptions() );
		final List<Object> results = new ArrayList<>();

		handleEmptyCollections( queryParameters.getCollectionKeys(), rs, session );
		EntityKey[] keys = new EntityKey[entitySpan]; //we can reuse it for each row
		LOG.trace( "Processing result set" );
		int count;

		final boolean debugEnabled = LOG.isDebugEnabled();
		for ( count = 0; count < maxRows && rs.next(); count++ ) {
			if ( debugEnabled ) {
				LOG.debugf( "Result set row: %s", count );
			}
			Object result = getRowFromResultSet(
					rs,
					session,
					queryParameters,
					lockModesArray,
					optionalObjectKey,
					hydratedObjects,
					keys,
					returnProxies,
					forcedResultTransformer
			);
			results.add( result );
			if ( createSubselects ) {
				subselectResultKeys.add( keys );
				keys = new EntityKey[entitySpan]; //can't reuse in this case
			}
		}

		LOG.tracev( "Done processing result set ({0} rows)", count );

		return results;
	}

	protected boolean isSubselectLoadingEnabled() {
		return false;
	}

	protected boolean hasSubselectLoadableCollections() {
		final Loadable[] loadables = getEntityPersisters();
		for ( Loadable loadable : loadables ) {
			if ( loadable.hasSubselectLoadableCollections() ) {
				return true;
			}
		}
		return false;
	}

	private static Set[] transpose(List keys) {
		Set[] result = new Set[( (EntityKey[]) keys.get( 0 ) ).length];
		for ( int j = 0; j < result.length; j++ ) {
			result[j] = new HashSet( keys.size() );
			for ( Object key : keys ) {
				result[j].add( ( (EntityKey[]) key )[j] );
			}
		}
		return result;
	}

	protected void createSubselects(List keys, QueryParameters queryParameters, SharedSessionContractImplementor session) {
		if ( keys.size() > 1 ) { //if we only returned one entity, query by key is more efficient

			Set[] keySets = transpose( keys );

			Map namedParameterLocMap = buildNamedParameterLocMap( queryParameters );

			final Loadable[] loadables = getEntityPersisters();
			final String[] aliases = getAliases();
			final String subselectQueryString = SubselectFetch.createSubselectFetchQueryFragment( queryParameters );
			final PersistenceContext persistenceContext = session.getPersistenceContextInternal();
			final BatchFetchQueue batchFetchQueue = persistenceContext.getBatchFetchQueue();
			for ( Object key : keys ) {
				final EntityKey[] rowKeys = (EntityKey[]) key;
				for ( int i = 0; i < rowKeys.length; i++ ) {

					if ( rowKeys[i] != null && loadables[i].hasSubselectLoadableCollections() ) {

						SubselectFetch subselectFetch = new SubselectFetch(
								subselectQueryString,
								aliases[i],
								loadables[i],
								queryParameters,
								keySets[i],
								namedParameterLocMap
						);

						batchFetchQueue
								.addSubselect( rowKeys[i], subselectFetch );
					}

				}

			}
		}
	}

	private Map buildNamedParameterLocMap(QueryParameters queryParameters) {
		if ( queryParameters.getNamedParameters() != null ) {
			final Map namedParameterLocMap = new HashMap();
			for ( String name : queryParameters.getNamedParameters().keySet() ) {
				namedParameterLocMap.put(
						name,
						getNamedParameterLocs( name )
				);
			}
			return namedParameterLocMap;
		}
		else {
			return null;
		}
	}

	private void initializeEntitiesAndCollections(
			final List hydratedObjects,
			final Object resultSetId,
			final SharedSessionContractImplementor session,
			final boolean readOnly) throws HibernateException {
		initializeEntitiesAndCollections(
				hydratedObjects,
				resultSetId,
				session,
				readOnly,
				Collections.emptyList()
		);
	}

	private void initializeEntitiesAndCollections(
			final List hydratedObjects,
			final Object resultSetId,
			final SharedSessionContractImplementor session,
			final boolean readOnly,
			List<AfterLoadAction> afterLoadActions) throws HibernateException {

		final CollectionPersister[] collectionPersisters = getCollectionPersisters();
		if ( collectionPersisters != null ) {
			for ( CollectionPersister collectionPersister : collectionPersisters ) {
				if ( collectionPersister.isArray() ) {
					//for arrays, we should end the collection load before resolving
					//the entities, since the actual array instances are not instantiated
					//during loading
					//TODO: or we could do this polymorphically, and have two
					//      different operations implemented differently for arrays
					endCollectionLoad( resultSetId, session, collectionPersister );
				}
			}
		}

		//important: reuse the same event instances for performance!
		final PreLoadEvent pre;
		final PostLoadEvent post;
		if ( session.isEventSource() ) {
			pre = new PreLoadEvent( (EventSource) session );
			post = new PostLoadEvent( (EventSource) session );
		}
		else {
			pre = null;
			post = null;
		}

		if ( hydratedObjects != null ) {
			int hydratedObjectsSize = hydratedObjects.size();
			LOG.tracev( "Total objects hydrated: {0}", hydratedObjectsSize );

			if ( hydratedObjectsSize != 0 ) {
				for ( Object hydratedObject : hydratedObjects ) {
					TwoPhaseLoad.initializeEntity( hydratedObject, readOnly, session, pre );
				}

			}
		}

		if ( collectionPersisters != null ) {
			for ( CollectionPersister collectionPersister : collectionPersisters ) {
				if ( !collectionPersister.isArray() ) {
					//for sets, we should end the collection load after resolving
					//the entities, since we might call hashCode() on the elements
					//TODO: or we could do this polymorphically, and have two
					//      different operations implemented differently for arrays
					endCollectionLoad( resultSetId, session, collectionPersister );
				}
			}
		}

		if ( hydratedObjects != null ) {
			for ( Object hydratedObject : hydratedObjects ) {
				TwoPhaseLoad.afterInitialize( hydratedObject, session );
			}
		}

		// Until this entire method is refactored w/ polymorphism, postLoad was
		// split off from initializeEntity.  It *must* occur after
		// endCollectionLoad to ensure the collection is in the
		// persistence context.
		final PersistenceContext persistenceContext = session.getPersistenceContextInternal();
		if ( hydratedObjects != null && hydratedObjects.size() > 0 ) {
			for ( Object hydratedObject : hydratedObjects ) {
				TwoPhaseLoad.postLoad( hydratedObject, session, post );
				if ( afterLoadActions != null ) {
					for ( AfterLoadAction afterLoadAction : afterLoadActions ) {
						final EntityEntry entityEntry = persistenceContext.getEntry( hydratedObject );
						if ( entityEntry == null ) {
							// big problem
							throw new HibernateException(
									"Could not locate EntityEntry immediately after two-phase load"
							);
						}
						afterLoadAction.afterLoad( session, hydratedObject, (Loadable) entityEntry.getPersister() );
					}
				}
			}
		}
	}

	protected void endCollectionLoad(
			final Object resultSetId,
			final SharedSessionContractImplementor session,
			final CollectionPersister collectionPersister) {
		//this is a query and we are loading multiple instances of the same collection role
		session.getPersistenceContextInternal()
				.getLoadContexts()
				.getCollectionLoadContext( (ResultSet) resultSetId )
				.endLoadingCollections( collectionPersister );
	}

	/**
	 * Determine the actual ResultTransformer that will be used to
	 * transform query results.
	 *
	 * @param resultTransformer the specified result transformer
	 *
	 * @return the actual result transformer
	 */
	protected ResultTransformer resolveResultTransformer(ResultTransformer resultTransformer) {
		return resultTransformer;
	}

	protected List getResultList(List results, ResultTransformer resultTransformer) throws QueryException {
		return results;
	}

	/**
	 * Are rows transformed immediately after being read from the ResultSet?
	 *
	 * @return true, if getResultColumnOrRow() transforms the results; false, otherwise
	 */
	protected boolean areResultSetRowsTransformedImmediately() {
		return false;
	}

	/**
	 * Returns the aliases that corresponding to a result row.
	 *
	 * @return Returns the aliases that corresponding to a result row.
	 */
	protected String[] getResultRowAliases() {
		return null;
	}

	/**
	 * Get the actual object that is returned in the user-visible result list.
	 * This empty implementation merely returns its first argument. This is
	 * overridden by some subclasses.
	 */
	protected Object getResultColumnOrRow(
			Object[] row,
			ResultTransformer transformer,
			ResultSet rs,
			SharedSessionContractImplementor session) throws SQLException, HibernateException {
		return row;
	}

	protected boolean[] includeInResultRow() {
		return null;
	}

	protected Object[] getResultRow(
			Object[] row,
			ResultSet rs,
			SharedSessionContractImplementor session) throws SQLException, HibernateException {
		return row;
	}

	/**
	 * For missing objects associated by one-to-one with another object in the
	 * result set, register the fact that the the object is missing with the
	 * session.
	 */
	protected void registerNonExists(
			final EntityKey[] keys,
			final Loadable[] persisters,
			final SharedSessionContractImplementor session) {

		final int[] owners = getOwners();
		if ( owners != null ) {

			EntityType[] ownerAssociationTypes = getOwnerAssociationTypes();
			final PersistenceContext persistenceContext = session.getPersistenceContextInternal();
			for ( int i = 0; i < keys.length; i++ ) {

				int owner = owners[i];
				if ( owner > -1 ) {
					EntityKey ownerKey = keys[owner];
					if ( keys[i] == null && ownerKey != null ) {


						/*final boolean isPrimaryKey;
						final boolean isSpecialOneToOne;
						if ( ownerAssociationTypes == null || ownerAssociationTypes[i] == null ) {
							isPrimaryKey = true;
							isSpecialOneToOne = false;
						}
						else {
							isPrimaryKey = ownerAssociationTypes[i].getRHSUniqueKeyPropertyName()==null;
							isSpecialOneToOne = ownerAssociationTypes[i].getLHSPropertyName()!=null;
						}*/

						//TODO: can we *always* use the "null property" approach for everything?
						/*if ( isPrimaryKey && !isSpecialOneToOne ) {
							persistenceContext.addNonExistantEntityKey(
									new EntityKey( ownerKey.getIdentifier(), persisters[i], session.getEntityMode() )
							);
						}
						else if ( isSpecialOneToOne ) {*/
						boolean isOneToOneAssociation = ownerAssociationTypes != null &&
								ownerAssociationTypes[i] != null &&
								ownerAssociationTypes[i].isOneToOne();
						if ( isOneToOneAssociation ) {
							persistenceContext.addNullProperty(
									ownerKey,
									ownerAssociationTypes[i].getPropertyName()
							);
						}
						/*}
						else {
							persistenceContext.addNonExistantEntityUniqueKey( new EntityUniqueKey(
									persisters[i].getEntityName(),
									ownerAssociationTypes[i].getRHSUniqueKeyPropertyName(),
									ownerKey.getIdentifier(),
									persisters[owner].getIdentifierType(),
									session.getEntityMode()
							) );
						}*/
					}
				}
			}
		}
	}

	/**
	 * Read one collection element from the current row of the JDBC result set
	 */
	private void readCollectionElement(
			final Object optionalOwner,
			final Serializable optionalKey,
			final CollectionPersister persister,
			final CollectionAliases descriptor,
			final ResultSet rs,
			final SharedSessionContractImplementor session)
			throws HibernateException, SQLException {

		final PersistenceContext persistenceContext = session.getPersistenceContextInternal();

		final Serializable collectionRowKey = (Serializable) persister.readKey(
				rs,
				descriptor.getSuffixedKeyAliases(),
				session
		);

		if ( collectionRowKey != null ) {
			// we found a collection element in the result set

			if ( LOG.isDebugEnabled() ) {
				LOG.debugf(
						"Found row of collection: %s",
						MessageHelper.collectionInfoString( persister, collectionRowKey, getFactory() )
				);
			}

			Object owner = optionalOwner;
			if ( owner == null ) {
				owner = persistenceContext.getCollectionOwner( collectionRowKey, persister );
				if ( owner == null ) {
					//TODO: This is assertion is disabled because there is a bug that means the
					//	  original owner of a transient, uninitialized collection is not known
					//	  if the collection is re-referenced by a different object associated
					//	  with the current Session
					//throw new AssertionFailure("bug loading unowned collection");
				}
			}

			PersistentCollection rowCollection = persistenceContext.getLoadContexts()
					.getCollectionLoadContext( rs )
					.getLoadingCollection( persister, collectionRowKey );

			if ( rowCollection != null ) {
				rowCollection.readFrom( rs, persister, descriptor, owner );
			}

		}
		else if ( optionalKey != null ) {
			// we did not find a collection element in the result set, so we
			// ensure that a collection is created with the owner's identifier,
			// since what we have is an empty collection

			if ( LOG.isDebugEnabled() ) {
				LOG.debugf(
						"Result set contains (possibly empty) collection: %s",
						MessageHelper.collectionInfoString( persister, optionalKey, getFactory() )
				);
			}

			persistenceContext.getLoadContexts()
					.getCollectionLoadContext( rs )
					.getLoadingCollection( persister, optionalKey ); // handle empty collection

		}

		// else no collection element, but also no owner

	}

	/**
	 * If this is a collection initializer, we need to tell the session that a collection
	 * is being initialized, to account for the possibility of the collection having
	 * no elements (hence no rows in the result set).
	 */
	protected void handleEmptyCollections(
			final Serializable[] keys,
			final Object resultSetId,
			final SharedSessionContractImplementor session) {

		if ( keys != null ) {
			// this is a collection initializer, so we must create a collection
			// for each of the passed-in keys, to account for the possibility
			// that the collection is empty and has no rows in the result set
			CollectionPersister[] collectionPersisters = getCollectionPersisters();
			final PersistenceContext persistenceContext = session.getPersistenceContextInternal();
			final boolean debugEnabled = LOG.isDebugEnabled();
			final CollectionLoadContext collectionLoadContext = persistenceContext
					.getLoadContexts()
					.getCollectionLoadContext( (ResultSet) resultSetId );
			for ( CollectionPersister collectionPersister : collectionPersisters ) {
				for ( Serializable key : keys ) {
					//handle empty collections
					if ( debugEnabled ) {
						LOG.debugf(
								"Result set contains (possibly empty) collection: %s",
								MessageHelper.collectionInfoString( collectionPersister, key, getFactory() )
						);
					}

					collectionLoadContext
							.getLoadingCollection( collectionPersister, key );
				}
			}
		}

		// else this is not a collection initializer (and empty collections will
		// be detected by looking for the owner's identifier in the result set)
	}

	/**
	 * Read a row of <tt>Key</tt>s from the <tt>ResultSet</tt> into the given array.
	 * Warning: this method is side-effecty.
	 * <p/>
	 * If an <tt>id</tt> is given, don't bother going to the <tt>ResultSet</tt>.
	 */
	private EntityKey getKeyFromResultSet(
			final int i,
			final Loadable persister,
			final Serializable id,
			final ResultSet rs,
			final SharedSessionContractImplementor session) throws HibernateException, SQLException {

		Serializable resultId;

		// if we know there is exactly 1 row, we can skip.
		// it would be great if we could _always_ skip this;
		// it is a problem for <key-many-to-one>

		if ( isSingleRowLoader() && id != null ) {
			resultId = id;
		}
		else {
			final Type idType = persister.getIdentifierType();
			resultId = (Serializable) idType.nullSafeGet(
					rs,
					getEntityAliases()[i].getSuffixedKeyAliases(),
					session,
					null //problematic for <key-many-to-one>!
			);

			final boolean idIsResultId = id != null &&
					resultId != null &&
					idType.isEqual( id, resultId, factory );

			if ( idIsResultId ) {
				resultId = id; //use the id passed in
			}
		}

		return resultId == null ? null : session.generateEntityKey( resultId, persister );
	}

	/**
	 * Check the version of the object in the <tt>ResultSet</tt> against
	 * the object version in the session cache, throwing an exception
	 * if the version numbers are different
	 */
	private void checkVersion(
			final int i,
			final Loadable persister,
			final Serializable id,
			final Object entity,
			final ResultSet rs,
			final SharedSessionContractImplementor session) throws HibernateException, SQLException {

		Object version = session.getPersistenceContextInternal().getEntry( entity ).getVersion();

		if ( version != null ) {
			// null version means the object is in the process of being loaded somewhere else in the ResultSet
			final VersionType versionType = persister.getVersionType();
			final Object currentVersion = versionType.nullSafeGet(
					rs,
					getEntityAliases()[i].getSuffixedVersionAliases(),
					session,
					null
			);
			if ( !versionType.isEqual( version, currentVersion ) ) {
				final StatisticsImplementor statistics = session.getFactory().getStatistics();
				if ( statistics.isStatisticsEnabled() ) {
					statistics.optimisticFailure( persister.getEntityName() );
				}
				throw new StaleObjectStateException( persister.getEntityName(), id );
			}
		}

	}

	/**
	 * Resolve any IDs for currently loaded objects, duplications within the
	 * <tt>ResultSet</tt>, etc. Instantiate empty objects to be initialized from the
	 * <tt>ResultSet</tt>. Return an array of objects (a row of results) and an
	 * array of booleans (by side-effect) that determine whether the corresponding
	 * object should be initialized.
	 */
	private Object[] getRow(
			final ResultSet rs,
			final Loadable[] persisters,
			final EntityKey[] keys,
			final Object optionalObject,
			final EntityKey optionalObjectKey,
			final LockMode[] lockModes,
			final List hydratedObjects,
			final SharedSessionContractImplementor session) throws HibernateException, SQLException {
		final int cols = persisters.length;
		final EntityAliases[] entityAliases = getEntityAliases();

		if ( LOG.isDebugEnabled() ) {
			LOG.debugf( "Result row: %s", StringHelper.toString( keys ) );
		}

		final Object[] rowResults = new Object[cols];

		for ( int i = 0; i < cols; i++ ) {

			Object object = null;
			EntityKey key = keys[i];

			if ( keys[i] == null ) {
				//do nothing
			}
			else {
				//If the object is already loaded, return the loaded one
				object = session.getEntityUsingInterceptor( key );
				if ( object != null ) {
					instanceAlreadyLoaded(
							rs,
							i,
							persisters[i],
							key,
							object,
							lockModes[i],
							hydratedObjects,
							session
					);
				}
				else {
					object = instanceNotYetLoaded(
							rs,
							i,
							persisters[i],
							entityAliases[i].getRowIdAlias(),
							key,
							lockModes[i],
							optionalObjectKey,
							optionalObject,
							hydratedObjects,
							session
					);
				}
			}

			rowResults[i] = object;

		}

		return rowResults;
	}

	/**
	 * The entity instance is already in the session cache
	 */
	protected void instanceAlreadyLoaded(
			final ResultSet rs,
			final int i,
			final Loadable persister,
			final EntityKey key,
			final Object object,
			final LockMode requestedLockMode,
			List hydratedObjects,
			final SharedSessionContractImplementor session)
			throws HibernateException, SQLException {
		if ( !persister.isInstance( object ) ) {
			throw new WrongClassException(
					"loaded object was of wrong class " + object.getClass(),
					key.getIdentifier(),
					persister.getEntityName()
			);
		}

		if ( persister.getBytecodeEnhancementMetadata().isEnhancedForLazyLoading() ) {
			// we have found an existing "managed copy" in the session
			// we need to check if this copy is an enhanced-proxy and, if so,
			// perform the hydration just as if it were "not yet loaded"
			final PersistentAttributeInterceptor interceptor = ( (PersistentAttributeInterceptable) object ).$$_hibernate_getInterceptor();
			if ( interceptor instanceof EnhancementAsProxyLazinessInterceptor ) {
				EntityEntry entry = session.getPersistenceContextInternal().getEntry( object );
				// Avoid loading the same entity proxy twice for the same result set: it could lead to errors,
				// because some code writes to its input (ID in hydrated state replaced by the loaded entity, in particular).
				if ( entry != null && entry.getStatus() != Status.LOADING ) {
					hydrateEntityState(
							rs,
							i,
							persister,
							getEntityAliases()[i].getRowIdAlias(),
							key,
							hydratedObjects,
							session,
							getInstanceClass(
									rs,
									i,
									persister,
									key.getIdentifier(),
									session
							),
							object,
							requestedLockMode
					);
				}

				// EARLY EXIT!!!
				//		- to skip the version check
				return;
			}
		}

		if ( LockMode.NONE != requestedLockMode && upgradeLocks() ) {
			final EntityEntry entry = session.getPersistenceContextInternal().getEntry( object );
			if ( entry.getLockMode().lessThan( requestedLockMode ) ) {
				//we only check the version when _upgrading_ lock modes
				if ( persister.isVersioned() ) {
					checkVersion( i, persister, key.getIdentifier(), object, rs, session );
				}
				//we need to upgrade the lock mode to the mode requested
				entry.setLockMode( requestedLockMode );
			}
		}
	}


	/**
	 * The entity instance is not in the session cache
	 */
	protected Object instanceNotYetLoaded(
			final ResultSet rs,
			final int i,
			final Loadable persister,
			final String rowIdAlias,
			final EntityKey key,
			final LockMode lockMode,
			final EntityKey optionalObjectKey,
			final Object optionalObject,
			final List hydratedObjects,
			final SharedSessionContractImplementor session)
			throws HibernateException, SQLException {
		final String instanceClass = getInstanceClass(
				rs,
				i,
				persister,
				key.getIdentifier(),
				session
		);

		// see if the entity defines reference caching, and if so use the cached reference (if one).
		if ( session.getCacheMode().isGetEnabled() && persister.canUseReferenceCacheEntries() ) {
			final EntityDataAccess cache = persister.getCacheAccessStrategy();
			if ( cache != null ) {
				final Object ck = cache.generateCacheKey(
						key.getIdentifier(),
						persister,
						session.getFactory(),
						session.getTenantIdentifier()
				);
				final Object cachedEntry = CacheHelper.fromSharedCache( session, ck, cache );
				if ( cachedEntry != null ) {
					CacheEntry entry = (CacheEntry) persister.getCacheEntryStructure().destructure(
							cachedEntry,
							factory
					);
					return ( (ReferenceCacheEntryImpl) entry ).getReference();
				}
			}
		}

		final Object object;
		if ( optionalObjectKey != null && key.equals( optionalObjectKey ) ) {
			//its the given optional object
			object = optionalObject;
		}
		else {
			// instantiate a new instance
			if ( persister.hasSubclasses() ) {
				object = session.instantiate( instanceClass , key.getIdentifier() );
			}
			else {
				//When there are no subclasses, use the persister instance directly
				//so to short-circuit the persister lookup:
				object = session.instantiate( persister, key.getIdentifier() );
			}
		}

		//need to hydrate it.

		// grab its state from the ResultSet and keep it in the Session
		// (but don't yet initialize the object itself)
		// note that we acquire LockMode.READ even if it was not requested
		LockMode acquiredLockMode = lockMode == LockMode.NONE ? LockMode.READ : lockMode;
		hydrateEntityState(
				rs,
				i,
				persister,
				rowIdAlias,
				key,
				hydratedObjects,
				session,
				instanceClass,
				object,
				acquiredLockMode
		);

		return object;
	}

	private void hydrateEntityState(
			ResultSet rs,
			int i,
			Loadable persister,
			String rowIdAlias,
			EntityKey key,
			List hydratedObjects,
			SharedSessionContractImplementor session,
			String instanceClass,
			Object object,
			LockMode acquiredLockMode) throws SQLException {
		loadFromResultSet(
				rs,
				i,
				object,
				instanceClass,
				key,
				rowIdAlias,
				acquiredLockMode,
				persister,
				session
		);

		//materialize associations (and initialize the object) later
		hydratedObjects.add( object );
	}

	private boolean isAllPropertyEagerFetchEnabled(int i) {
		boolean[] array = getEntityEagerPropertyFetches();
		return array != null && array[i];
	}

	private boolean[] getPerPropertyEagerFetchEnabled(int i) {
		boolean[][] array = getEntityEagerPerPropertyFetches();
		return array != null ? array[i] : null;
	}

	/**
	 * Hydrate the state an object from the SQL <tt>ResultSet</tt>, into
	 * an array or "hydrated" values (do not resolve associations yet),
	 * and pass the hydrates state to the session.
	 */
	private void loadFromResultSet(
			final ResultSet rs,
			final int i,
			final Object object,
			final String instanceEntityName,
			final EntityKey key,
			final String rowIdAlias,
			final LockMode lockMode,
			final Loadable rootPersister,
			final SharedSessionContractImplementor session) throws SQLException, HibernateException {

		final Serializable id = key.getIdentifier();

		// Get the persister for the _subclass_
		final Loadable persister = (Loadable) getFactory().getEntityPersister( instanceEntityName );

		if ( LOG.isTraceEnabled() ) {
			LOG.tracef(
					"Initializing object from ResultSet: %s",
					MessageHelper.infoString(
							persister,
							id,
							getFactory()
					)
			);
		}

		boolean fetchAllPropertiesRequested = isAllPropertyEagerFetchEnabled( i );

		// add temp entry so that the next step is circular-reference
		// safe - only needed because some types don't take proper
		// advantage of two-phase-load (esp. components)
		TwoPhaseLoad.addUninitializedEntity(
				key,
				object,
				persister,
				lockMode,
				session
		);

		//This is not very nice (and quite slow):
		final String[][] cols = persister == rootPersister ?
				getEntityAliases()[i].getSuffixedPropertyAliases() :
				getEntityAliases()[i].getSuffixedPropertyAliases( persister );

		final Object[] values = persister.hydrate(
				rs,
				id,
				object,
				rootPersister,
				cols,
				fetchAllPropertiesRequested,
				getPerPropertyEagerFetchEnabled( i ),
				session
		);

		final Object rowId = persister.hasRowId() ? rs.getObject( rowIdAlias ) : null;

		final AssociationType[] ownerAssociationTypes = getOwnerAssociationTypes();
		if ( ownerAssociationTypes != null && ownerAssociationTypes[i] != null ) {
			String ukName = ownerAssociationTypes[i].getRHSUniqueKeyPropertyName();
			if ( ukName != null ) {
				final int index = ( (UniqueKeyLoadable) persister ).getPropertyIndex( ukName );
				final Type type = persister.getPropertyTypes()[index];

				// polymorphism not really handled completely correctly,
				// perhaps...well, actually its ok, assuming that the
				// entity name used in the lookup is the same as the
				// the one used here, which it will be

				EntityUniqueKey euk = new EntityUniqueKey(
						rootPersister.getEntityName(), //polymorphism comment above
						ukName,
						type.semiResolve( values[index], session, object ),
						type,
						persister.getEntityMode(),
						session.getFactory()
				);
				session.getPersistenceContextInternal().addEntity( euk, object );
			}
		}

		TwoPhaseLoad.postHydrate(
				persister,
				id,
				values,
				rowId,
				object,
				lockMode,
				session
		);

	}

	/**
	 * Determine the concrete class of an instance in the <tt>ResultSet</tt>
	 */
	private String getInstanceClass(
			final ResultSet rs,
			final int i,
			final Loadable persister,
			final Serializable id,
			final SharedSessionContractImplementor session) throws HibernateException, SQLException {

		if ( persister.hasSubclasses() ) {

			// Code to handle subclasses of topClass
			final Object discriminatorValue = persister.getDiscriminatorType().nullSafeGet(
					rs,
					getEntityAliases()[i].getSuffixedDiscriminatorAlias(),
					session,
					null
			);

			final String result = persister.getSubclassForDiscriminatorValue( discriminatorValue );

			if ( result == null ) {
				//whoops we got an instance of another class hierarchy branch
				throw new WrongClassException(
						"Discriminator: " + discriminatorValue,
						id,
						persister.getEntityName()
				);
			}

			return result;

		}
		else {
			return persister.getEntityName();
		}
	}

	/**
	 * Advance the cursor to the first required row of the <tt>ResultSet</tt>
	 */
	private void advance(final ResultSet rs, final RowSelection selection) throws SQLException {

		final int firstRow = LimitHelper.getFirstRow( selection );
		if ( firstRow != 0 ) {
			if ( getFactory().getSessionFactoryOptions().isScrollableResultSetsEnabled() ) {
				// we can go straight to the first required row
				rs.absolute( firstRow );
			}
			else {
				// we need to step through the rows one row at a time (slow)
				for ( int m = 0; m < firstRow; m++ ) {
					rs.next();
				}
			}
		}
	}

	/**
	 * Build LIMIT clause handler applicable for given selection criteria. Returns {@link NoopLimitHandler} delegate
	 * if dialect does not support LIMIT expression or processed query does not use pagination.
	 *
	 * @param selection Selection criteria.
	 *
	 * @return LIMIT clause delegate.
	 */
	protected LimitHandler getLimitHandler(RowSelection selection) {
		final LimitHandler limitHandler = getFactory().getDialect().getLimitHandler();
		return LimitHelper.useLimit( limitHandler, selection ) ? limitHandler : NoopLimitHandler.INSTANCE;
	}

	private ScrollMode getScrollMode(
			boolean scroll,
			LimitHandler limitHandler,
			QueryParameters queryParameters) {
		final boolean canScroll = getFactory().getSessionFactoryOptions().isScrollableResultSetsEnabled();
		if ( canScroll ) {
			if ( scroll ) {
				return queryParameters.getScrollMode();
			}
			final RowSelection selection = queryParameters.getRowSelection();
			final boolean useLimit = LimitHelper.useLimit( limitHandler, selection );
			final boolean hasFirstRow = LimitHelper.hasFirstRow( selection );
			final boolean useLimitOffset = hasFirstRow && useLimit && limitHandler.supportsLimitOffset();
			if ( hasFirstRow && !useLimitOffset ) {
				return ScrollMode.SCROLL_INSENSITIVE;
			}
		}
		return null;
	}

	/**
	 * Process query string by applying filters, LIMIT clause, locks and comments if necessary.
	 * Finally execute SQL statement and advance to the first row.
	 */
	protected SqlStatementWrapper executeQueryStatement(
			final QueryParameters queryParameters,
			final boolean scroll,
			List<AfterLoadAction> afterLoadActions,
			final SharedSessionContractImplementor session) throws SQLException {
		return executeQueryStatement( getSQLString(), queryParameters, scroll, afterLoadActions, session );
	}

	protected SqlStatementWrapper executeQueryStatement(
			String sqlStatement,
			QueryParameters queryParameters,
			boolean scroll,
			List<AfterLoadAction> afterLoadActions,
			SharedSessionContractImplementor session) throws SQLException {

		// Processing query filters.
		queryParameters.processFilters( sqlStatement, session );

		// Applying LIMIT clause.
		final LimitHandler limitHandler = getLimitHandler(
				queryParameters.getRowSelection()
		);
		String sql = limitHandler.processSql( queryParameters.getFilteredSQL(), queryParameters );

		// Adding locks and comments.
		sql = preprocessSQL( sql, queryParameters, getFactory(), afterLoadActions );

		final PreparedStatement st = prepareQueryStatement( sql, queryParameters, limitHandler, scroll, session );

		final ResultSet rs;

		if( queryParameters.isCallable() && isTypeOf( st, CallableStatement.class ) ) {
			final CallableStatement cs = st.unwrap( CallableStatement.class );

			rs = getResultSet(
					cs,
					queryParameters.getRowSelection(),
					limitHandler,
					queryParameters.hasAutoDiscoverScalarTypes(),
					session
			);
		}
		else {
			rs = getResultSet(
				st,
				queryParameters.getRowSelection(),
				limitHandler,
				queryParameters.hasAutoDiscoverScalarTypes(),
				session
			);
		}

		return new SqlStatementWrapper(
			st,
			rs
		);

	}

	private boolean isTypeOf(final Statement statement, final Class<? extends Statement> type) {
		if ( isJdbc4 ) {
			try {
				// This is "more correct" than #isInstance, but not always supported.
				return statement.isWrapperFor( type );
			}
			catch (SQLException e) {
				// No operation
			}
			catch (Throwable e) {
				// No operation. Note that this catches more than just SQLException to
				// cover edge cases where a driver might throw an UnsupportedOperationException, AbstractMethodError,
				// etc.  If so, skip permanently.
				isJdbc4 = false;
			}
		}
		return type.isInstance( statement );
	}

	/**
	 * Obtain a <tt>PreparedStatement</tt> with all parameters pre-bound.
	 * Bind JDBC-style <tt>?</tt> parameters, named parameters, and
	 * limit parameters.
	 */
	protected final PreparedStatement prepareQueryStatement(
			String sql,
			final QueryParameters queryParameters,
			final LimitHandler limitHandler,
			final boolean scroll,
			final SharedSessionContractImplementor session) throws SQLException, HibernateException {

		final PreparedStatement preparedStatement = session.getJdbcCoordinator().getStatementPreparer().prepareQueryStatement(
				sql,
				queryParameters.isCallable(),
				getScrollMode( scroll, limitHandler, queryParameters )
		);
		return bindPreparedStatement( preparedStatement, queryParameters, limitHandler, session );
	}

	protected final PreparedStatement bindPreparedStatement(
			final PreparedStatement st,
			final QueryParameters queryParameters,
			final LimitHandler limitHandler,
			final SharedSessionContractImplementor session) throws SQLException, HibernateException {

		final Dialect dialect = getFactory().getDialect();
		final RowSelection selection = queryParameters.getRowSelection();
		final boolean callable = queryParameters.isCallable();

		try {

			int col = 1;
			//TODO: can we limit stored procedures ?!
			col += limitHandler.bindLimitParametersAtStartOfQuery( selection, st, col );

			if ( callable ) {
				col = dialect.registerResultSetOutParameter( (CallableStatement) st, col );
			}

			col += bindParameterValues( st, queryParameters, col, session );

			col += limitHandler.bindLimitParametersAtEndOfQuery( selection, st, col );

			limitHandler.setMaxRows( selection, st );

			if ( selection != null ) {
				if ( selection.getTimeout() != null ) {
					st.setQueryTimeout( selection.getTimeout() );
				}
				if ( selection.getFetchSize() != null ) {
					st.setFetchSize( selection.getFetchSize() );
				}
			}

			// handle lock timeout...
			LockOptions lockOptions = queryParameters.getLockOptions();
			if ( lockOptions != null ) {
				if ( lockOptions.getTimeOut() != LockOptions.WAIT_FOREVER ) {
					if ( !dialect.supportsLockTimeouts() ) {
						if ( LOG.isDebugEnabled() ) {
							LOG.debugf(
									"Lock timeout [%s] requested but dialect reported to not support lock timeouts",
									lockOptions.getTimeOut()
							);
						}
					}
					else if ( dialect.isLockTimeoutParameterized() ) {
						st.setInt( col++, lockOptions.getTimeOut() );
					}
				}
			}

			if ( LOG.isTraceEnabled() ) {
				LOG.tracev( "Bound [{0}] parameters total", col );
			}
		}
		catch (SQLException | HibernateException e) {
			session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().release( st );
			session.getJdbcCoordinator().afterStatementExecution();
			throw e;
		}

		return st;
	}

	/**
	 * Bind all parameter values into the prepared statement in preparation
	 * for execution.
	 *
	 * @param statement The JDBC prepared statement
	 * @param queryParameters The encapsulation of the parameter values to be bound.
	 * @param startIndex The position from which to start binding parameter values.
	 * @param session The originating session.
	 *
	 * @return The number of JDBC bind positions actually bound during this method execution.
	 *
	 * @throws SQLException Indicates problems performing the binding.
	 */
	protected int bindParameterValues(
			PreparedStatement statement,
			QueryParameters queryParameters,
			int startIndex,
			SharedSessionContractImplementor session) throws SQLException {
		int span = 0;
		span += bindPositionalParameters( statement, queryParameters, startIndex, session );
		span += bindNamedParameters( statement, queryParameters.getNamedParameters(), startIndex + span, session );
		return span;
	}

	/**
	 * Bind positional parameter values to the JDBC prepared statement.
	 * <p/>
	 * Positional parameters are those specified by JDBC-style ? parameters
	 * in the source query.  It is (currently) expected that these come
	 * before any named parameters in the source query.
	 *
	 * @param statement The JDBC prepared statement
	 * @param queryParameters The encapsulation of the parameter values to be bound.
	 * @param startIndex The position from which to start binding parameter values.
	 * @param session The originating session.
	 *
	 * @return The number of JDBC bind positions actually bound during this method execution.
	 *
	 * @throws SQLException Indicates problems performing the binding.
	 * @throws org.hibernate.HibernateException Indicates problems delegating binding to the types.
	 */
	protected int bindPositionalParameters(
			final PreparedStatement statement,
			final QueryParameters queryParameters,
			final int startIndex,
			final SharedSessionContractImplementor session) throws SQLException, HibernateException {
		final Object[] values = queryParameters.getFilteredPositionalParameterValues();
		final Type[] types = queryParameters.getFilteredPositionalParameterTypes();
		int span = 0;
		for ( int i = 0; i < values.length; i++ ) {
			types[i].nullSafeSet( statement, values[i], startIndex + span, session );
			span += types[i].getColumnSpan( getFactory() );
		}
		return span;
	}

	/**
	 * Bind named parameters to the JDBC prepared statement.
	 * <p/>
	 * This is a generic implementation, the problem being that in the
	 * general case we do not know enough information about the named
	 * parameters to perform this in a complete manner here.  Thus this
	 * is generally overridden on subclasses allowing named parameters to
	 * apply the specific behavior.  The most usual limitation here is that
	 * we need to assume the type span is always one...
	 *
	 * @param statement The JDBC prepared statement
	 * @param namedParams A map of parameter names to values
	 * @param startIndex The position from which to start binding parameter values.
	 * @param session The originating session.
	 *
	 * @return The number of JDBC bind positions actually bound during this method execution.
	 *
	 * @throws SQLException Indicates problems performing the binding.
	 * @throws org.hibernate.HibernateException Indicates problems delegating binding to the types.
	 */
	protected int bindNamedParameters(
			final PreparedStatement statement,
			final Map<String, TypedValue> namedParams,
			final int startIndex,
			final SharedSessionContractImplementor session) throws SQLException, HibernateException {
		int result = 0;
		if ( CollectionHelper.isEmpty( namedParams ) ) {
			return result;
		}

		final boolean debugEnabled = LOG.isDebugEnabled();
		final SessionFactoryImplementor factory = getFactory();

		for ( Map.Entry<String, TypedValue> entry : namedParams.entrySet() ) {
			final String name = entry.getKey();
			final TypedValue typedValue = entry.getValue();
			final Type type = typedValue.getType();
			final int columnSpan = type.getColumnSpan( factory );
			final int[] locs = getNamedParameterLocs( name );
			for ( int loc : locs ) {
				if ( debugEnabled ) {
					LOG.debugf(
							"bindNamedParameters() %s -> %s [%s]",
							typedValue.getValue(),
							name,
							loc + startIndex
					);
				}
				int start = loc * columnSpan + startIndex;
				type.nullSafeSet( statement, typedValue.getValue(), start, session );
			}
			result += locs.length;
		}
		return result;
	}

	public int[] getNamedParameterLocs(String name) {
		throw new AssertionFailure( "no named parameters" );
	}

	/**
	 * Execute given <tt>PreparedStatement</tt>, advance to the first result and return SQL <tt>ResultSet</tt>.
	 */
	protected final ResultSet getResultSet(
			final PreparedStatement st,
			final RowSelection selection,
			final LimitHandler limitHandler,
			final boolean autodiscovertypes,
			final SharedSessionContractImplementor session) throws SQLException, HibernateException {
		try {
			ResultSet rs = session.getJdbcCoordinator().getResultSetReturn().extract( st );

			return preprocessResultSet( rs, selection, limitHandler, autodiscovertypes, session );
		}
		catch (SQLException | HibernateException e) {
			session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().release( st );
			session.getJdbcCoordinator().afterStatementExecution();
			throw e;
		}
	}

	/**
	 * Execute given <tt>CallableStatement</tt>, advance to the first result and return SQL <tt>ResultSet</tt>.
	 */
	protected final ResultSet getResultSet(
			final CallableStatement st,
			final RowSelection selection,
			final LimitHandler limitHandler,
			final boolean autodiscovertypes,
			final SharedSessionContractImplementor session) throws SQLException, HibernateException {
		try {
			ResultSet rs = session.getJdbcCoordinator().getResultSetReturn().extract( st );

			return preprocessResultSet( rs, selection, limitHandler, autodiscovertypes, session );
		}
		catch (SQLException | HibernateException e) {
			session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().release( st );
			session.getJdbcCoordinator().afterStatementExecution();
			throw e;
		}
	}

	protected ResultSet preprocessResultSet(
			ResultSet rs,
			final RowSelection selection,
			final LimitHandler limitHandler,
			final boolean autodiscovertypes,
			final SharedSessionContractImplementor session
	) throws SQLException, HibernateException {
		rs = wrapResultSetIfEnabled( rs, session );

		if ( !limitHandler.supportsLimitOffset() || !LimitHelper.useLimit( limitHandler, selection ) ) {
			advance( rs, selection );
		}

		if ( autodiscovertypes ) {
			autoDiscoverTypes( rs );
		}
		return rs;
	}

	protected void autoDiscoverTypes(ResultSet rs) {
		throw new AssertionFailure( "Auto discover types not supported in this loader" );

	}

	private ResultSet wrapResultSetIfEnabled(final ResultSet rs, final SharedSessionContractImplementor session) {
		if ( session.getFactory().getSessionFactoryOptions().isWrapResultSetsEnabled() ) {
			try {
				LOG.debugf( "Wrapping result set [%s]", rs );
				return session.getFactory()
						.getServiceRegistry()
						.getService( JdbcServices.class )
						.getResultSetWrapper().wrap( rs, retrieveColumnNameToIndexCache( rs ) );
			}
			catch (SQLException e) {
				LOG.unableToWrapResultSet( e );
				return rs;
			}
		}
		else {
			return rs;
		}
	}

	private ColumnNameCache retrieveColumnNameToIndexCache(final ResultSet rs) throws SQLException {
		final ColumnNameCache cache = columnNameCache;
		if ( cache == null ) {
			//there is no need for a synchronized second check, as in worst case
			//we'll have allocated an unnecessary ColumnNameCache
			LOG.trace( "Building columnName -> columnIndex cache" );
			columnNameCache = new ColumnNameCache( rs.getMetaData().getColumnCount() );
			return columnNameCache;
		}
		else {
			return cache;
		}
	}

	/**
	 * Called by subclasses that load entities
	 */
	protected final List loadEntity(
			final SharedSessionContractImplementor session,
			final Object id,
			final Type identifierType,
			final Object optionalObject,
			final String optionalEntityName,
			final Serializable optionalIdentifier,
			final EntityPersister persister,
			final LockOptions lockOptions,
			final Boolean readOnly) throws HibernateException {
		if ( LOG.isDebugEnabled() ) {
			LOG.debugf( "Loading entity: %s", MessageHelper.infoString( persister, id, identifierType, getFactory() ) );
		}

		List result;
		try {
			QueryParameters qp = new QueryParameters();
			qp.setPositionalParameterTypes( new Type[] {identifierType} );
			qp.setPositionalParameterValues( new Object[] {id} );
			qp.setOptionalObject( optionalObject );
			qp.setOptionalEntityName( optionalEntityName );
			qp.setOptionalId( optionalIdentifier );
			qp.setLockOptions( lockOptions );
			if ( readOnly != null ) {
				qp.setReadOnly( readOnly );
			}
			result = doQueryAndInitializeNonLazyCollections( session, qp, false );
		}
		catch (SQLException sqle) {
			final Loadable[] persisters = getEntityPersisters();
			throw factory.getJdbcServices().getSqlExceptionHelper().convert(
					sqle,
					"could not load an entity: " +
							MessageHelper.infoString(
									persisters[persisters.length - 1],
									id,
									identifierType,
									getFactory()
							),
					getSQLString()
			);
		}

		LOG.debug( "Done entity load" );

		return result;

	}

	/**
	 * Called by subclasses that load entities
	 *
	 * @param persister only needed for logging
	 */
	protected final List loadEntity(
			final SharedSessionContractImplementor session,
			final Object key,
			final Object index,
			final Type keyType,
			final Type indexType,
			final EntityPersister persister) throws HibernateException {
		LOG.debug( "Loading collection element by index" );

		List result;
		try {
			result = doQueryAndInitializeNonLazyCollections(
					session,
					new QueryParameters(
							new Type[] {keyType, indexType},
							new Object[] {key, index}
					),
					false
			);
		}
		catch (SQLException sqle) {
			throw factory.getJdbcServices().getSqlExceptionHelper().convert(
					sqle,
					"could not load collection element by index",
					getSQLString()
			);
		}

		LOG.debug( "Done entity load" );

		return result;

	}

	/**
	 * Called by wrappers that batch load entities
	 */
	public final List loadEntityBatch(
			final SharedSessionContractImplementor session,
			final Serializable[] ids,
			final Type idType,
			final Object optionalObject,
			final String optionalEntityName,
			final Serializable optionalId,
			final EntityPersister persister,
			LockOptions lockOptions) throws HibernateException {
		return loadEntityBatch( session, ids, idType, optionalObject, optionalEntityName, optionalId, persister, lockOptions, null );
	}

	/**
	 * Called by wrappers that batch load entities
	 */
	public final List loadEntityBatch(
			final SharedSessionContractImplementor session,
			final Serializable[] ids,
			final Type idType,
			final Object optionalObject,
			final String optionalEntityName,
			final Serializable optionalId,
			final EntityPersister persister,
			final LockOptions lockOptions,
			final Boolean readOnly) throws HibernateException {
		if ( LOG.isDebugEnabled() ) {
			LOG.debugf( "Batch loading entity: %s", MessageHelper.infoString( persister, ids, getFactory() ) );
		}

		Type[] types = new Type[ids.length];
		Arrays.fill( types, idType );
		List result;
		try {
			QueryParameters qp = new QueryParameters();
			qp.setPositionalParameterTypes( types );
			qp.setPositionalParameterValues( ids );
			qp.setOptionalObject( optionalObject );
			qp.setOptionalEntityName( optionalEntityName );
			qp.setOptionalId( optionalId );
			qp.setLockOptions( lockOptions );
			if ( readOnly != null ) {
				qp.setReadOnly( readOnly );
			}
			result = doQueryAndInitializeNonLazyCollections( session, qp, false );
		}
		catch (SQLException sqle) {
			throw factory.getJdbcServices().getSqlExceptionHelper().convert(
					sqle,
					"could not load an entity batch: " +
							MessageHelper.infoString( getEntityPersisters()[0], ids, getFactory() ),
					getSQLString()
			);
		}

		LOG.debug( "Done entity batch load" );

		return result;

	}

	/**
	 * Called by subclasses that initialize collections
	 */
	public final void loadCollection(
			final SharedSessionContractImplementor session,
			final Serializable id,
			final Type type) throws HibernateException {
		if ( LOG.isDebugEnabled() ) {
			LOG.debugf(
					"Loading collection: %s",
					MessageHelper.collectionInfoString( getCollectionPersisters()[0], id, getFactory() )
			);
		}

		Serializable[] ids = new Serializable[] {id};
		try {
			doQueryAndInitializeNonLazyCollections(
					session,
					new QueryParameters( new Type[] {type}, ids, ids ),
					true
			);
		}
		catch (SQLException sqle) {
			throw factory.getJdbcServices().getSqlExceptionHelper().convert(
					sqle,
					"could not initialize a collection: " +
							MessageHelper.collectionInfoString( getCollectionPersisters()[0], id, getFactory() ),
					getSQLString()
			);
		}

		LOG.debug( "Done loading collection" );
	}

	/**
	 * Called by wrappers that batch initialize collections
	 */
	public final void loadCollectionBatch(
			final SharedSessionContractImplementor session,
			final Serializable[] ids,
			final Type type) throws HibernateException {
		if ( LOG.isDebugEnabled() ) {
			LOG.debugf(
					"Batch loading collection: %s",
					MessageHelper.collectionInfoString( getCollectionPersisters()[0], ids, getFactory() )
			);
		}

		Type[] idTypes = new Type[ids.length];
		Arrays.fill( idTypes, type );
		try {
			doQueryAndInitializeNonLazyCollections(
					session,
					new QueryParameters( idTypes, ids, ids ),
					true
			);
		}
		catch (SQLException sqle) {
			throw factory.getJdbcServices().getSqlExceptionHelper().convert(
					sqle,
					"could not initialize a collection batch: " +
							MessageHelper.collectionInfoString( getCollectionPersisters()[0], ids, getFactory() ),
					getSQLString()
			);
		}

		LOG.debug( "Done batch load" );
	}

	/**
	 * Called by subclasses that batch initialize collections
	 */
	protected final void loadCollectionSubselect(
			final SharedSessionContractImplementor session,
			final Serializable[] ids,
			final Object[] parameterValues,
			final Type[] parameterTypes,
			final Map<String, TypedValue> namedParameters,
			final Type type) throws HibernateException {
		try {
			doQueryAndInitializeNonLazyCollections(
					session,
					new QueryParameters( parameterTypes, parameterValues, namedParameters, ids ),
					true
			);
		}
		catch (SQLException sqle) {
			throw factory.getJdbcServices().getSqlExceptionHelper().convert(
					sqle,
					"could not load collection by subselect: " +
							MessageHelper.collectionInfoString( getCollectionPersisters()[0], ids, getFactory() ),
					getSQLString()
			);
		}
	}

	/**
	 * Return the query results, using the query cache, called
	 * by subclasses that implement cacheable queries
	 */
	protected List list(
			final SharedSessionContractImplementor session,
			final QueryParameters queryParameters,
			final Set<Serializable> querySpaces,
			final Type[] resultTypes) throws HibernateException {
		final boolean cacheable = factory.getSessionFactoryOptions().isQueryCacheEnabled() &&
				queryParameters.isCacheable();

		if ( cacheable ) {
			return listUsingQueryCache( session, queryParameters, querySpaces, resultTypes );
		}
		else {
			return listIgnoreQueryCache( session, queryParameters );
		}
	}

	private List listIgnoreQueryCache(SharedSessionContractImplementor session, QueryParameters queryParameters) {
		return getResultList( doList( session, queryParameters ), queryParameters.getResultTransformer() );
	}

	private List listUsingQueryCache(
			final SharedSessionContractImplementor session,
			final QueryParameters queryParameters,
			final Set<Serializable> querySpaces,
			final Type[] resultTypes) {

		QueryResultsCache queryCache = factory.getCache().getQueryResultsCache( queryParameters.getCacheRegion() );

		QueryKey key = generateQueryKey( session, queryParameters );

		if ( querySpaces == null || querySpaces.size() == 0 ) {
			LOG.tracev( "Unexpected querySpaces is {0}", ( querySpaces == null ? querySpaces : "empty" ) );
		}
		else {
			LOG.tracev( "querySpaces is {0}", querySpaces );
		}

		List result = getResultFromQueryCache(
				session,
				queryParameters,
				querySpaces,
				resultTypes,
				queryCache,
				key
		);

		if ( result == null ) {
			result = doList( session, queryParameters, key.getResultTransformer() );

			putResultInQueryCache(
					session,
					queryParameters,
					resultTypes,
					queryCache,
					key,
					result
			);
		}

		ResultTransformer resolvedTransformer = resolveResultTransformer( queryParameters.getResultTransformer() );
		if ( resolvedTransformer != null ) {
			result = (
					areResultSetRowsTransformedImmediately() ?
							key.getResultTransformer().retransformResults(
									result,
									getResultRowAliases(),
									queryParameters.getResultTransformer(),
									includeInResultRow()
							) :
							key.getResultTransformer().untransformToTuples(
									result
							)
			);
		}

		return getResultList( result, queryParameters.getResultTransformer() );
	}

	protected QueryKey generateQueryKey(
			SharedSessionContractImplementor session,
			QueryParameters queryParameters) {
		return QueryKey.generateQueryKey(
				getSQLString(),
				queryParameters,
				FilterKey.createFilterKeys( session.getLoadQueryInfluencers().getEnabledFilters() ),
				session,
				createCacheableResultTransformer( queryParameters )
		);
	}

	protected CacheableResultTransformer createCacheableResultTransformer(QueryParameters queryParameters) {
		return CacheableResultTransformer.create(
				queryParameters.getResultTransformer(),
				getResultRowAliases(),
				includeInResultRow()
		);
	}

	protected List getResultFromQueryCache(
			final SharedSessionContractImplementor session,
			final QueryParameters queryParameters,
			final Set<Serializable> querySpaces,
			final Type[] resultTypes,
			final QueryResultsCache queryCache,
			final QueryKey key) {
		List result = null;

		if ( session.getCacheMode().isGetEnabled() ) {
			boolean isImmutableNaturalKeyLookup =
					queryParameters.isNaturalKeyLookup() &&
							resultTypes.length == 1 &&
							resultTypes[0].isEntityType() &&
							getEntityPersister( EntityType.class.cast( resultTypes[0] ) )
									.getEntityMetamodel()
									.hasImmutableNaturalId();

			final PersistenceContext persistenceContext = session.getPersistenceContextInternal();
			boolean defaultReadOnlyOrig = persistenceContext.isDefaultReadOnly();
			if ( queryParameters.isReadOnlyInitialized() ) {
				// The read-only/modifiable mode for the query was explicitly set.
				// Temporarily set the default read-only/modifiable setting to the query's setting.
				persistenceContext.setDefaultReadOnly( queryParameters.isReadOnly() );
			}
			else {
				// The read-only/modifiable setting for the query was not initialized.
				// Use the default read-only/modifiable from the persistence context instead.
				queryParameters.setReadOnly( persistenceContext.isDefaultReadOnly() );
			}
			try {
				result = queryCache.get(
						key,
						querySpaces,
						key.getResultTransformer().getCachedResultTypes( resultTypes ),
						session
				);
			}
			finally {
				persistenceContext.setDefaultReadOnly( defaultReadOnlyOrig );
			}

			final StatisticsImplementor statistics = factory.getStatistics();
			if ( statistics.isStatisticsEnabled() ) {
				if ( result == null ) {
					statistics.queryCacheMiss( getQueryIdentifier(), queryCache.getRegion().getName() );
				}
				else {
					statistics.queryCacheHit( getQueryIdentifier(), queryCache.getRegion().getName() );
				}
			}
		}

		return result;
	}

	protected EntityPersister getEntityPersister(EntityType entityType) {
		return factory.getMetamodel().entityPersister( entityType.getAssociatedEntityName() );
	}

	protected void putResultInQueryCache(
			final SharedSessionContractImplementor session,
			final QueryParameters queryParameters,
			final Type[] resultTypes,
			final QueryResultsCache queryCache,
			final QueryKey key,
			final List result) {
		if ( session.getCacheMode().isPutEnabled() ) {
			boolean put = queryCache.put(
					key,
					result,
					key.getResultTransformer().getCachedResultTypes( resultTypes ),
					session
			);
			final StatisticsImplementor statistics = factory.getStatistics();
			if ( put && statistics.isStatisticsEnabled() ) {
				statistics.queryCachePut( getQueryIdentifier(), queryCache.getRegion().getName() );
			}
		}
	}

	/**
	 * Actually execute a query, ignoring the query cache
	 */

	protected List doList(final SharedSessionContractImplementor session, final QueryParameters queryParameters)
			throws HibernateException {
		return doList( session, queryParameters, null );
	}

	private List doList(
			final SharedSessionContractImplementor session,
			final QueryParameters queryParameters,
			final ResultTransformer forcedResultTransformer)
			throws HibernateException {

		final StatisticsImplementor statistics = getFactory().getStatistics();
		final boolean stats = statistics.isStatisticsEnabled();
		long startTime = 0;
		if ( stats ) {
			startTime = System.nanoTime();
		}

		List result;
		try {
			result = doQueryAndInitializeNonLazyCollections( session, queryParameters, true, forcedResultTransformer );
		}
		catch (SQLException sqle) {
			throw factory.getJdbcServices().getSqlExceptionHelper().convert(
					sqle,
					"could not execute query",
					getSQLString()
			);
		}

		if ( stats ) {
			final long endTime = System.nanoTime();
			final long milliseconds = TimeUnit.MILLISECONDS.convert( endTime - startTime, TimeUnit.NANOSECONDS );
			statistics.queryExecuted(
					getQueryIdentifier(),
					result.size(),
					milliseconds
			);
		}

		return result;
	}

	/**
	 * Check whether the current loader can support returning ScrollableResults.
	 *
	 * @throws HibernateException
	 */
	protected void checkScrollability() throws HibernateException {
		// Allows various loaders (ok mainly the QueryLoader :) to check
		// whether scrolling of their result set should be allowed.
		//
		// By default it is allowed.
	}

	/**
	 * Does the result set to be scrolled contain collection fetches?
	 *
	 * @return True if it does, and thus needs the special fetching scroll
	 * functionality; false otherwise.
	 */
	protected boolean needsFetchingScroll() {
		return false;
	}

	/**
	 * Return the query results, as an instance of <tt>ScrollableResults</tt>
	 *
	 * @param queryParameters The parameters with which the query should be executed.
	 * @param returnTypes The expected return types of the query
	 * @param holderInstantiator If the return values are expected to be wrapped
	 * in a holder, this is the thing that knows how to wrap them.
	 * @param session The session from which the scroll request originated.
	 *
	 * @return The ScrollableResults instance.
	 *
	 * @throws HibernateException Indicates an error executing the query, or constructing
	 * the ScrollableResults.
	 */
	protected ScrollableResultsImplementor scroll(
			final QueryParameters queryParameters,
			final Type[] returnTypes,
			final HolderInstantiator holderInstantiator,
			final SharedSessionContractImplementor session) throws HibernateException {
		checkScrollability();

		final StatisticsImplementor statistics = getFactory().getStatistics();
		final boolean stats = getQueryIdentifier() != null &&
				statistics.isStatisticsEnabled();
		long startTime = 0;
		if ( stats ) {
			startTime = System.nanoTime();
		}

		try {
			// Don't use Collections#emptyList() here -- follow on locking potentially adds AfterLoadActions,
			// so the list cannot be immutable.
			final SqlStatementWrapper wrapper = executeQueryStatement(
					queryParameters,
					true,
					new ArrayList<AfterLoadAction>(),
					session
			);
			final ResultSet rs = wrapper.getResultSet();
			final PreparedStatement st = (PreparedStatement) wrapper.getStatement();

			if ( stats ) {
				final long endTime = System.nanoTime();
				final long milliseconds = TimeUnit.MILLISECONDS.convert( endTime - startTime, TimeUnit.NANOSECONDS );
				statistics.queryExecuted(
						getQueryIdentifier(),
						0,
						milliseconds
				);
			}

			if ( needsFetchingScroll() ) {
				return new FetchingScrollableResultsImpl(
						rs,
						st,
						session,
						this,
						queryParameters,
						returnTypes,
						holderInstantiator
				);
			}
			else {
				return new ScrollableResultsImpl(
						rs,
						st,
						session,
						this,
						queryParameters,
						returnTypes,
						holderInstantiator
				);
			}

		}
		catch (SQLException sqle) {
			throw factory.getJdbcServices().getSqlExceptionHelper().convert(
					sqle,
					"could not execute query using scroll",
					getSQLString()
			);
		}

	}

	/**
	 * Calculate and cache select-clause suffixes. Must be
	 * called by subclasses after instantiation.
	 */
	protected void postInstantiate() {
	}

	/**
	 * Get the result set descriptor
	 */
	protected abstract EntityAliases[] getEntityAliases();

	protected abstract CollectionAliases[] getCollectionAliases();

	/**
	 * Identifies the query for statistics reporting, if null,
	 * no statistics will be reported
	 */
	protected String getQueryIdentifier() {
		return null;
	}

	public final SessionFactoryImplementor getFactory() {
		return factory;
	}

	@Override
	public String toString() {
		return getClass().getName() + '(' + getSQLString() + ')';
	}

	/**
	 * Wrapper class for {@link Statement} and associated {@link ResultSet}.
	 */
	protected static class SqlStatementWrapper {
		private final Statement statement;
		private final ResultSet resultSet;

		private SqlStatementWrapper(Statement statement, ResultSet resultSet) {
			this.resultSet = resultSet;
			this.statement = statement;
		}

		public ResultSet getResultSet() {
			return resultSet;
		}

		public Statement getStatement() {
			return statement;
		}
	}

	/**
	 * Remove distinct keyword from SQL statement if the query should not pass it through.
	 * @param sql SQL string
	 * @param parameters SQL parameters
	 * @return SQL string
	 */
	protected String processDistinctKeyword(
			String sql,
			QueryParameters parameters) {
		if ( !parameters.isPassDistinctThrough() ) {
			if ( sql.startsWith( SELECT_DISTINCT ) ) {
				return SELECT + sql.substring( SELECT_DISTINCT.length() );
			}
		}
		return sql;
	}
}
