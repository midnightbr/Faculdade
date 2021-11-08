/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.onetomany.inverseToSuperclass;

import java.util.List;

import org.hibernate.envers.Audited;

@Audited
public class Root {

	private long id;

	private String str;

	private List<DetailSubclass> items;

	public Root() {

	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getStr() {
		return str;
	}

	public void setStr(String str) {
		this.str = str;
	}

	public List<DetailSubclass> getItems() {
		return items;
	}

	public void setItems(List<DetailSubclass> items) {
		this.items = items;
	}

}
