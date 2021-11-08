/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.sql.check;


/**
 * An entity which is expected to be mapped to each database using stored
 * procedures which throw exceptions on their own; in other words, using
 * {@link org.hibernate.engine.spi.ExecuteUpdateResultCheckStyle#NONE}.
 *
 * @author Steve Ebersole
 */
public class ExceptionCheckingEntity {
	private Long id;
	private String name;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}

