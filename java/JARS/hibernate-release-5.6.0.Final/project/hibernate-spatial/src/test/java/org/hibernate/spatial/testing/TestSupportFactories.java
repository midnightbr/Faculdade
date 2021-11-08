/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.testing;

import org.hibernate.dialect.CockroachDB192Dialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.PostgreSQL82Dialect;
import org.hibernate.spatial.SpatialDialect;
import org.hibernate.spatial.testing.dialects.cockroachdb.CockroachDBTestSupport;
import org.hibernate.spatial.testing.dialects.db2.DB2TestSupport;
import org.hibernate.spatial.testing.dialects.h2geodb.GeoDBTestSupport;
import org.hibernate.spatial.testing.dialects.hana.HANATestSupport;
import org.hibernate.spatial.testing.dialects.mariadb.MariaDBTestSupport;
import org.hibernate.spatial.testing.dialects.mysql.MySQL56TestSupport;
import org.hibernate.spatial.testing.dialects.mysql.MySQL8TestSupport;
import org.hibernate.spatial.testing.dialects.mysql.MySQLTestSupport;
import org.hibernate.spatial.testing.dialects.oracle.OracleSDOTestSupport;
import org.hibernate.spatial.testing.dialects.postgis.PostgisTestSupport;
import org.hibernate.spatial.testing.dialects.sqlserver.SQLServerTestSupport;


/**
 * @author Karel Maesen, Geovise BVBA
 * creation-date: Sep 30, 2010
 */
public class TestSupportFactories {

	private static final TestSupportFactories instance = new TestSupportFactories();

	private TestSupportFactories() {
	}

	public static TestSupportFactories instance() {
		return instance;
	}

	private static Class<? extends TestSupport> getSupportFactoryClass(Dialect dialect) {
		String canonicalName = dialect.getClass().getCanonicalName();
		if ( ( dialect instanceof SpatialDialect ) && PostgreSQL82Dialect.class.isAssignableFrom( dialect.getClass() ) ) {
			//this test works because all postgis dialects ultimately derive of the Postgresql82Dialect
			return PostgisTestSupport.class;
		}

		if ( ( dialect instanceof SpatialDialect ) && MariaDBDialect.class.isAssignableFrom( dialect.getClass() ) ) {
			return MariaDBTestSupport.class;
		}

		if ( ( dialect instanceof SpatialDialect ) && CockroachDB192Dialect.class.isAssignableFrom( dialect.getClass() ) ) {
			return CockroachDBTestSupport.class;
		}

		if ( "org.hibernate.spatial.dialect.h2geodb.GeoDBDialect".equals( canonicalName ) ) {
			return GeoDBTestSupport.class;
		}
		if ( "org.hibernate.spatial.dialect.sqlserver.SqlServer2008SpatialDialect".equals( canonicalName ) ) {
			return SQLServerTestSupport.class;
		}
		if ( "org.hibernate.spatial.dialect.sqlserver.SqlServer2012SpatialDialect".equals( canonicalName ) ) {
			return SQLServerTestSupport.class;
		}
		if ( "org.hibernate.spatial.dialect.mysql.MySQLSpatialDialect".equals( canonicalName ) ||
				"org.hibernate.spatial.dialect.mysql.MySQL5InnoDBSpatialDialect".equals( canonicalName ) ) {
			return MySQLTestSupport.class;
		}

		if ( "org.hibernate.spatial.dialect.mysql.MySQL8SpatialDialect".equals( canonicalName ) ) {
			return MySQL8TestSupport.class;
		}

		if ( "org.hibernate.spatial.dialect.mysql.MySQL56SpatialDialect".equals( canonicalName ) ||
				"org.hibernate.spatial.dialect.mysql.MySQL56InnoDBSpatialDialect".equals( canonicalName ) ) {
			return MySQL56TestSupport.class;
		}
		if ( "org.hibernate.spatial.dialect.oracle.OracleSpatial10gDialect".equals( canonicalName ) ||
				"org.hibernate.spatial.dialect.oracle.OracleSpatialSDO10gDialect".equals( canonicalName ) ) {
			return OracleSDOTestSupport.class;
		}
		if ( "org.hibernate.spatial.dialect.hana.HANASpatialDialect".equals( canonicalName ) ) {
			return HANATestSupport.class;
		}

		if ( "org.hibernate.spatial.dialect.db2.DB2SpatialDialect".equals( canonicalName ) ) {
			return DB2TestSupport.class;
		}
		throw new IllegalArgumentException( "Dialect not known in test suite" );
	}

	public TestSupport getTestSupportFactory(Dialect dialect) throws InstantiationException, IllegalAccessException {
		if ( dialect == null ) {
			throw new IllegalArgumentException( "Dialect argument is required." );
		}
		Class testSupportFactoryClass = getSupportFactoryClass( dialect );
		return instantiate( testSupportFactoryClass );

	}

	private TestSupport instantiate(Class<? extends TestSupport> testSupportFactoryClass)
			throws IllegalAccessException, InstantiationException {
		return testSupportFactoryClass.newInstance();
	}

	private ClassLoader getClassLoader() {
		return this.getClass().getClassLoader();
	}

}
