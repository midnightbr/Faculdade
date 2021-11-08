/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.associations;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import org.jboss.logging.Logger;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Vlad Mihalcea
 */
public class ManyToManyUnidirectionalTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Person.class,
				Address.class,
		};
	}

	@Test
	public void testLifecycle() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::associations-many-to-many-unidirectional-lifecycle-example[]
			Person person1 = new Person();
			Person person2 = new Person();

			Address address1 = new Address( "12th Avenue", "12A" );
			Address address2 = new Address( "18th Avenue", "18B" );

			person1.getAddresses().add( address1 );
			person1.getAddresses().add( address2 );

			person2.getAddresses().add( address1 );

			entityManager.persist( person1 );
			entityManager.persist( person2 );

			entityManager.flush();

			person1.getAddresses().remove( address1 );
			//end::associations-many-to-many-unidirectional-lifecycle-example[]
		} );
	}

	@Test
	public void testRemove() {
		final Long personId = doInJPA( this::entityManagerFactory, entityManager -> {
			Person person1 = new Person();
			Person person2 = new Person();

			Address address1 = new Address( "12th Avenue", "12A" );
			Address address2 = new Address( "18th Avenue", "18B" );

			person1.getAddresses().add( address1 );
			person1.getAddresses().add( address2 );

			person2.getAddresses().add( address1 );

			entityManager.persist( person1 );
			entityManager.persist( person2 );

			return person1.id;
		} );
		doInJPA( this::entityManagerFactory, entityManager -> {
			log.info( "Remove" );
			//tag::associations-many-to-many-unidirectional-remove-example[]
			Person person1 = entityManager.find( Person.class, personId );
			entityManager.remove( person1 );
			//end::associations-many-to-many-unidirectional-remove-example[]
		} );
	}

	//tag::associations-many-to-many-unidirectional-example[]
	@Entity(name = "Person")
	public static class Person {

		@Id
		@GeneratedValue
		private Long id;

		@ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
		private List<Address> addresses = new ArrayList<>();

		//Getters and setters are omitted for brevity

	//end::associations-many-to-many-unidirectional-example[]

		public Person() {
		}

		public List<Address> getAddresses() {
			return addresses;
		}
	//tag::associations-many-to-many-unidirectional-example[]
	}

	@Entity(name = "Address")
	public static class Address {

		@Id
		@GeneratedValue
		private Long id;

		private String street;

		@Column(name = "`number`")
		private String number;

		//Getters and setters are omitted for brevity

	//end::associations-many-to-many-unidirectional-example[]

		public Address() {
		}

		public Address(String street, String number) {
			this.street = street;
			this.number = number;
		}

		public Long getId() {
			return id;
		}

		public String getStreet() {
			return street;
		}

		public String getNumber() {
			return number;
		}
	//tag::associations-many-to-many-unidirectional-example[]
	}
	//end::associations-many-to-many-unidirectional-example[]
}
