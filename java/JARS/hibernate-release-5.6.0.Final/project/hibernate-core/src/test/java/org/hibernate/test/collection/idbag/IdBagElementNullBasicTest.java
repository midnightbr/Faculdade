/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.collection.idbag;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;

import org.hibernate.annotations.CollectionId;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;

/**
 * @author Gail Badner
 */
public class IdBagElementNullBasicTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				AnEntity.class
		};
	}

	@Test
	public void testPersistNullValue() {
		int entityId = doInHibernate(
				this::sessionFactory, session -> {
					AnEntity e = new AnEntity();
					e.aCollection.add( null );
					session.persist( e );
					return e.id;
				}
		);

		doInHibernate(
				this::sessionFactory, session -> {
					AnEntity e = session.get( AnEntity.class, entityId );
					assertEquals( 0, e.aCollection.size() );
					assertEquals( 0, getCollectionElementRows( entityId ).size() );
					session.delete( e );
				}
		);
	}

	@Test
	public void addNullValue() {
		int entityId = doInHibernate(
				this::sessionFactory, session -> {
					AnEntity e = new AnEntity();
					session.persist( e );
					return e.id;
				}
		);

		doInHibernate(
				this::sessionFactory, session -> {
					AnEntity e = session.get( AnEntity.class, entityId );
					assertEquals( 0, e.aCollection.size() );
					assertEquals( 0, getCollectionElementRows( entityId ).size() );
					e.aCollection.add( null );
				}
		);

		doInHibernate(
				this::sessionFactory, session -> {
					AnEntity e = session.get( AnEntity.class, entityId );
					assertEquals( 0, e.aCollection.size() );
					assertEquals( 0, getCollectionElementRows( entityId ).size() );
					session.delete( e );
				}
		);
	}

	@Test
	public void testUpdateNonNullValueToNull() {
		int entityId = doInHibernate(
				this::sessionFactory, session -> {
					AnEntity e = new AnEntity();
					e.aCollection.add( "def" );
					session.persist( e );
					return e.id;
				}
		);

		doInHibernate(
				this::sessionFactory, session -> {
					AnEntity e = session.get( AnEntity.class, entityId );
					assertEquals( 1, e.aCollection.size() );
					assertEquals( 1, getCollectionElementRows( entityId ).size() );
					e.aCollection.set( 0, null );
				}
		);

		doInHibernate(
				this::sessionFactory, session -> {
					AnEntity e = session.get( AnEntity.class, entityId );
					assertEquals( 0, e.aCollection.size() );
					assertEquals( 0, getCollectionElementRows( entityId ).size() );
					session.delete( e );
				}
		);
	}

	@Test
	public void testUpdateNonNullValueToNullWithExtraValue() {
		int entityId = doInHibernate(
				this::sessionFactory, session -> {
					AnEntity e = new AnEntity();
					e.aCollection.add( "def" );
					e.aCollection.add( "ghi" );
					session.persist( e );
					return e.id;
				}
		);

		doInHibernate(
				this::sessionFactory, session -> {
					AnEntity e = session.get( AnEntity.class, entityId );
					assertEquals( 2, e.aCollection.size() );
					assertEquals( 2, getCollectionElementRows( e.id ).size() );
					if ( "def".equals( e.aCollection.get( 0 ) ) ) {
						e.aCollection.set( 0, null );
					}
					else {
						e.aCollection.set( 1, null );
					}
				}
		);

		doInHibernate(
				this::sessionFactory, session -> {
					AnEntity e = session.get( AnEntity.class, entityId );
					assertEquals( 1, e.aCollection.size() );
					assertEquals( 1, getCollectionElementRows( e.id ).size() );
					assertEquals( "ghi", e.aCollection.get( 0 ) );
					session.delete( e );
				}
		);
	}

	private List getCollectionElementRows(int id) {
		return doInHibernate(
				this::sessionFactory, session -> {
					return session.createNativeQuery(
							"SELECT aCollection FROM AnEntity_aCollection where AnEntity_id = " + id
					).list();
				}
		);
	}

	@Entity
	@Table(name="AnEntity")
	@GenericGenerator(name="increment", strategy = "increment")
	public static class AnEntity {
		@Id
		@GeneratedValue
		private int id;

		@ElementCollection
		@CollectionTable(name = "AnEntity_aCollection", joinColumns = { @JoinColumn( name = "AnEntity_id" ) })
		@CollectionId(
				//columns = { @Column },
				column = @Column( name = "collection_id" ),
				type = @Type(type = "long"),
				generator = "increment"
		)
		private List<String> aCollection = new ArrayList<>();
	}
}
