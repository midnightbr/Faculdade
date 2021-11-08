/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.associations.any;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class AnyTest extends BaseCoreFunctionalTestCase {

    @Override
    protected Class<?>[] getAnnotatedClasses() {
        return new Class<?>[] {
            IntegerProperty.class,
            StringProperty.class,
            PropertyHolder.class
        };
    }

    @Override
    protected String[] getAnnotatedPackages() {
        return new String[] {
            getClass().getPackage().getName()
        };
    }

    @Test
    public void test() {

        doInHibernate( this::sessionFactory, session -> {
            //tag::associations-any-persist-example[]
            IntegerProperty ageProperty = new IntegerProperty();
            ageProperty.setId( 1L );
            ageProperty.setName( "age" );
            ageProperty.setValue( 23 );

            session.persist( ageProperty );

            StringProperty nameProperty = new StringProperty();
            nameProperty.setId( 1L );
            nameProperty.setName( "name" );
            nameProperty.setValue( "John Doe" );

            session.persist( nameProperty );

            PropertyHolder namePropertyHolder = new PropertyHolder();
            namePropertyHolder.setId( 1L );
            namePropertyHolder.setProperty( nameProperty );

            session.persist( namePropertyHolder );
            //end::associations-any-persist-example[]
        } );

        doInHibernate( this::sessionFactory, session -> {
            //tag::associations-any-query-example[]
            PropertyHolder propertyHolder = session.get( PropertyHolder.class, 1L );

            assertEquals("name", propertyHolder.getProperty().getName());
            assertEquals("John Doe", propertyHolder.getProperty().getValue());
            //end::associations-any-query-example[]
        } );
    }


}
