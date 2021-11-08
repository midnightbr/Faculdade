/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.lock;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.JDBCException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.StaleObjectStateException;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.Lockable;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.sql.SimpleSelect;
import org.hibernate.stat.spi.StatisticsImplementor;

/**
 * A locking strategy where the locks are obtained through select statements.
 * <p/>
 * For non-read locks, this is achieved through the Dialect's specific
 * SELECT ... FOR UPDATE syntax.
 *
 * @see org.hibernate.dialect.Dialect#getForUpdateString(org.hibernate.LockMode)
 * @see org.hibernate.dialect.Dialect#appendLockHint(org.hibernate.LockMode, String)
 *
 * @author Steve Ebersole
 * @since 3.2
 */
public class SelectLockingStrategy extends AbstractSelectLockingStrategy {
	/**
	 * Construct a locking strategy based on SQL SELECT statements.
	 *
	 * @param lockable The metadata for the entity to be locked.
	 * @param lockMode Indicates the type of lock to be acquired.
	 */
	public SelectLockingStrategy(Lockable lockable, LockMode lockMode) {
		super( lockable, lockMode );
	}

	@Override
	public void lock(
			Serializable id,
			Object version,
			Object object,
			int timeout,
			SharedSessionContractImplementor session) throws StaleObjectStateException, JDBCException {
		final String sql = determineSql( timeout );
		final SessionFactoryImplementor factory = session.getFactory();
		final Lockable lockable = getLockable();
		try {
			final JdbcCoordinator jdbcCoordinator = session.getJdbcCoordinator();
			final PreparedStatement st = jdbcCoordinator.getStatementPreparer().prepareStatement( sql );
			try {
				lockable.getIdentifierType().nullSafeSet( st, id, 1, session );
				if ( lockable.isVersioned() ) {
					lockable.getVersionType().nullSafeSet(
							st,
							version,
							lockable.getIdentifierType().getColumnSpan( factory ) + 1,
							session
					);
				}

				final ResultSet rs = jdbcCoordinator.getResultSetReturn().extract( st );
				try {
					if ( !rs.next() ) {
						final StatisticsImplementor statistics = factory.getStatistics();
						if ( statistics.isStatisticsEnabled() ) {
							statistics.optimisticFailure( lockable.getEntityName() );
						}
						throw new StaleObjectStateException( lockable.getEntityName(), id );
					}
				}
				finally {
					jdbcCoordinator.getLogicalConnection().getResourceRegistry().release( rs, st );
				}
			}
			finally {
				jdbcCoordinator.getLogicalConnection().getResourceRegistry().release( st );
				jdbcCoordinator.afterStatementExecution();
			}

		}
		catch ( SQLException sqle ) {
			throw session.getJdbcServices().getSqlExceptionHelper().convert(
					sqle,
					"could not lock: " + MessageHelper.infoString( lockable, id, session.getFactory() ),
					sql
				);
		}
	}

	protected String generateLockString(int timeout) {
		final SessionFactoryImplementor factory = getLockable().getFactory();
		final LockOptions lockOptions = new LockOptions( getLockMode() );
		lockOptions.setTimeOut( timeout );
		final SimpleSelect select = new SimpleSelect( factory.getDialect() )
				.setLockOptions( lockOptions )
				.setTableName( getLockable().getRootTableName() )
				.addColumn( getLockable().getRootTableIdentifierColumnNames()[0] )
				.addCondition( getLockable().getRootTableIdentifierColumnNames(), "=?" );
		if ( getLockable().isVersioned() ) {
			select.addCondition( getLockable().getVersionColumnName(), "=?" );
		}
		if ( factory.getSessionFactoryOptions().isCommentsEnabled() ) {
			select.setComment( getLockMode() + " lock " + getLockable().getEntityName() );
		}
		return select.toStatementString();
	}
}
