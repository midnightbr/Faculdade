/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test;

import java.util.Arrays;
import java.util.List;

import org.hibernate.testing.cleaner.DatabaseCleaner;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import org.jboss.logging.Logger;

/**
 * An abstract Envers test which runs the tests using two audit strategies.
 *
 * @author Adam Warski (adam at warski dot org)
 */
@RunWith(EnversRunner.class)
public abstract class AbstractEnversTest {

	static {
		DatabaseCleaner.clearSchemas();
	}

	protected final Logger log = Logger.getLogger( getClass() );

	private String auditStrategy;

	@Parameterized.Parameters
	public static List<Object[]> data() {
		return Arrays.asList(
				new Object[] {null},
				new Object[] {"org.hibernate.envers.strategy.ValidityAuditStrategy"}
		);
	}

	public void setTestData(Object[] data) {
		auditStrategy = (String) data[0];
	}

	public String getAuditStrategy() {
		return auditStrategy;
	}
}
