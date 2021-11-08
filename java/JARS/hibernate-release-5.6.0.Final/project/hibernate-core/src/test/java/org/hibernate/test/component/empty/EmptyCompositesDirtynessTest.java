/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.component.empty;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * Test class for empty embedded dirtiness computation.
 *
 * @author Laurent Almeras
 */
public class EmptyCompositesDirtynessTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { ComponentEmptyEmbeddedOwner.class, ComponentEmptyEmbedded.class };
	}

	@Override
	protected void configure(Configuration configuration) {
		super.configure( configuration );
		configuration.getProperties().put( Environment.CREATE_EMPTY_COMPOSITES_ENABLED, Boolean.valueOf( false ) );
	}

	/**
	 * Test for dirtyness computation consistency when a property is an empty composite.
	 */
	@Test
	@TestForIssue(jiraKey = "HHH-7610")
	public void testCompositesEmpty() {
		Session s = openSession();
		try {
			s.getTransaction().begin();

			ComponentEmptyEmbeddedOwner owner = new ComponentEmptyEmbeddedOwner();
			s.persist( owner );

			s.flush();
			s.getTransaction().commit();

			s.clear();
			s.getTransaction().begin();
			owner = (ComponentEmptyEmbeddedOwner) s.get( ComponentEmptyEmbeddedOwner.class, owner.getId() );
			assertNull( owner.getEmbedded() );
			owner.setEmbedded( new ComponentEmptyEmbedded() );

			// technically, as all properties are null, update may not be necessary
			assertFalse( session.isDirty() ); // must be false to avoid unnecessary updates

			s.getTransaction().rollback();
		}
		finally {
			s.close();
		}
	}
}
