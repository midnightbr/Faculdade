/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.resource.transaction.backend.jta.internal.synchronization;

import java.io.Serializable;
import javax.transaction.SystemException;

import org.hibernate.engine.spi.SessionImplementor;

/**
 * A pluggable strategy for defining how the {@link javax.transaction.Synchronization} registered by Hibernate handles
 * exceptions.
 *
 * @author Steve Ebersole
 */
public interface ExceptionMapper extends Serializable {
	/**
	 * Map a JTA {@link javax.transaction.SystemException} to the appropriate runtime-based exception.
	 *
	 * @param message The message to use for the returned exception
	 * @param systemException The causal exception
	 *
	 * @return The appropriate exception to throw
	 */
	RuntimeException mapStatusCheckFailure(String message, SystemException systemException, SessionImplementor sessionImplementor);

	/**
	 * Map an exception encountered during a managed flush to the appropriate runtime-based exception.
	 *
	 * @param message The message to use for the returned exception
	 * @param failure The causal exception
	 *
	 * @return The appropriate exception to throw
	 */
	RuntimeException mapManagedFlushFailure(String message, RuntimeException failure, SessionImplementor session);
}
