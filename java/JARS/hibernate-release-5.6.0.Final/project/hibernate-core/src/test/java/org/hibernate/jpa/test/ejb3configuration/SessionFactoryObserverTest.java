/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.ejb3configuration;

import java.util.Collections;
import javax.persistence.EntityManagerFactory;

import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder;
import org.hibernate.jpa.test.PersistenceUnitInfoAdapter;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="mailto:emmanuel@hibernate.org">Emmanuel Bernard</a>
 */
public class SessionFactoryObserverTest {
    @Test
	public void testSessionFactoryObserverProperty() {

		EntityManagerFactoryBuilder builder = Bootstrap.getEntityManagerFactoryBuilder(
				new PersistenceUnitInfoAdapter(),
				Collections.singletonMap(
						AvailableSettings.SESSION_FACTORY_OBSERVER,
						GoofySessionFactoryObserver.class.getName()
				)
		);

		try {
			final EntityManagerFactory entityManagerFactory = builder.build();
			entityManagerFactory.close();
			Assert.fail( "GoofyException should have been thrown" );
		}
		catch ( GoofyException e ) {
			//success
		}
	}

	public static class GoofySessionFactoryObserver implements SessionFactoryObserver {
		public void sessionFactoryCreated(SessionFactory factory) {
		}

		public void sessionFactoryClosed(SessionFactory factory) {
			throw new GoofyException();
		}
	}

	public static class GoofyException extends RuntimeException {
	}
}
