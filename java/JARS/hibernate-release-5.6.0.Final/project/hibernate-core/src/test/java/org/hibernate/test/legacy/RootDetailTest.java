/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.legacy;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

import org.hibernate.Hibernate;
import org.hibernate.LockMode;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.criterion.Example;
import org.hibernate.criterion.Restrictions;
import org.hibernate.dialect.*;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.jdbc.AbstractWork;
import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.SkipLog;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class RootDetailTest extends LegacyTestCase {
	@Override
	public String[] getMappings() {
		return new String[] {
			"legacy/RootDetail.hbm.xml",
			"legacy/Custom.hbm.xml",
			"legacy/Category.hbm.xml",
			"legacy/Nameable.hbm.xml",
			"legacy/SingleSeveral.hbm.xml",
			"legacy/WZ.hbm.xml",
			"legacy/UpDown.hbm.xml",
			"legacy/Eye.hbm.xml"
		};
	}

	@Override
	public void configure(Configuration cfg) {
		super.configure(cfg);
		Properties props = new Properties();
		props.put( Environment.ORDER_INSERTS, "true" );
		props.put( Environment.STATEMENT_BATCH_SIZE, "10" );
		cfg.addProperties( props );
	}

	@Test
	public void testOuterJoin() throws Exception {
		Session s = openSession();
		s.beginTransaction();
		Eye e = new Eye();
		e.setName("Eye Eye");
		Jay jay = new Jay(e);
		e.setJay( jay );
		s.saveOrUpdate( e );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		e = (Eye) s.createCriteria(Eye.class).uniqueResult();
		assertTrue( Hibernate.isInitialized( e.getJay() ) );
		assertTrue( Hibernate.isInitialized( e.getJays() ) );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		jay = (Jay) s.createQuery("select new Jay(eye) from Eye eye").uniqueResult();
		assertTrue( "Eye Eye".equals( jay.getEye().getName() ) );
		s.delete( jay.getEye() );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	@SuppressWarnings( {"unchecked"})
	public void testCopy() throws Exception {
		Category catWA = new Category();
		catWA.setName("HSQL workaround");
		Category cat = new Category();
		cat.setName("foo");
		Category subCatBar = new Category();
		subCatBar.setName("bar");
		Category subCatBaz = new Category();
		subCatBaz.setName("baz");
		cat.getSubcategories().add(subCatBar);
		cat.getSubcategories().add(subCatBaz);

		Session s = openSession();
		s.beginTransaction();
		s.save( catWA );
		s.save( cat );
		s.getTransaction().commit();
		s.close();

		cat.setName("new foo");
		subCatBar.setName("new bar");
		cat.getSubcategories().remove(subCatBaz);
		Category newCat = new Category();
		newCat.setName("new");
		cat.getSubcategories().add(newCat);
		Category newSubCat = new Category();
		newSubCat.setName("new sub");
		newCat.getSubcategories().add(newSubCat);

		s = openSession();
		s.beginTransaction();
		Category copiedCat = (Category) s.merge( cat );
		s.getTransaction().commit();
		s.close();

		assertFalse( copiedCat==cat );
		//assertFalse( copiedCat.getSubcategories().contains(newCat) );
		assertTrue( cat.getSubcategories().contains(newCat) );

		s = openSession();
		cat = (Category) s.createQuery("from Category cat where cat.name='new foo'").uniqueResult();
		newSubCat = (Category) s.createQuery("from Category cat left join fetch cat.subcategories where cat.name='new sub'").uniqueResult();
		assertTrue( newSubCat.getName().equals("new sub") );
		s.close();

		newSubCat.getSubcategories().add(cat);
		cat.setName("new new foo");

		s = openSession();
		s.beginTransaction();
		s.delete(cat);
		s.delete( subCatBaz );
		s.delete( catWA );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testNotNullDiscriminator() throws Exception {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Up up = new Up();
		up.setId1("foo");
		up.setId2(123l);
		Down down = new Down();
		down.setId1("foo");
		down.setId2(321l);
		down.setValue(12312312l);
		s.save(up);
		s.save(down);
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		List list = s.createQuery( "from Up up order by up.id2 asc" ).list();
		assertTrue( list.size()==2 );
		assertFalse( list.get(0) instanceof Down );
		assertTrue( list.get(1) instanceof Down );
		list = s.createQuery( "from Down down" ).list();
		assertTrue( list.size()==1 );
		assertTrue( list.get(0) instanceof Down );
		//list = s.find("from Up down where down.class = Down");
		assertTrue( list.size()==1 );
		assertTrue( list.get(0) instanceof Down );
		for ( Object entity : s.createQuery( "from Up" ).list() ) {
			s.delete( entity );
		}
		t.commit();
		s.close();

	}

	@Test
	@SkipForDialect(value = CockroachDB192Dialect.class, comment = "https://github.com/cockroachdb/cockroach/issues/27871")
	public void testSelfManyToOne() throws Exception {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Root m = new Root();
		m.setOtherRoot(m);
		s.save(m);
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		Iterator i = s.createQuery( "from Root" ).iterate();
		m = (Root) i.next();
		assertTrue( m.getOtherRoot()==m );
		if ( getDialect() instanceof HSQLDialect || getDialect() instanceof MySQLDialect ) {
			m.setOtherRoot(null);
			s.flush();
		}
		s.delete(m);
		t.commit();
		s.close();
	}

	@Test
	@SkipForDialect(value = CockroachDB192Dialect.class, comment = "https://github.com/cockroachdb/cockroach/issues/27871")
	public void testExample() throws Exception {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Root m = new Root();
		m.setName("name");
		m.setX(5);
		m.setOtherRoot(m);
		s.save(m);
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		Root m1 = (Root) s.createCriteria( Root.class)
			.add( Example.create(m).enableLike().ignoreCase().excludeProperty("bigDecimal") )
			.uniqueResult();
		assertTrue( m1.getOtherRoot()==m1 );
		m1 = (Root) s.createCriteria( Root.class)
			.add( Restrictions.eq("name", "foobar") )
			.uniqueResult();
		assertTrue( m1==null );
		m1 = (Root) s.createCriteria( Root.class)
			.add( Example.create(m).excludeProperty("bigDecimal") )
			.createCriteria("otherRoot")
				.add( Example.create(m).excludeZeroes().excludeProperty("bigDecimal") )
			.uniqueResult();
		assertTrue( m1.getOtherRoot()==m1 );
		Root m2 = (Root) s.createCriteria( Root.class)
			.add( Example.create(m).excludeNone().excludeProperty("bigDecimal") )
			.uniqueResult();
		assertTrue( m2==m1 );
		m.setName(null);
		m2 = (Root) s.createCriteria( Root.class)
			.add( Example.create(m).excludeNone().excludeProperty("bigDecimal") )
			.uniqueResult();
		assertTrue( null == m2 );
		if (getDialect() instanceof HSQLDialect || getDialect() instanceof MySQLDialect) {
			m1.setOtherRoot(null);
			s.flush();
		}
		s.delete(m1);
		t.commit();
		s.close();
	}

	@Test
	public void testNonLazyBidirectional() throws Exception {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Single sin = new Single();
		sin.setId("asdfds");
		sin.setString("adsa asdfasd");
		Several sev = new Several();
		sev.setId("asdfasdfasd");
		sev.setString("asd ddd");
		sin.getSeveral().add(sev);
		sev.setSingle(sin);
		s.save(sin);
		t.commit();
		s.close();
		s = openSession();
		t = s.beginTransaction();
		sin = (Single) s.load( Single.class, sin );
		t.commit();
		s.close();
		s = openSession();
		t = s.beginTransaction();
		sev = (Several) s.load( Several.class, sev );
		t.commit();
		s.close();
		s = openSession();
		t = s.beginTransaction();
		s.createQuery( "from Several" ).list();
		t.commit();
		s.close();
		s = openSession();
		t = s.beginTransaction();
		for ( Object entity : s.createQuery( "from Single" ).list() ) {
			s.delete( entity );
		}
		t.commit();
		s.close();
	}

	@Test
	public void testCollectionQuery() throws Exception {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		if ( !(getDialect() instanceof MySQLDialect) && !(getDialect() instanceof SAPDBDialect) && !(getDialect() instanceof MckoiDialect) ) {
			s.createQuery( "FROM Root m WHERE NOT EXISTS ( FROM m.details d WHERE NOT d.i=5 )" ).iterate();
			s.createQuery( "FROM Root m WHERE NOT 5 IN ( SELECT d.i FROM m.details AS d )" ).iterate();
		}
		s.createQuery( "SELECT m FROM Root m JOIN m.details d WHERE d.i=5" ).iterate();
		s.createQuery( "SELECT m FROM Root m JOIN m.details d WHERE d.i=5" ).list();
		s.createQuery( "SELECT m.id FROM Root AS m JOIN m.details AS d WHERE d.i=5" ).list();
		t.commit();
		s.close();
	}

	@Test
	public void tesRootDetail() throws Exception {
		if (getDialect() instanceof HSQLDialect) return;

		Session s = openSession();
		Transaction t = s.beginTransaction();
		Root root = new Root();
		assertTrue( "save returned native id", s.save( root )!=null );
		Serializable mid = s.getIdentifier( root );
		Detail d1 = new Detail();
		d1.setRoot( root );
		Serializable did = s.save(d1);
		Detail d2 = new Detail();
		d2.setI(12);
		d2.setRoot( root );
		assertTrue( "generated id returned", s.save(d2)!=null);
		root.addDetail(d1);
		root.addDetail(d2);
		if ( !(getDialect() instanceof MySQLDialect) && !(getDialect() instanceof SAPDBDialect) && !(getDialect() instanceof MckoiDialect) && !(getDialect() instanceof org.hibernate.dialect.TimesTenDialect)) {
			assertTrue(
				"query",
					s.createQuery(
							"from Detail d, Root m where m = d.root and size(m.outgoing) = 0 and size(m.incoming) = 0"
					).list().size()==2
			);
		}
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		root = new Root();
		s.load( root, mid);
		assertTrue( root.getDetails().size()==2 );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		root = (Root) s.load( Root.class, mid);
		Iterator iter = root.getDetails().iterator();
		int i=0;
		while ( iter.hasNext() ) {
			Detail d = (Detail) iter.next();
			assertTrue( "root-detail", d.getRoot()== root );
			i++;
		}
		assertTrue( "root-detail", i==2 );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		assertTrue( s.createQuery( "select elements(root.details) from Root root" ).list().size()==2 );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		List list = s.createQuery( "from Root m left join fetch m.details" ).list();
		Root m = (Root) list.get(0);
		assertTrue( Hibernate.isInitialized( m.getDetails() ) );
		assertTrue( m.getDetails().size()==2 );
		list = s.createQuery( "from Detail d inner join fetch d.root" ).list();
		Detail dt = (Detail) list.get(0);
		Serializable dtid = s.getIdentifier(dt);
		assertTrue( dt.getRoot()==m );

		//assertTrue(m.getAllDetails().size()==2);

		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		list = s.createQuery( "select m from Root m1, Root m left join fetch m.details where m.name=m1.name" )
				.list();
		assertTrue( Hibernate.isInitialized( ( (Root) list.get(0) ).getDetails() ) );
		dt = (Detail) s.load(Detail.class, dtid);
		assertTrue( ( (Root) list.get(0) ).getDetails().contains(dt) );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		list = s.createQuery(
				"select m, m1.name from Root m1, Root m left join fetch m.details where m.name=m1.name"
		).list();
		assertTrue( Hibernate.isInitialized( ( (Root) ( (Object[]) list.get(0) )[0] ).getDetails() ) );
		dt = (Detail) s.load(Detail.class, dtid);
		assertTrue( ( (Root) ( (Object[]) list.get(0) )[0] ).getDetails().contains(dt) );
		t.commit();
		s.close();


		s = openSession();
		t = s.beginTransaction();
		Detail dd = (Detail) s.load(Detail.class, did);
		root = dd.getRoot();
		assertTrue( "detail-root", root.getDetails().contains(dd) );
		assertTrue( s.createFilter( root.getDetails(), "order by this.i desc" ).list().size()==2 );
		assertTrue( s.createFilter( root.getDetails(), "select this where this.id > -1" ).list().size()==2 );
		Query q = s.createFilter( root.getDetails(), "where this.id > :id" );
		q.setInteger("id", -1);
		assertTrue( q.list().size()==2 );
		q = s.createFilter( root.getDetails(), "where this.id > :id1 and this.id < :id2" );
		q.setInteger("id1", -1);
		q.setInteger("id2", 99999999);
		assertTrue( q.list().size()==2 );
		q.setInteger("id2", -1);
		assertTrue( q.list().size()==0 );

		list = new ArrayList();
		list.add(did);
		list.add( new Long(-1) );

		q = s.createFilter( root.getDetails(), "where this.id in (:ids)" );
		q.setParameterList("ids", list);
		assertTrue( q.list().size()==1 );

		q = s.createFilter( root.getDetails(), "where this.id in (:ids)" );
		q.setParameterList("ids", list);
		assertTrue( q.iterate().hasNext() );

		assertTrue( s.createFilter( root.getDetails(), "where this.id > -1" ).list().size()==2 );
		assertTrue( s.createFilter( root.getDetails(), "select this.root where this.id > -1" ).list().size()==2 );
		assertTrue(
				s.createFilter( root.getDetails(), "select m from Root m where this.id > -1 and this.root=m" )
						.list()
						.size()==2 );
		assertTrue( s.createFilter( root.getIncoming(), "where this.id > -1 and this.name is not null" ).list().size()==0 );

		assertTrue( s.createFilter( root.getDetails(), "select max(this.i)" ).iterate().next() instanceof Integer );
		assertTrue( s.createFilter( root.getDetails(), "select max(this.i) group by this.id" ).iterate().next() instanceof Integer );
		assertTrue( s.createFilter( root.getDetails(), "select count(*)" ).iterate().next() instanceof Long );

		assertTrue( s.createFilter( root.getDetails(), "select this.root" ).list().size()==2 );
		assertTrue( s.createFilter( root.getMoreDetails(), "" ).list().size()==0 );
		assertTrue( s.createFilter( root.getIncoming(), "" ).list().size()==0 );

		Query f = s.createFilter( root.getDetails(), "select max(this.i) where this.i < :top and this.i>=:bottom" );
		f.setInteger("top", 100);
		f.setInteger("bottom", 0);
		assertEquals( f.iterate().next(), new Integer(12) );
		f.setInteger("top", 2);
		assertEquals( f.iterate().next(), new Integer(0) );

		f = s.createFilter( root.getDetails(), "select max(this.i) where this.i not in (:list)" );
		Collection coll = new ArrayList();
		coll.add( new Integer(-666) );
		coll.add( new Integer(22) );
		coll.add( new Integer(0) );
		f.setParameterList("list", coll);
		assertEquals( f.iterate().next(), new Integer(12) );

		f = s.createFilter( root.getDetails(), "select max(this.i) where this.i not in (:list) and this.root.name = :listy2" );
		f.setParameterList("list", coll);
		f.setParameter( "listy2", root.getName() );
		assertEquals( f.iterate().next(), new Integer(12) );

		iter = root.getDetails().iterator();
		i=0;
		while ( iter.hasNext() ) {
			Detail d = (Detail) iter.next();
			assertTrue( "root-detail", d.getRoot()== root );
			s.delete(d);
			i++;
		}
		assertTrue( "root-detail", i==2 );
		s.delete( root );
		t.commit();
		s.close();
	}

	@Test
	public void testIncomingOutgoing() throws Exception {
		Session s = openSession();
		s.beginTransaction();
		Root root1 = new Root();
		Root root2 = new Root();
		Root root3 = new Root();
		s.save( root1 );
		s.save( root2 );
		s.save( root3 );
		root1.addIncoming( root2 );
		root2.addOutgoing( root1 );
		root1.addIncoming( root3 );
		root3.addOutgoing( root1 );
		Serializable m1id = s.getIdentifier( root1 );
		assertTrue(
				s.createFilter( root1.getIncoming(), "where this.id > 0 and this.name is not null" )
						.list()
						.size() == 2
		);
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		root1 = (Root) s.load( Root.class, m1id);
		Iterator iter = root1.getIncoming().iterator();
		int i=0;
		while ( iter.hasNext() ) {
			Root m = (Root) iter.next();
			assertTrue( "outgoing", m.getOutgoing().size()==1 );
			assertTrue( "outgoing", m.getOutgoing().contains( root1 ) );
			s.delete(m);
			i++;
		}
		assertTrue( "incoming-outgoing", i == 2 );
		s.delete( root1 );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testCascading() throws Exception {
		Session s = openSession();
		s.beginTransaction();
		Detail d1 = new Detail();
		Detail d2 = new Detail();
		d2.setI(22);
		Root m = new Root();
		Root m0 = new Root();
		Serializable m0id = s.save(m0);
		m0.addDetail(d1); m0.addDetail(d2);
		d1.setRoot(m0); d2.setRoot(m0);
		m.getMoreDetails().add(d1);
		m.getMoreDetails().add(d2);
		Serializable mid = s.save(m);
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		m = (Root) s.load( Root.class, mid);
		assertTrue( "cascade save", m.getMoreDetails().size()==2 );
		assertTrue( "cascade save", ( (Detail) m.getMoreDetails().iterator().next() ).getRoot().getDetails().size()==2 );
		s.delete( m );
		s.delete( s.load( Root.class, m0id ) );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testNamedQuery() throws Exception {
		Session s = openSession();
		s.beginTransaction();
		Query q = s.getNamedQuery("all_details");
		q.list();
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testUpdateLazyCollections() throws Exception {
		Session s = openSession();
		s.beginTransaction();
		Root m = new Root();
		s.save( m );
		Detail d1 = new Detail();
		Detail d2 = new Detail();
		d2.setX( 14 );
		d1.setRoot( m );
		d2.setRoot( m );
		s.save( d1 );
		s.save( d2 );
		m.addDetail( d1 );
		m.addDetail( d2 );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		m = (Root) s.load( Root.class, m.getId() );
		s.getTransaction().commit();
		s.close();
		m.setName("New Name");

		s = openSession();
		s.beginTransaction();
		s.update( m );
		Iterator iter = m.getDetails().iterator();
		int i=0;
		while ( iter.hasNext() ) {
			assertTrue( iter.next()!=null );
			i++;
		}
		assertTrue(i==2);
		iter = m.getDetails().iterator();
		while ( iter.hasNext() ) s.delete( iter.next() );
		s.delete( m );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testMultiLevelCascade() throws Exception {
		Session s = openSession();
		Transaction txn = s.beginTransaction();
		Detail detail = new Detail();
		SubDetail subdetail = new SubDetail();
		Root m = new Root();
		Root m0 = new Root();
		Serializable m0id = s.save(m0);
		m0.addDetail(detail);
		detail.setRoot(m0);
		m.getMoreDetails().add(detail);
		detail.setSubDetails( new HashSet() );
		detail.getSubDetails().add(subdetail);
		Serializable mid = s.save(m);
		txn.commit();
		s.close();

		s = openSession();
		txn = s.beginTransaction();
		m = (Root) s.load( Root.class, mid );
		assertTrue( ( (Detail) m.getMoreDetails().iterator().next() ).getSubDetails().size()!=0 );
		s.delete(m);
		assertTrue( s.createQuery( "from SubDetail" ).list().size()==0 );
		assertTrue( s.createQuery( "from Detail d" ).list().size()==0 );
		s.delete( s.load( Root.class, m0id) );
		txn.commit();
		s.close();
	}

	@Test
	public void testMixNativeAssigned() throws Exception {
		Session s = openSession();
		s.beginTransaction();
		Category c = new Category();
		c.setName("NAME");
		Assignable assn = new Assignable();
		assn.setId("i.d.");
		List l = new ArrayList();
		l.add( c );
		assn.setCategories( l );
		c.setAssignable( assn );
		s.save( assn );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		s.delete( assn );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	@SkipForDialect(value = AbstractHANADialect.class, comment = " HANA doesn't support tables consisting of only a single auto-generated column")
	public void testCollectionReplaceOnUpdate() throws Exception {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Category c = new Category();
		List list = new ArrayList();
		c.setSubcategories(list);
		list.add( new Category() );
		s.save(c);
		t.commit();
		s.close();
		c.setSubcategories(list);

		s = openSession();
		t = s.beginTransaction();
		s.update(c);
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		c = (Category) s.load( Category.class, new Long( c.getId() ), LockMode.UPGRADE );
		List list2 = c.getSubcategories();
		t.commit();
		s.close();

		assertTrue( !Hibernate.isInitialized( c.getSubcategories() ) );

		c.setSubcategories(list2);
		s = openSession();
		t = s.beginTransaction();
		s.update(c);
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		c = (Category) s.load( Category.class, new Long( c.getId() ), LockMode.UPGRADE );
		assertTrue( c.getSubcategories().size()==1 );
		s.delete(c);
		t.commit();
		s.close();
	}

	@Test
	@SkipForDialect(value = AbstractHANADialect.class, comment = " HANA doesn't support tables consisting of only a single auto-generated column")
	public void testCollectionReplace2() throws Exception {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Category c = new Category();
		List list = new ArrayList();
		c.setSubcategories(list);
		list.add( new Category() );
		Category c2 = new Category();
		s.save(c2);
		s.save(c);
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		c = (Category) s.load( Category.class, new Long( c.getId() ), LockMode.UPGRADE );
		List list2 = c.getSubcategories();
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		c2 = (Category) s.load( Category.class, new Long( c2.getId() ), LockMode.UPGRADE );
		c2.setSubcategories(list2);
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		c2 = (Category) s.load( Category.class, new Long( c2.getId() ), LockMode.UPGRADE );
		assertTrue( c2.getSubcategories().size()==1 );
		s.delete(c2);
		s.delete( s.load( Category.class, new Long( c.getId() ) ) );
		t.commit();
		s.close();
	}

	@Test
	@SkipForDialect(value = AbstractHANADialect.class, comment = " HANA doesn't support tables consisting of only a single auto-generated column")
	public void testCollectionReplace() throws Exception {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Category c = new Category();
		List list = new ArrayList();
		c.setSubcategories(list);
		list.add( new Category() );
		s.save(c);
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		c = (Category) s.load( Category.class, new Long( c.getId() ), LockMode.UPGRADE );
		c.setSubcategories(list);
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		c = (Category) s.load( Category.class, new Long( c.getId() ), LockMode.UPGRADE );
		List list2 = c.getSubcategories();
		t.commit();
		s.close();

		assertTrue( !Hibernate.isInitialized( c.getSubcategories() ) );

		s = openSession();
		t = s.beginTransaction();
		c = (Category) s.load( Category.class, new Long( c.getId() ), LockMode.UPGRADE );
		c.setSubcategories(list2);
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		c = (Category) s.load( Category.class, new Long( c.getId() ), LockMode.UPGRADE );
		assertTrue( c.getSubcategories().size()==1 );
		s.delete(c);
		t.commit();
		s.close();
	}

	@Test
	@SkipForDialect(value = AbstractHANADialect.class, comment = " HANA doesn't support tables consisting of only a single auto-generated column")
	public void testCategories() throws Exception {
		Session s = openSession();
		s.beginTransaction();
		Category c = new Category();
		c.setName(Category.ROOT_CATEGORY);
		Category c1 = new Category();
		Category c2 = new Category();
		Category c3 = new Category();
		c.getSubcategories().add(c1);
		c.getSubcategories().add(c2);
		c2.getSubcategories().add( null );
		c2.getSubcategories().add( c3 );
		s.save( c );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		s.lock(c, LockMode.UPGRADE);
		Category loaded = (Category) s.load( Category.class, new Long( c3.getId() ) );
		assertTrue( s.contains(c3) );
		assertTrue(loaded==c3);
		assertTrue( s.getCurrentLockMode(c3)==LockMode.NONE );
		assertTrue( s.getCurrentLockMode( c ) == LockMode.UPGRADE );
		s.flush();
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		loaded = (Category) s.load( Category.class, new Long( c.getId() ) );
		assertFalse( Hibernate.isInitialized( loaded.getSubcategories() ) );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		s.lock(loaded, LockMode.NONE);
		assertTrue( loaded.getSubcategories().size()==2 );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		c = (Category) s.load( Category.class, new Long( c.getId() ) );
		System.out.println( c.getSubcategories() );
		assertTrue( c.getSubcategories().get(0)!=null && c.getSubcategories().get(1)!=null );
		List list = ( (Category) c.getSubcategories().get(1) ).getSubcategories();
		assertTrue( list.get(1)!=null && list.get(0)==null );

		assertTrue(
				s.createQuery( "from Category c where c.name = org.hibernate.test.legacy.Category.ROOT_CATEGORY" )
						.iterate().hasNext()
		);
		s.delete( c );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	@SkipForDialect(value = AbstractHANADialect.class, comment = " HANA doesn't support tables consisting of only a single auto-generated column")
	public void testCollectionRefresh() throws Exception {
		Session s = openSession();
		s.beginTransaction();
		Category c = new Category();
		List list = new ArrayList();
		c.setSubcategories(list);
		list.add( new Category() );
		c.setName("root");
		Serializable id = s.save(c);
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		c = (Category) s.load(Category.class, id);
		s.refresh( c );
		s.flush();
		assertTrue( c.getSubcategories().size() == 1 );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		c = (Category) s.load(Category.class, id);
		assertTrue( c.getSubcategories().size() == 1 );
		s.delete( c );
		s.getTransaction().commit();
		s.close();
	}

	protected boolean isSerializableIsolationEnforced() throws Exception {
		JdbcConnectionAccess connectionAccess = sessionFactory().getServiceRegistry().getService( JdbcServices.class ).getBootstrapJdbcConnectionAccess();
		Connection conn = null;
		try {
			conn = connectionAccess.obtainConnection();
			return conn.getTransactionIsolation() >= Connection.TRANSACTION_SERIALIZABLE;
		}
		finally {
			if ( conn != null ) {
				try {
					connectionAccess.releaseConnection( conn );
				}
				catch ( Throwable ignore ) {
					// ignore...
				}
			}
		}
	}

	@Test
	@SkipForDialect(value = AbstractHANADialect.class, comment = " HANA doesn't support tables consisting of only a single auto-generated column")
	public void testCachedCollectionRefresh() throws Exception {
		if ( isSerializableIsolationEnforced() ) {
			SkipLog.reportSkip( "SERIALIZABLE isolation", "cached collection refreshing" );
			return;
		}
		Session s = openSession();
		s.beginTransaction();
		Category c = new Category();
		List list = new ArrayList();
		c.setSubcategories(list);
		list.add( new Category() );
		c.setName("root");
		Serializable id = s.save(c);
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		c = (Category) s.load(Category.class, id);
		c.getSubcategories().size(); //force load and cache
		s.getTransaction().commit();
		s.close();
		
		s = openSession();
		if ( (getDialect() instanceof MySQLDialect) ) {
			s.doWork(
					new AbstractWork() {
						@Override
						public void execute(Connection connection) throws SQLException {
							connection.setTransactionIsolation( Connection.TRANSACTION_READ_COMMITTED );
						}
					}
			);
		}
		s.beginTransaction();
		c = (Category) s.load(Category.class, id);
		c.getSubcategories().size(); //force load

		Session ss = openSession();
		ss.beginTransaction();
		Category c2 = (Category) ss.load(Category.class, id);
		ss.delete( c2.getSubcategories().get(0) );
		c2.getSubcategories().clear();
		ss.getTransaction().commit();
		ss.close();

		s.refresh(c);
		assertTrue( c.getSubcategories().size()==0 );

		ss = openSession();
		ss.beginTransaction();
		c2 = (Category) ss.load(Category.class, id);
		c2.getSubcategories().add( new Category() );
		c2.getSubcategories().add( new Category() );
		ss.getTransaction().commit();
		ss.close();

		s.refresh(c);
		assertEquals( 2, c.getSubcategories().size() );

		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		c = (Category) s.load(Category.class, id);
		assertEquals( 2, c.getSubcategories().size() );
		s.delete(c);
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testCustomPersister() throws Exception {
		Session s = openSession();
		Custom c = new Custom();
		c.setName( "foo" );
		c.id="100";
		s.beginTransaction();
		String id = id = (String) s.save( c );
		assertTrue( c == s.load( Custom.class, id ) );
		s.flush();
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		c = (Custom) s.load(Custom.class, id);
		assertTrue( c.getName().equals("foo") );
		c.setName( "bar" );
		s.flush();
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		c = (Custom) s.load(Custom.class, id);
		assertTrue( c.getName().equals("bar") );
		s.delete(c);
		s.flush();
		s.getTransaction().commit();
		s.close();
		s = openSession();
		boolean none = false;
		try {
			s.load(Custom.class, id);
		}
		catch (ObjectNotFoundException onfe) {
			none=true;
		}
		assertTrue(none);
		s.close();
	}

	@Test
	public void testInterface() throws Exception {
		Session s = openSession();
		s.beginTransaction();
		Serializable id = s.save( new BasicNameable() );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		Nameable n = (Nameable) s.load(Nameable.class, id);
		s.delete(n);
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testNoUpdateManyToOne() throws Exception {
		Session s = openSession();
		s.beginTransaction();
		W w1 = new W();
		W w2 = new W();
		Z z = new Z();
		z.setW(w1);
		s.save(z);
		s.flush();
		z.setW(w2);
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		s.update(z);
		s.flush();
		s.delete(z);
		for ( Object entity : s.createQuery( "from W" ).list() ) {
			s.delete( entity );
		}
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testQueuedBagAdds() throws Exception {
		Session s = openSession();
		s.beginTransaction();
		Assignable a = new Assignable();
		a.setId("foo");
		a.setCategories( new ArrayList() );
		Category c = new Category();
		c.setAssignable(a);
		a.getCategories().add(c);
		s.save(a);
		s.getTransaction().commit();
		s.close();

		sessionFactory().getCache().evictCollectionRegion( "org.hibernate.test.legacy.Assignable.categories" );

		s = openSession();
		s.beginTransaction();
		a = (Assignable) s.get(Assignable.class, "foo");
		c = new Category();
		c.setAssignable(a);
		a.getCategories().add(c);
		assertFalse( Hibernate.isInitialized( a.getCategories() ) );
		assertTrue( a.getCategories().size()==2 );
		s.getTransaction().commit();
		s.close();

		sessionFactory().getCache().evictCollectionRegion( "org.hibernate.test.legacy.Assignable.categories" );

		s = openSession();
		s.beginTransaction();
		a = (Assignable) s.get(Assignable.class, "foo");
		c = new Category();
		c.setAssignable(a);
		a.getCategories().add(c);
		assertFalse( Hibernate.isInitialized( a.getCategories() ) );
		s.flush();
		assertFalse( Hibernate.isInitialized( a.getCategories() ) );
		assertTrue( a.getCategories().size()==3 );
		s.getTransaction().commit();
		s.close();

		sessionFactory().getCache().evictCollectionRegion( "org.hibernate.test.legacy.Assignable.categories" );

		s = openSession();
		s.beginTransaction();
		a = (Assignable) s.get(Assignable.class, "foo");
		assertTrue( a.getCategories().size()==3 );
		s.delete(a);
		s.getTransaction().commit();
		s.close();

	}

	@Test
	@SkipForDialect(value = AbstractHANADialect.class, comment = " HANA doesn't support tables consisting of only a single auto-generated column")
	public void testPolymorphicCriteria() throws Exception {
		Session s = openSession();
		Transaction txn = s.beginTransaction();
		Category f = new Category();
		Single b = new Single();
		b.setId("asdfa");
		b.setString("asdfasdf");
		s.save(f);
		s.save(b);
		List list = s.createCriteria(Object.class).list();
		assertTrue( list.size()==2 );
		assertTrue( list.contains(f) && list.contains(b) );
		s.delete(f);
		s.delete(b);
		txn.commit();
		s.close();
	}

}
