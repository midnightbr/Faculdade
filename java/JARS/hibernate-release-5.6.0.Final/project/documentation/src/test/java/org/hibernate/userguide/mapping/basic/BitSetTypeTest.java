/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.mapping.basic;

import java.util.BitSet;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.annotations.Type;
import org.hibernate.cfg.Configuration;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class BitSetTypeTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Product.class
		};
	}

	@Override
	protected void configure(Configuration configuration) {
		super.configure( configuration );
		//tag::basic-custom-type-register-BasicType-example[]
		configuration.registerTypeContributor( (typeContributions, serviceRegistry) -> {
			typeContributions.contributeType( BitSetType.INSTANCE );
		} );
		//end::basic-custom-type-register-BasicType-example[]
	}

	@Test
	public void test() {

		//tag::basic-custom-type-BitSetType-persistence-example[]
		BitSet bitSet = BitSet.valueOf( new long[] {1, 2, 3} );

		doInHibernate( this::sessionFactory, session -> {
			Product product = new Product( );
			product.setId( 1 );
			product.setBitSet( bitSet );
			session.persist( product );
		} );

		doInHibernate( this::sessionFactory, session -> {
			Product product = session.get( Product.class, 1 );
			assertEquals(bitSet, product.getBitSet());
		} );
		//end::basic-custom-type-BitSetType-persistence-example[]
	}

	//tag::basic-custom-type-BitSetType-mapping-example[]
	@Entity(name = "Product")
	public static class Product {

		@Id
		private Integer id;

		@Type( type = "bitset" )
		private BitSet bitSet;

		public Integer getId() {
			return id;
		}

		//Getters and setters are omitted for brevity
	//end::basic-custom-type-BitSetType-mapping-example[]

		public void setId(Integer id) {
			this.id = id;
		}

		public BitSet getBitSet() {
			return bitSet;
		}

		public void setBitSet(BitSet bitSet) {
			this.bitSet = bitSet;
		}
	//tag::basic-custom-type-BitSetType-mapping-example[]
	}
	//end::basic-custom-type-BitSetType-mapping-example[]
}
