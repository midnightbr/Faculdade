/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.resource.transaction.backend.jdbc.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.persistence.RollbackException;
import javax.transaction.Status;

import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.transaction.spi.IsolationDelegate;
import org.hibernate.engine.transaction.spi.TransactionObserver;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.jpa.spi.JpaCompliance;
import org.hibernate.resource.jdbc.spi.JdbcSessionOwner;
import org.hibernate.resource.transaction.backend.jdbc.spi.JdbcResourceTransaction;
import org.hibernate.resource.transaction.backend.jdbc.spi.JdbcResourceTransactionAccess;
import org.hibernate.resource.transaction.internal.SynchronizationRegistryStandardImpl;
import org.hibernate.resource.transaction.spi.SynchronizationRegistry;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorOwner;
import org.hibernate.resource.transaction.spi.TransactionStatus;

import static org.hibernate.internal.CoreLogging.messageLogger;

/**
 * An implementation of TransactionCoordinator based on managing a transaction through the JDBC Connection
 * via {@link org.hibernate.resource.transaction.backend.jdbc.spi.JdbcResourceTransaction}
 *
 * @author Steve Ebersole
 *
 * @see org.hibernate.resource.transaction.backend.jdbc.spi.JdbcResourceTransaction
 */
public class JdbcResourceLocalTransactionCoordinatorImpl implements TransactionCoordinator {
	private static final CoreMessageLogger log = messageLogger( JdbcResourceLocalTransactionCoordinatorImpl.class );

	private final TransactionCoordinatorBuilder transactionCoordinatorBuilder;
	private final JdbcResourceTransactionAccess jdbcResourceTransactionAccess;
	private final TransactionCoordinatorOwner transactionCoordinatorOwner;
	private final SynchronizationRegistryStandardImpl synchronizationRegistry = new SynchronizationRegistryStandardImpl();

	private final JpaCompliance jpaCompliance;

	private TransactionDriverControlImpl physicalTransactionDelegate;

	private int timeOut = -1;

	private transient List<TransactionObserver> observers = null;

	/**
	 * Construct a ResourceLocalTransactionCoordinatorImpl instance.  package-protected to ensure access goes through
	 * builder.
	 *
	 * @param owner The transactionCoordinatorOwner
	 */
	JdbcResourceLocalTransactionCoordinatorImpl(
			TransactionCoordinatorBuilder transactionCoordinatorBuilder,
			TransactionCoordinatorOwner owner,
			JdbcResourceTransactionAccess jdbcResourceTransactionAccess) {
		this.transactionCoordinatorBuilder = transactionCoordinatorBuilder;
		this.jdbcResourceTransactionAccess = jdbcResourceTransactionAccess;
		this.transactionCoordinatorOwner = owner;

		this.jpaCompliance = owner.getJdbcSessionOwner()
				.getJdbcSessionContext()
				.getSessionFactory()
				.getSessionFactoryOptions()
				.getJpaCompliance();
	}

	/**
	 * Needed because while iterating the observers list and executing the before/update callbacks,
	 * some observers might get removed from the list.
	 *
	 * @return TransactionObserver
	 */
	private Iterable<TransactionObserver> observers() {
		if ( observers == null || observers.isEmpty() ) {
			return Collections.emptyList();
		}
		else {
			return new ArrayList<>( observers );
		}
	}

	@Override
	public TransactionDriver getTransactionDriverControl() {
		// Again, this PhysicalTransactionDelegate will act as the bridge from the local transaction back into the
		// coordinator.  We lazily build it as we invalidate each delegate after each transaction (a delegate is
		// valid for just one transaction)
		if ( physicalTransactionDelegate == null ) {
			physicalTransactionDelegate = new TransactionDriverControlImpl( jdbcResourceTransactionAccess.getResourceLocalTransaction() );
		}
		return physicalTransactionDelegate;
	}

	@Override
	public void explicitJoin() {
		// nothing to do here, but log a warning
		log.callingJoinTransactionOnNonJtaEntityManager();
	}

	@Override
	public boolean isJoined() {
		return physicalTransactionDelegate != null && getTransactionDriverControl().isActive( true );
	}

	@Override
	public void pulse() {
		// nothing to do here
	}

	@Override
	public SynchronizationRegistry getLocalSynchronizations() {
		return synchronizationRegistry;
	}

	@Override
	public JpaCompliance getJpaCompliance() {
		return jpaCompliance;
	}

	@Override
	public boolean isActive() {
		return transactionCoordinatorOwner.isActive();
	}

	@Override
	public IsolationDelegate createIsolationDelegate() {
		final JdbcSessionOwner jdbcSessionOwner = transactionCoordinatorOwner.getJdbcSessionOwner();

		return new JdbcIsolationDelegate(
				jdbcSessionOwner.getJdbcConnectionAccess(),
				jdbcSessionOwner.getJdbcSessionContext().getServiceRegistry().getService( JdbcServices.class ).getSqlExceptionHelper()
		);
	}

	@Override
	public TransactionCoordinatorBuilder getTransactionCoordinatorBuilder() {
		return this.transactionCoordinatorBuilder;
	}

	@Override
	public void setTimeOut(int seconds) {
		this.timeOut = seconds;
	}

	@Override
	public int getTimeOut() {
		return this.timeOut;
	}

	// PhysicalTransactionDelegate ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private void afterBeginCallback() {
		if(this.timeOut > 0) {
			transactionCoordinatorOwner.setTransactionTimeOut( this.timeOut );
		}


		// report entering into a "transactional context"
		transactionCoordinatorOwner.startTransactionBoundary();

		// trigger the Transaction-API-only after-begin callback
		transactionCoordinatorOwner.afterTransactionBegin();

		// notify all registered observers
		for ( TransactionObserver observer : observers() ) {
			observer.afterBegin();
		}
		log.trace( "ResourceLocalTransactionCoordinatorImpl#afterBeginCallback" );
	}

	private void beforeCompletionCallback() {
		log.trace( "ResourceLocalTransactionCoordinatorImpl#beforeCompletionCallback" );
		try {
			transactionCoordinatorOwner.beforeTransactionCompletion();
			synchronizationRegistry.notifySynchronizationsBeforeTransactionCompletion();
			for ( TransactionObserver observer : observers() ) {
				observer.beforeCompletion();
			}
		}
		catch (RuntimeException e) {
			if ( physicalTransactionDelegate != null ) {
				// should never happen that the physicalTransactionDelegate is null, but to be safe
				physicalTransactionDelegate.markRollbackOnly();
			}
			throw e;
		}
	}

	private void afterCompletionCallback(boolean successful) {
		log.tracef( "ResourceLocalTransactionCoordinatorImpl#afterCompletionCallback(%s)", successful );
		final int statusToSend = successful ? Status.STATUS_COMMITTED : Status.STATUS_UNKNOWN;
		synchronizationRegistry.notifySynchronizationsAfterTransactionCompletion( statusToSend );

		transactionCoordinatorOwner.afterTransactionCompletion( successful, false );
		for ( TransactionObserver observer : observers() ) {
			observer.afterCompletion( successful, false );
		}
	}

	@Override
	public void addObserver(TransactionObserver observer) {
		if ( observers == null ) {
			observers = new ArrayList<>( 6 );
		}
		observers.add( observer );
	}

	@Override
	public void removeObserver(TransactionObserver observer) {
		if ( observers != null ) {
			observers.remove( observer );
		}
	}

	/**
	 * The delegate bridging between the local (application facing) transaction and the "physical" notion of a
	 * transaction via the JDBC Connection.
	 */
	public class TransactionDriverControlImpl implements TransactionDriver {
		private final JdbcResourceTransaction jdbcResourceTransaction;
		private boolean invalid;
		private boolean rollbackOnly = false;

		public TransactionDriverControlImpl(JdbcResourceTransaction jdbcResourceTransaction) {
			super();
			this.jdbcResourceTransaction = jdbcResourceTransaction;
		}

		protected void invalidate() {
			invalid = true;
		}

		@Override
		public void begin() {
			errorIfInvalid();

			jdbcResourceTransaction.begin();
			JdbcResourceLocalTransactionCoordinatorImpl.this.afterBeginCallback();
		}

		protected void errorIfInvalid() {
			if ( invalid ) {
				throw new IllegalStateException( "Physical-transaction delegate is no longer valid" );
			}
		}

		@Override
		public void commit() {
			try {
				if ( rollbackOnly ) {
					log.debugf( "On commit, transaction was marked for roll-back only, rolling back" );

					try {
						rollback();

						if ( jpaCompliance.isJpaTransactionComplianceEnabled() ) {
							log.debugf( "Throwing RollbackException on roll-back of transaction marked rollback-only on commit" );
							throw new RollbackException( "Transaction was marked for rollback-only" );
						}

						return;
					}
					catch (RollbackException e) {
						throw e;
					}
					catch (RuntimeException e) {
						log.debug( "Encountered failure rolling back failed commit", e );
						throw e;
					}
				}

				JdbcResourceLocalTransactionCoordinatorImpl.this.beforeCompletionCallback();
				jdbcResourceTransaction.commit();
				JdbcResourceLocalTransactionCoordinatorImpl.this.afterCompletionCallback( true );
			}
			catch (RollbackException e) {
				throw e;
			}
			catch (RuntimeException e) {
				try {
					rollback();
				}
				catch (RuntimeException e2) {
					log.debug( "Encountered failure rolling back failed commit", e2 );
				}
				throw e;
			}
		}

		@Override
		public void rollback() {
			try {
				TransactionStatus status = jdbcResourceTransaction.getStatus();
				if ( ( rollbackOnly && status != TransactionStatus.NOT_ACTIVE ) || status == TransactionStatus.ACTIVE ) {
					jdbcResourceTransaction.rollback();
					JdbcResourceLocalTransactionCoordinatorImpl.this.afterCompletionCallback( false );
				}
			}
			finally {
				rollbackOnly = false;
			}

			// no-op otherwise.
		}

		@Override
		public TransactionStatus getStatus() {
			return rollbackOnly ? TransactionStatus.MARKED_ROLLBACK : jdbcResourceTransaction.getStatus();
		}

		@Override
		public void markRollbackOnly() {
			if ( getStatus() != TransactionStatus.ROLLED_BACK ) {
				if ( log.isDebugEnabled() ) {
					log.debug(
							"JDBC transaction marked for rollback-only (exception provided for stack trace)",
							new Exception( "exception just for purpose of providing stack trace" )
					);
				}

				rollbackOnly = true;
			}
		}
	}
}
