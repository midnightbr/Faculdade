/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.entity.plan;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import org.hibernate.LockOptions;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.Loader;
import org.hibernate.loader.entity.UniqueEntityLoader;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.type.Type;

import org.jboss.logging.Logger;

/**
 * The base contract for UniqueEntityLoader implementations capable of performing batch-fetch loading of entities
 * using multiple primary key values in the SQL <tt>WHERE</tt> clause.
 * <p/>
 * Typically these are
 *
 * @author Gavin King
 * @author Steve Ebersole
 *
 * @see org.hibernate.loader.entity.BatchingEntityLoaderBuilder
 * @see org.hibernate.loader.entity.UniqueEntityLoader
 */
public abstract class BatchingEntityLoader implements UniqueEntityLoader {
	private static final Logger log = Logger.getLogger( BatchingEntityLoader.class );

	private final EntityPersister persister;

	public BatchingEntityLoader(EntityPersister persister) {
		this.persister = persister;
	}

	public EntityPersister persister() {
		return persister;
	}

	@Override
	public Object load(Serializable id, Object optionalObject, SharedSessionContractImplementor session) {
		return load( id, optionalObject, session, LockOptions.NONE, null );
	}

	protected QueryParameters buildQueryParameters(
			Serializable id,
			Serializable[] ids,
			Object optionalObject,
			LockOptions lockOptions) {
		Type[] types = new Type[ids.length];
		Arrays.fill( types, persister().getIdentifierType() );

		QueryParameters qp = new QueryParameters();
		qp.setPositionalParameterTypes( types );
		qp.setPositionalParameterValues( ids );
		qp.setOptionalObject( optionalObject );
		qp.setOptionalEntityName( persister().getEntityName() );
		qp.setOptionalId( id );
		qp.setLockOptions( lockOptions );
		return qp;
	}

	protected Object getObjectFromList(List results, Serializable id, SharedSessionContractImplementor session) {
		for ( Object obj : results ) {
			final boolean equal = persister.getIdentifierType().isEqual(
					id,
					session.getContextEntityIdentifier( obj ),
					session.getFactory()
			);
			if ( equal ) {
				return obj;
			}
		}
		return null;
	}

	protected Object doBatchLoad(
			Serializable id,
			Loader loaderToUse,
			SharedSessionContractImplementor session,
			Serializable[] ids,
			Object optionalObject,
			LockOptions lockOptions) {
		if ( log.isDebugEnabled() ) {
			log.debugf( "Batch loading entity: %s", MessageHelper.infoString( persister, ids, session.getFactory() ) );
		}

		QueryParameters qp = buildQueryParameters( id, ids, optionalObject, lockOptions );

		try {
			final List results = loaderToUse.doQueryAndInitializeNonLazyCollections( session, qp, false );
			log.debug( "Done entity batch load" );
			return getObjectFromList(results, id, session);
		}
		catch ( SQLException sqle ) {
			throw session.getJdbcServices().getSqlExceptionHelper().convert(
					sqle,
					"could not load an entity batch: " + MessageHelper.infoString( persister(), ids, session.getFactory() ),
					loaderToUse.getSQLString()
			);
		}
	}

}
