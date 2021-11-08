/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.dialect.oracle;

import java.util.List;

import org.hibernate.QueryException;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.type.Type;

/**
 * Special function for accessing a member variable of an Oracle Object
 *
 * @author Karel Maesen
 */
class SDOObjectProperty implements SQLFunction {

	private final Type type;

	private final String name;

	public SDOObjectProperty(String name, Type type) {
		this.type = type;
		this.name = name;
	}

	public Type getReturnType(Type columnType, Mapping mapping)
			throws QueryException {
		return type == null ? columnType : type;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.hibernate.dialect.function.SQLFunction#hasArguments()
	 */

	public boolean hasArguments() {
		return true;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.hibernate.dialect.function.SQLFunction#hasParenthesesIfNoArguments()
	 */

	public boolean hasParenthesesIfNoArguments() {
		return false;
	}

	public String getName() {
		return this.name;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.hibernate.dialect.function.SQLFunction#render(java.util.List,
	 *      org.hibernate.engine.SessionFactoryImplementor)
	 */

	public String render(Type firstArgtype, List args, SessionFactoryImplementor factory)
			throws QueryException {
		final StringBuilder buf = new StringBuilder();
		if ( args.isEmpty() ) {
			throw new QueryException(
					"First Argument in arglist must be object of which property is queried"
			);
		}
		buf.append( args.get( 0 ) ).append( "." ).append( name );
		return buf.toString();
	}

}
