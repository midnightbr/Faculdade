/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.test.collection.delayedOperation;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests delayed operations that are queued for a PersistentSet. remove( Object )
 * requires extra lazy to queue the operations.
 * @author Gail Badner
 */
public class ListDelayedOperationTest extends BaseCoreFunctionalTestCase {
	private Long parentId;
	private Long childId1;
	private Long childId2;

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Parent.class,
				Child.class
		};
	}

	@Before
	public void setup() {
		// start by cleaning up in case a test fails
		if ( parentId != null ) {
			cleanup();
		}

		Parent parent = new Parent();
		Child child1 = new Child( "Sherman" );
		Child child2 = new Child( "Yogi" );
		parent.addChild( child1 );
		parent.addChild( child2 );

		Session s = openSession();
		s.getTransaction().begin();
		s.persist( parent );
		s.getTransaction().commit();
		s.close();

		parentId = parent.getId();
		childId1 = child1.getId();
		childId2 = child2.getId();
	}

	@After
	public void cleanup() {
		Session s = openSession();
		s.getTransaction().begin();
		Parent parent = s.get( Parent.class, parentId );
		parent.getChildren().clear();
		s.delete( parent );
		s.getTransaction().commit();
		s.close();

		parentId = null;
	}

	@Test
	@TestForIssue( jiraKey = "HHH-5855")
	public void testSimpleAddDetached() {
		// Create 2 detached Child objects.
		Session s = openSession();
		s.getTransaction().begin();
		Child c1 = new Child( "Darwin" );
		s.persist( c1 );
		Child c2 = new Child( "Comet" );
		s.persist( c2 );
		s.getTransaction().commit();
		s.close();

		// Now Child c is detached.

		s = openSession();
		s.getTransaction().begin();
		Parent p = s.get( Parent.class, parentId );
		assertFalse( Hibernate.isInitialized( p.getChildren() ) );
		// add detached Child c
		p.addChild( c1 );
		// collection should still be uninitialized
		assertFalse( Hibernate.isInitialized( p.getChildren() ) );
		s.getTransaction().commit();
		s.close();

		// Add a detached Child and commit
		s = openSession();
		s.getTransaction().begin();
		p = s.get( Parent.class, parentId );
		assertFalse( Hibernate.isInitialized( p.getChildren() ) );
		assertEquals( 3, p.getChildren().size() );
		s.getTransaction().commit();
		s.close();

		// Add another detached Child, merge, and commit
		s = openSession();
		s.getTransaction().begin();
		p = s.get( Parent.class, parentId );
		assertFalse( Hibernate.isInitialized( p.getChildren() ) );
		p.addChild( c2 );
		assertFalse( Hibernate.isInitialized( p.getChildren() ) );
		s.merge( p );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();
		p = s.get( Parent.class, parentId );
		assertFalse( Hibernate.isInitialized( p.getChildren() ) );
		assertEquals( 4, p.getChildren().size() );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-5855")
	public void testSimpleAddTransient() {
		// Add a transient Child and commit.
		Session s = openSession();
		s.getTransaction().begin();
		Parent p = s.get( Parent.class, parentId );
		assertFalse( Hibernate.isInitialized( p.getChildren() ) );
		// add transient Child
		p.addChild( new Child( "Darwin" ) );
		// collection should still be uninitialized
		assertFalse( Hibernate.isInitialized( p.getChildren() ) );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();
		p = s.get( Parent.class, parentId );
		assertFalse( Hibernate.isInitialized( p.getChildren() ) );
		assertEquals( 3, p.getChildren().size() );
		s.getTransaction().commit();
		s.close();

		// Add another transient Child and commit again.
		s = openSession();
		s.getTransaction().begin();
		p = s.get( Parent.class, parentId );
		assertFalse( Hibernate.isInitialized( p.getChildren() ) );
		// add transient Child
		p.addChild( new Child( "Comet" ) );
		// collection should still be uninitialized
		assertFalse( Hibernate.isInitialized( p.getChildren() ) );
		s.merge( p );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();
		p = s.get( Parent.class, parentId );
		assertFalse( Hibernate.isInitialized( p.getChildren() ) );
		assertEquals( 4, p.getChildren().size() );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-5855")
	public void testSimpleAddManaged() {
		// Add 2 Child entities
		Session s = openSession();
		s.getTransaction().begin();
		Child c1 = new Child( "Darwin" );
		s.persist( c1 );
		Child c2 = new Child( "Comet" );
		s.persist( c2 );
		s.getTransaction().commit();
		s.close();

		// Add a managed Child and commit
		s = openSession();
		s.getTransaction().begin();
		Parent p = s.get( Parent.class, parentId );
		assertFalse( Hibernate.isInitialized( p.getChildren() ) );
		// get the first Child so it is managed; add to collection
		p.addChild( s.get( Child.class, c1.getId() ) );
		// collection should still be uninitialized
		assertFalse( Hibernate.isInitialized( p.getChildren() ) );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();
		p = s.get( Parent.class, parentId );
		assertFalse( Hibernate.isInitialized( p.getChildren() ) );
		assertEquals( 3, p.getChildren().size() );
		s.getTransaction().commit();
		s.close();

		// Add the other managed Child, merge and commit.
		s = openSession();
		s.getTransaction().begin();
		p = s.get( Parent.class, parentId );
		assertFalse( Hibernate.isInitialized( p.getChildren() ) );
		// get the second Child so it is managed; add to collection
		p.addChild( s.get( Child.class, c2.getId() ) );
		// collection should still be uninitialized
		assertFalse( Hibernate.isInitialized( p.getChildren() ) );
		s.merge( p );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();
		p = s.get( Parent.class, parentId );
		assertFalse( Hibernate.isInitialized( p.getChildren() ) );
		assertEquals( 4, p.getChildren().size() );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-5855")
	public void testSimpleRemoveDetached() {
		// Get the 2 Child entities and detach.
		Session s = openSession();
		s.getTransaction().begin();
		Child c1 = s.get( Child.class, childId1 );
		Child c2 = s.get( Child.class, childId2 );
		s.getTransaction().commit();
		s.close();

		// Remove a detached entity element and commit
		s = openSession();
		s.getTransaction().begin();
		Parent p = s.get( Parent.class, parentId );
		assertFalse( Hibernate.isInitialized( p.getChildren() ) );
		// remove a detached element and commit
		Hibernate.initialize( p.getChildren() );
		p.removeChild( c1 );
		for ( Child c : p.getChildren() ) {
			if ( c.equals( c1 ) ) {
				s.evict( c );
			}
		}
		assertTrue( Hibernate.isInitialized( p.getChildren() ) );
		//s.merge( p );
		s.getTransaction().commit();
		s.close();

		// Remove a detached entity element, merge, and commit
		s = openSession();
		s.getTransaction().begin();
		p = s.get( Parent.class, parentId );
		assertFalse( Hibernate.isInitialized( p.getChildren() ) );
		Hibernate.initialize( p.getChildren() );
		assertEquals( 1, p.getChildren().size() );
		s.getTransaction().commit();
		s.close();

		// Remove a detached entity element, merge, and commit
		s = openSession();
		s.getTransaction().begin();
		p = s.get( Parent.class, parentId );
		assertFalse( Hibernate.isInitialized( p.getChildren() ) );
		// remove a detached element and commit
		p.removeChild( c2 );
		assertFalse( Hibernate.isInitialized( p.getChildren() ) );
		p = (Parent) s.merge( p );
		Hibernate.initialize( p );
		s.getTransaction().commit();
		s.close();

		// Remove a detached entity element, merge, and commit
		s = openSession();
		s.getTransaction().begin();
		p = s.get( Parent.class, parentId );
		assertFalse( Hibernate.isInitialized( p.getChildren() ) );
		assertEquals( 0, p.getChildren().size() );
		s.getTransaction().commit();
		s.close();
	}

/* STILL WORKING ON THIS ONE...
	@Test
	@TestForIssue( jiraKey = "HHH-5855")
	public void testSimpleRemoveManaged() {
		// Remove a managed entity element and commit
		Session s = openSession();
		s.getTransaction().begin();
		Parent p = s.get( Parent.class, parentId );
		assertFalse( Hibernate.isInitialized( p.getChildren() ) );
		// get c1 so it is managed, then remove and commit
		p.removeChild( s.get( Child.class, childId1 ) );
		assertFalse( Hibernate.isInitialized( p.getChildren() ) );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();
		p = s.get( Parent.class, parentId );
		assertFalse( Hibernate.isInitialized( p.getChildren() ) );
		assertEquals( 1, p.getChildren().size() );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();
		p = s.get( Parent.class, parentId );
		assertFalse( Hibernate.isInitialized( p.getChildren() ) );
		// get c1 so it is managed, then remove, merge and commit
		p.removeChild( s.get( Child.class, childId2 ) );
		assertFalse( Hibernate.isInitialized( p.getChildren() ) );
		s.merge( p );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();
		p = s.get( Parent.class, parentId );
		assertFalse( Hibernate.isInitialized( p.getChildren() ) );
		assertEquals( 0, p.getChildren().size() );
		s.getTransaction().commit();
		s.close();
	}
*/

	@Entity(name = "Parent")
	public static class Parent {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		private Long id;

		@OneToMany(cascade = CascadeType.ALL, mappedBy = "parent", orphanRemoval = true)
		@LazyCollection(LazyCollectionOption.EXTRA )
		@OrderColumn
		private List<Child> children = new ArrayList<Child>();

		public Parent() {
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public List<Child> getChildren() {
			return children;
		}

		public void addChild(Child child) {
			children.add(child);
			child.setParent(this);
		}

		public void addChild(Child child, int i) {
			children.add(i, child );
			child.setParent(this);
		}

		public void removeChild(Child child) {
			children.remove(child);
			child.setParent(null);
		}
	}

	@Entity(name = "Child")
	public static class Child {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		private Long id;

		@Column(nullable = false)
		private String name;

		@ManyToOne
		private Parent parent;

		public Child() {
		}

		public Child(String name) {
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Parent getParent() {
			return parent;
		}

		public void setParent(Parent parent) {
			this.parent = parent;
		}

		@Override
		public String toString() {
			return "Child{" +
					"id=" + id +
					", name='" + name + '\'' +
					'}';
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			Child child = (Child) o;

			return name.equals( child.name );

		}

		@Override
		public int hashCode() {
			return name.hashCode();
		}
	}

}
