/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;
import java.util.LinkedHashMap;

/**
 * A specialization of the map type, with (resultset-based) ordering.
 */
public class OrderedMapType extends MapType {

	/**
	 * @deprecated Use the other constructor
	 */
	@Deprecated
	public OrderedMapType(TypeFactory.TypeScope typeScope, String role, String propertyRef) {
		this( role, propertyRef );
	}

	public OrderedMapType(String role, String propertyRef) {
		super( role, propertyRef );
	}

	@Override
	public Object instantiate(int anticipatedSize) {
		return anticipatedSize > 0
				? new LinkedHashMap( anticipatedSize )
				: new LinkedHashMap();
	}

}
