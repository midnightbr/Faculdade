/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.filter.secondarytable;


import org.hibernate.Session;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.transaction.TransactionUtil;
import org.junit.Assert;
import org.junit.Test;

public class SecondaryTableTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {User.class};
	}

	@Override
	protected void prepareTest() throws Exception {
		TransactionUtil.doInHibernate( this::sessionFactory, s -> {
			insertUser( s, "q@s.com", 21, false, "a1", "b" );
			insertUser( s, "r@s.com", 22, false, "a2", "b" );
			insertUser( s, "s@s.com", 23, true, "a3", "b" );
			insertUser( s, "t@s.com", 24, false, "a4", "b" );
		} );
	}

	@Test
	public void testFilter() {
		try (Session session = openSession()) {
			Assert.assertEquals(
					Long.valueOf( 4 ),
					session.createQuery( "select count(u) from User u" ).uniqueResult()
			);
			session.enableFilter( "ageFilter" ).setParameter( "age", 24 );
			Assert.assertEquals(
					Long.valueOf( 2 ),
					session.createQuery( "select count(u) from User u" ).uniqueResult()
			);
		}
	}

	private void insertUser(
			Session session,
			String emailAddress,
			int age,
			boolean lockedOut,
			String username,
			String password) {
		User user = new User();
		user.setEmailAddress( emailAddress );
		user.setAge( age );
		user.setLockedOut( lockedOut );
		user.setUsername( username );
		user.setPassword( password );
		session.persist( user );
	}

}
