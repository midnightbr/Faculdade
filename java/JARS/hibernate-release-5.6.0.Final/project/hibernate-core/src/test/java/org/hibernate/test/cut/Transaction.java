/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: Transaction.java 6234 2005-03-29 03:07:30Z oneovthafew $
package org.hibernate.test.cut;


/**
 * @author Gavin King
 */
public class Transaction {

	private Long id;
	private String description;
	private MonetoryAmount value;
	private CompositeDateTime timestamp;

	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
	public Long getId() {
		return id;
	}
	
	public void setId(Long id) {
		this.id = id;
	}
	
	public MonetoryAmount getValue() {
		return value;
	}
	
	public void setValue(MonetoryAmount value) {
		this.value = value;
	}

	public CompositeDateTime getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(CompositeDateTime timestamp) {
		this.timestamp = timestamp;
	}

}
