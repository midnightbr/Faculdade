/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.collections;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Vlad Mihalcea
 */
public class UnidirectionalOrderedByListTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Person.class,
				Phone.class,
		};
	}

	@Test
	public void testLifecycle() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Person person = new Person( 1L );
			person.getPhones().add( new Phone( 1L, "landline", "028-234-9876" ) );
			person.getPhones().add( new Phone( 2L, "mobile", "072-122-9876" ) );
			entityManager.persist( person );
		} );
		doInJPA( this::entityManagerFactory, entityManager -> {
			Person person = entityManager.find( Person.class, 1L );
			person.getPhones().remove( 0 );
		} );
	}

	//tag::collections-unidirectional-ordered-list-order-by-example[]
	@Entity(name = "Person")
	public static class Person {

		@Id
		private Long id;

		@OneToMany(cascade = CascadeType.ALL)
		@OrderBy("number")
		private List<Phone> phones = new ArrayList<>();

		//Getters and setters are omitted for brevity

	//end::collections-unidirectional-ordered-list-order-by-example[]

		public Person() {
		}

		public Person(Long id) {
			this.id = id;
		}

		public List<Phone> getPhones() {
			return phones;
		}
	//tag::collections-unidirectional-ordered-list-order-by-example[]
	}

	@Entity(name = "Phone")
	public static class Phone {

		@Id
		private Long id;

		private String type;

		@Column(name = "`number`")
		private String number;

		//Getters and setters are omitted for brevity

	//end::collections-unidirectional-ordered-list-order-by-example[]

		public Phone() {
		}

		public Phone(Long id, String type, String number) {
			this.id = id;
			this.type = type;
			this.number = number;
		}

		public Long getId() {
			return id;
		}

		public String getType() {
			return type;
		}

		public String getNumber() {
			return number;
		}
	//tag::collections-unidirectional-ordered-list-order-by-example[]
	}
	//end::collections-unidirectional-ordered-list-order-by-example[]
}
