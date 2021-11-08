/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.result.spi;

import java.util.Set;

import org.hibernate.engine.query.spi.sql.NativeSQLQueryReturn;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * @author Steve Ebersole
 */
public interface ResultContext {
	SharedSessionContractImplementor getSession();
	Set<String> getSynchronizedQuerySpaces();

	// for now...
	// see Loader-redesign proposal
	String getSql();
	QueryParameters getQueryParameters();
	NativeSQLQueryReturn[] getQueryReturns();
}
