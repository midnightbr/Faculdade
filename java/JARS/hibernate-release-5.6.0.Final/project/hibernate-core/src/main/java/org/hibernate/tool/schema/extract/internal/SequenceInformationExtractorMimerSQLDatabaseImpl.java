/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.extract.internal;

/**
 * @author Vlad Mihalcea
 */
public class SequenceInformationExtractorMimerSQLDatabaseImpl extends SequenceInformationExtractorLegacyImpl {
	/**
	 * Singleton access
	 */
	public static final SequenceInformationExtractorMimerSQLDatabaseImpl INSTANCE = new SequenceInformationExtractorMimerSQLDatabaseImpl();

	@Override
	protected String sequenceStartValueColumn() {
		return "initial_value";
	}

	@Override
	protected String sequenceMinValueColumn() {
		return null;
	}
}
