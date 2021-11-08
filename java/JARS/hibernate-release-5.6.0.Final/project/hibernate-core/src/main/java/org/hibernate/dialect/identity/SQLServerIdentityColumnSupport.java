/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.identity;

/**
 * @author Andrea Boriero
 */
public class SQLServerIdentityColumnSupport extends AbstractTransactSQLIdentityColumnSupport {
	/**
	 * Use <tt>insert table(...) values(...) select SCOPE_IDENTITY()</tt>
	 * <p/>
	 * {@inheritDoc}
	 */
	@Override
	public String appendIdentitySelectToInsert(String insertSQL) {
		return insertSQL + " select scope_identity()";
	}
}
