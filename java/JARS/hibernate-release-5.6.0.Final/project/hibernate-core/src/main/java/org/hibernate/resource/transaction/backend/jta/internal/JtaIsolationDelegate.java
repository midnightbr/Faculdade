/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.resource.transaction.backend.jta.internal;

import javax.transaction.NotSupportedException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Callable;

import org.hibernate.HibernateException;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.engine.transaction.spi.IsolationDelegate;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.jdbc.WorkExecutor;
import org.hibernate.jdbc.WorkExecutorVisitable;

/**
 * An isolation delegate for JTA environments.
 *
 * @author Andrea Boriero
 */
public class JtaIsolationDelegate implements IsolationDelegate {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( JtaIsolationDelegate.class );

	private final JdbcConnectionAccess connectionAccess;
	private final SqlExceptionHelper sqlExceptionHelper;
	private final TransactionManager transactionManager;

	public JtaIsolationDelegate(
			JdbcConnectionAccess connectionAccess,
			SqlExceptionHelper sqlExceptionHelper,
			TransactionManager transactionManager) {
		this.connectionAccess = connectionAccess;
		this.sqlExceptionHelper = sqlExceptionHelper;
		this.transactionManager = transactionManager;
	}

	protected JdbcConnectionAccess jdbcConnectionAccess() {
		return this.connectionAccess;
	}

	protected SqlExceptionHelper sqlExceptionHelper() {
		return this.sqlExceptionHelper;
	}

	@Override
	public <T> T delegateWork(final WorkExecutorVisitable<T> work, final boolean transacted) throws HibernateException {
		return doInSuspendedTransaction(new HibernateCallable<T>() {
			@Override
			public T call() throws HibernateException {
				HibernateCallable<T> workCallable = new HibernateCallable<T>() {
					@Override
					public T call() throws HibernateException {
						return doTheWork(work);
					}
				};
				if ( transacted ) {
					return doInNewTransaction( workCallable, transactionManager );
				}
				else {
					return workCallable.call();
				}
			}
		});
	}

	@Override
	public <T> T delegateCallable(final Callable<T> callable, final boolean transacted) throws HibernateException {
		return doInSuspendedTransaction(new HibernateCallable<T>() {
			@Override
			public T call() throws HibernateException {
				HibernateCallable<T> workCallable = new HibernateCallable<T>() {
					@Override
					public T call() throws HibernateException {
						try {
							return callable.call();
						}
						catch (HibernateException e) {
							throw e;
						}
						catch (Exception e) {
							throw new HibernateException(e);
						}
					}
				};
				if ( transacted ) {
					return doInNewTransaction( workCallable, transactionManager );
				}
				else {
					return workCallable.call();
				}
			}
		});
	}

	private <T> T doInSuspendedTransaction(HibernateCallable<T> callable) {
		try {
			// First we suspend any current JTA transaction
			Transaction surroundingTransaction = transactionManager.suspend();
			LOG.debugf( "Surrounding JTA transaction suspended [%s]", surroundingTransaction );

			boolean hadProblems = false;
			try {
				return callable.call();
			}
			catch (HibernateException e) {
				hadProblems = true;
				throw e;
			}
			finally {
				try {
					transactionManager.resume( surroundingTransaction );
					LOG.debugf( "Surrounding JTA transaction resumed [%s]", surroundingTransaction );
				}
				catch (Throwable t) {
					// if the actually work had an error use that, otherwise error based on t
					if ( !hadProblems ) {
						//noinspection ThrowFromFinallyBlock
						throw new HibernateException( "Unable to resume previously suspended transaction", t );
					}
				}
			}
		}
		catch (SystemException e) {
			throw new HibernateException( "Unable to suspend current JTA transaction", e );
		}
	}

	private <T> T doInNewTransaction(HibernateCallable<T> callable, TransactionManager transactionManager) {
		try {
			// start the new isolated transaction
			transactionManager.begin();

			try {
				T result = callable.call();
				// if everything went ok, commit the isolated transaction
				transactionManager.commit();
				return result;
			}
			catch (Exception e) {
				try {
					transactionManager.rollback();
				}
				catch (Exception ignore) {
					LOG.unableToRollbackIsolatedTransaction( e, ignore );
				}
				throw new HibernateException( "Could not apply work", e );
			}
		}
		catch (SystemException e) {
			throw new HibernateException( "Unable to start isolated transaction", e );
		}
		catch (NotSupportedException e) {
			throw new HibernateException( "Unable to start isolated transaction", e );
		}
	}

	private <T> T doTheWork(WorkExecutorVisitable<T> work) {
		try {
			// obtain our isolated connection
			Connection connection = jdbcConnectionAccess().obtainConnection();
			try {
				// do the actual work
				return work.accept( new WorkExecutor<T>(), connection );
			}
			catch (HibernateException e) {
				throw e;
			}
			catch (Exception e) {
				throw new HibernateException( "Unable to perform isolated work", e );
			}
			finally {
				try {
					// no matter what, release the connection (handle)
					jdbcConnectionAccess().releaseConnection( connection );
				}
				catch (Throwable ignore) {
					LOG.unableToReleaseIsolatedConnection( ignore );
				}
			}
		}
		catch (SQLException e) {
			throw sqlExceptionHelper().convert( e, "unable to obtain isolated JDBC connection" );
		}
	}

	// Callable that does not throw Exception; in Java <8 there's no Supplier
	private interface HibernateCallable<T> {
		T call() throws HibernateException;
	}
}
