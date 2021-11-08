/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.extract.internal;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author Vlad Mihalcea
 */
public class SequenceInformationExtractorInformixDatabaseImpl extends SequenceInformationExtractorLegacyImpl {
	/**
	 * Singleton access
	 */
	public static final SequenceInformationExtractorInformixDatabaseImpl INSTANCE = new SequenceInformationExtractorInformixDatabaseImpl();

	@Override
	protected String sequenceCatalogColumn() {
		return null;
	}

	@Override
	protected String sequenceSchemaColumn() {
		return null;
	}

	@Override
	protected String sequenceStartValueColumn() {
		return "start_val";
	}

	@Override
	protected String sequenceMinValueColumn() {
		return "min_val";
	}

	@Override
	protected String sequenceMaxValueColumn() {
		return "max_val";
	}

	@Override
	protected String sequenceIncrementColumn() {
		return "inc_val";
	}
}
