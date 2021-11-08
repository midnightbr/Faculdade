/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.testing.dialects.sqlserver;

import org.hibernate.spatial.testing.SQLExpressionTemplate;
import org.hibernate.spatial.testing.TestDataElement;
import org.hibernate.spatial.testing.WktUtility;

/**
 * @author Karel Maesen, Geovise BVBA
 */
public class SQLServerExpressionTemplate implements SQLExpressionTemplate {

	static final String SQL_TEMPLATE = "insert into geomtest (id, type, geom) values (%d, '%s', Geometry::STGeomFromText('%s', %d))";

	public String toInsertSql(TestDataElement testDataElement) {
		int srid = WktUtility.getSRID( testDataElement.wkt );
		String wkt = WktUtility.getWkt( testDataElement.wkt );
		return String.format(
				SQL_TEMPLATE,
				testDataElement.id,
				testDataElement.type,
				wkt,
				srid
		);
	}


}
