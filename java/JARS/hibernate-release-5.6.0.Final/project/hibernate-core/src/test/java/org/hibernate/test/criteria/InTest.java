/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.criteria;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.hibernate.dialect.DerbyDialect;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Dragan Bozanovic (draganb)
 */
public class InTest extends BaseCoreFunctionalTestCase {

	public String[] getMappings() {
		return new String[]{ "criteria/Person.hbm.xml" };
	}

	@Test
	public void testIn() {
		Session session = openSession();
		Transaction tx = session.beginTransaction();
		session.save( new Woman() );
		session.save( new Man() );
		session.flush();
		tx.commit();
		session.close();
		session = openSession();
		tx = session.beginTransaction();
		List persons = session.createCriteria( Person.class ).add(
				Restrictions.in( "class", Woman.class ) ).list();
		assertEquals( 1, persons.size() );
		tx.rollback();
		session.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-8901" )
	@RequiresDialectFeature(DialectChecks.NotSupportsEmptyInListCheck.class)
	@SkipForDialect(value = DerbyDialect.class, comment = "Derby doesn't like `x in (null)`")
	public void testEmptyInListForDialectNotSupportsEmptyInList() {
		Session session = openSession();
		Transaction tx = session.beginTransaction();
		session.save( new Woman() );
		session.save( new Man() );
		session.flush();
		tx.commit();
		session.close();
		session = openSession();
		tx = session.beginTransaction();
		List persons = session.createCriteria( Person.class ).add(
				Restrictions.in( "name", Collections.emptySet() ) ).list();
		assertEquals( 0, persons.size() );
		tx.rollback();
		session.close();
	}

	@Override
	protected boolean isCleanupTestDataRequired() {
		return true;
	}

}
