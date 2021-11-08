/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.legacy;
import java.util.List;

import org.junit.Test;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Example;
import org.hibernate.criterion.Restrictions;
import org.hibernate.dialect.SybaseASE15Dialect;
import org.hibernate.testing.SkipForDialect;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Query by example test to allow nested components
 *
 * @author Emmanuel Bernard
 */
public class QueryByExampleTest extends LegacyTestCase {
	@Override
    public String[] getMappings() {
        return new String[] { "legacy/Componentizable.hbm.xml" };
    }

	@Test
    public void testSimpleQBE() throws Exception {
    	deleteData();
        initData();

        Session s = openSession();

        Transaction t = s.beginTransaction();
        Componentizable componentizable = getComponentizeable("hibernate", "open sourc%", "open source1");
        Criteria crit = s.createCriteria(Componentizable.class);
        Example ex = Example.create(componentizable).enableLike();
        crit.add(ex);
        List result = crit.list();
        assertNotNull(result);
        assertEquals(1, result.size());

        t.commit();
        s.close();
    }

	@Test
    @SkipForDialect( value = SybaseASE15Dialect.class, jiraKey = "HHH-4580")
    public void testJunctionNotExpressionQBE() throws Exception {
        deleteData();
        initData();
        Session s = openSession();
        Transaction t = s.beginTransaction();
        Componentizable componentizeable = getComponentizeable("hibernate", null, "ope%");
        Criteria crit = s.createCriteria(Componentizable.class);
        Example ex = Example.create(componentizeable).enableLike();

        crit.add(Restrictions.or(Restrictions.not(ex), ex));

        List result = crit.list();
        assertNotNull(result);
        assertEquals(2, result.size());
        t.commit();
        s.close();

    }

	@Test
    public void testExcludingQBE() throws Exception {
        deleteData();
        initData();
        Session s = openSession();
        Transaction t = s.beginTransaction();
        Componentizable componentizeable = getComponentizeable("hibernate", null, "ope%");
        Criteria crit = s.createCriteria(Componentizable.class);
        Example ex = Example.create(componentizeable).enableLike()
            .excludeProperty("component.subComponent");
        crit.add(ex);
        List result = crit.list();
        assertNotNull(result);
        assertEquals(3, result.size());

        componentizeable = getComponentizeable("hibernate", "ORM tool", "fake stuff");
        crit = s.createCriteria(Componentizable.class);
        ex = Example.create(componentizeable).enableLike()
            .excludeProperty("component.subComponent.subName1");
        crit.add(ex);
        result = crit.list();
        assertNotNull(result);
        assertEquals(1, result.size());
        t.commit();
        s.close();


    }

    private void initData() throws Exception {
        Session s = openSession();
        Transaction t = s.beginTransaction();
        Componentizable componentizeable = getComponentizeable("hibernate", "ORM tool", "ORM tool1");
        s.saveOrUpdate(componentizeable);
        componentizeable = getComponentizeable("hibernate", "open source", "open source1");
        s.saveOrUpdate(componentizeable);
        componentizeable = getComponentizeable("hibernate", null, null);
        s.saveOrUpdate(componentizeable);
        t.commit();
        s.close();
    }

    private void deleteData() throws Exception {
    	Session s = openSession();
        Transaction t = s.beginTransaction();
		for ( Object entity : s.createQuery( "from Componentizable" ).list() ) {
			s.delete( entity );
		}
        t.commit();
        s.close();
    }

    private Componentizable getComponentizeable(String name, String subname, String subname1) {
        Componentizable componentizable = new Componentizable();
        if (name != null) {
            Component component = new Component();
            component.setName(name);
            if (subname != null || subname1 != null) {
                SubComponent subComponent = new SubComponent();
                subComponent.setSubName(subname);
                subComponent.setSubName1(subname1);
                component.setSubComponent(subComponent);
            }
            componentizable.setComponent(component);
        }
        return componentizable;
    }
}
