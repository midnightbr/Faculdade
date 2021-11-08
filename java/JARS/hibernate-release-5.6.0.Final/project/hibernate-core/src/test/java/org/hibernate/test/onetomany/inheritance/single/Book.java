/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.onetomany.inheritance.single;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;


@Entity
public class Book extends Product {

	private String isbn;
	
	@ManyToOne
	private Library library;
	
	public Book() {
		super();
	}
	
	public Book(String inventoryCode, String isbn) {
		super(inventoryCode);
		this.isbn = isbn;
	}
	
	public String getIsbn() {
		return isbn;
	}
	
	public Library getLibrary() {
		return library;
	}

	public void setLibrary(Library library) {
		this.library = library;
	}
}
