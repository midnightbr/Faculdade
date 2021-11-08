/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.generatedkeys.identity;

import javax.persistence.PersistenceException;
import javax.persistence.TransactionRequiredException;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * @author Steve Ebersole
 */
@RequiresDialectFeature( DialectChecks.SupportsIdentityColumns.class )
public class IdentityGeneratedKeysTest extends BaseCoreFunctionalTestCase {
	public void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.GENERATE_STATISTICS, "true" );
	}

	public String[] getMappings() {
		return new String[] { "generatedkeys/identity/MyEntity.hbm.xml" };
	}

	@Test
	public void testIdentityColumnGeneratedIds() {
		Session s = openSession();
		s.beginTransaction();
		MyEntity myEntity = new MyEntity( "test" );
		Long id = ( Long ) s.save( myEntity );
		assertNotNull( "identity column did not force immediate insert", id );
		assertEquals( id, myEntity.getId() );
		s.delete( myEntity );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testPersistOutsideTransaction() {
		Session s = openSession();
		try {
			// first test save() which should force an immediate insert...
			MyEntity myEntity1 = new MyEntity( "test-save" );
			Long id = (Long) s.save( myEntity1 );
			assertNotNull( "identity column did not force immediate insert", id );
			assertEquals( id, myEntity1.getId() );

			// next test persist() which should cause a delayed insert...
			long initialInsertCount = sessionFactory().getStatistics().getEntityInsertCount();
			MyEntity myEntity2 = new MyEntity( "test-persist" );
			s.persist( myEntity2 );
			assertEquals( "persist on identity column not delayed", initialInsertCount, sessionFactory().getStatistics().getEntityInsertCount() );
			assertNull( myEntity2.getId() );

			// an explicit flush should cause execution of the delayed insertion
			s.flush();
			fail( "TransactionRequiredException required upon flush" );
		}
		catch ( PersistenceException ex ) {
			// expected
			assertTyping( TransactionRequiredException.class, ex );
		}
		finally {
			s.close();
		}
	}

	@Test
	@SuppressWarnings( {"unchecked"})
	public void testPersistOutsideTransactionCascadedToNonInverseCollection() {
		long initialInsertCount = sessionFactory().getStatistics().getEntityInsertCount();
		Session s = openSession();
		try {
			MyEntity myEntity = new MyEntity( "test-persist" );
			myEntity.getNonInverseChildren().add( new MyChild( "test-child-persist-non-inverse" ) );
			s.persist( myEntity );
			assertEquals( "persist on identity column not delayed", initialInsertCount, sessionFactory().getStatistics().getEntityInsertCount() );
			assertNull( myEntity.getId() );
			s.flush();
			fail( "TransactionRequiredException required upon flush" );
		}
		catch ( PersistenceException ex ) {
			// expected
			assertTyping( TransactionRequiredException.class, ex );
		}
		finally {
			s.close();
		}
	}

	@Test
	@SuppressWarnings( {"unchecked"})
	public void testPersistOutsideTransactionCascadedToInverseCollection() {
		long initialInsertCount = sessionFactory().getStatistics().getEntityInsertCount();
		Session s = openSession();
		try {
			MyEntity myEntity2 = new MyEntity( "test-persist-2" );
			MyChild child = new MyChild( "test-child-persist-inverse" );
			myEntity2.getInverseChildren().add( child );
			child.setInverseParent( myEntity2 );
			s.persist( myEntity2 );
			assertEquals( "persist on identity column not delayed", initialInsertCount, sessionFactory().getStatistics().getEntityInsertCount() );
			assertNull( myEntity2.getId() );
			s.flush();
			fail( "TransactionRequiredException expected upon flush." );
		}
		catch ( PersistenceException ex ) {
			// expected
			assertTyping( TransactionRequiredException.class, ex );
		}
		finally {
			s.close();
		}
	}

	@Test
	public void testPersistOutsideTransactionCascadedToManyToOne() {
		long initialInsertCount = sessionFactory().getStatistics().getEntityInsertCount();
		Session s = openSession();
		try {
			MyEntity myEntity = new MyEntity( "test-persist" );
			myEntity.setSibling( new MySibling( "test-persist-sibling-out" ) );
			s.persist( myEntity );
			assertEquals( "persist on identity column not delayed", initialInsertCount, sessionFactory().getStatistics().getEntityInsertCount() );
			assertNull( myEntity.getId() );
			s.flush();
			fail( "TransactionRequiredException expected upon flush." );
		}
		catch ( PersistenceException ex ) {
			// expected
			assertTyping( TransactionRequiredException.class, ex );
		}
		finally {
			s.close();
		}
	}

	@Test
	public void testPersistOutsideTransactionCascadedFromManyToOne() {
		long initialInsertCount = sessionFactory().getStatistics().getEntityInsertCount();
		Session s = openSession();
		try {
			MyEntity myEntity2 = new MyEntity( "test-persist-2" );
			MySibling sibling = new MySibling( "test-persist-sibling-in" );
			sibling.setEntity( myEntity2 );
			s.persist( sibling );
			assertEquals( "persist on identity column not delayed", initialInsertCount, sessionFactory().getStatistics().getEntityInsertCount() );
			assertNull( myEntity2.getId() );
			s.flush();
			fail( "TransactionRequiredException expected upon flush." );
		}
		catch ( PersistenceException ex ) {
			// expected
			assertTyping( TransactionRequiredException.class, ex );
		}
		finally {
			s.close();
		}
	}
}
