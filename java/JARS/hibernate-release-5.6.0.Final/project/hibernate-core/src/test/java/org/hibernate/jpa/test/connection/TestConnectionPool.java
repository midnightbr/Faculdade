/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.connection;

import java.util.Map;
import java.util.Properties;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.PersistenceException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProviderImpl;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.exception.SQLGrammarException;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-11257")
public class TestConnectionPool
		extends BaseEntityManagerFunctionalTestCase {

	private final static int CONNECTION_POOL_SIZE = 2;

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] { TestEntity.class };
	}

	@Override
	protected boolean createSchema() {
		return false;
	}

	@Override
	protected void addConfigOptions(Map options) {
		options.put(
				AvailableSettings.POOL_SIZE,
				Integer.valueOf( CONNECTION_POOL_SIZE )
		);
		options.put( "hibernate.connection.customProperty", "x" );
		options.put( AvailableSettings.CONNECTION_PROVIDER_DISABLES_AUTOCOMMIT, "true" );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-13700")
	public void testConnectionPoolPropertyFiltering() {
		ConnectionProvider cp = serviceRegistry().getService( ConnectionProvider.class );
		DriverManagerConnectionProviderImpl dmcp = (DriverManagerConnectionProviderImpl) cp;
		Properties connectionProperties = dmcp.getConnectionProperties();
		Assert.assertEquals( "x", connectionProperties.getProperty( "customProperty" ) );
		Assert.assertNull( connectionProperties.getProperty( "pool_size" ) );
		Assert.assertNull( connectionProperties.getProperty( "provider_disables_autocommit" ) );
	}

	@Test
	public void testConnectionPoolDoesNotConsumeAllConnections() {
		for ( int i = 0; i < CONNECTION_POOL_SIZE + 1; ++i ) {
			EntityManager entityManager = getOrCreateEntityManager();
			try {
				for ( int j = 0; j < 2; j++ ) {
					try {
						final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
						final CriteriaQuery<TestEntity> criteriaQuery = builder.createQuery(
								TestEntity.class );
						criteriaQuery.select( criteriaQuery.from( TestEntity.class ) );

						entityManager.createQuery( criteriaQuery ).getResultList();
					}
					catch ( PersistenceException e ) {
						if ( e.getCause() instanceof SQLGrammarException ) {
							//expected, the schema was not created
						}
						else {
							throw e;
						}
					}
				}
			}
			finally {
				entityManager.close();
			}
		}
	}

	@Entity(name = "Test_Entity")
	public static class TestEntity {

		@Id
		public long id;
	}
}
