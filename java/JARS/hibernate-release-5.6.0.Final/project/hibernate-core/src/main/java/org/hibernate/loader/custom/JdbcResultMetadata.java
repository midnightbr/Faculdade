/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.custom;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.type.Type;

/**
 * Simplified access to JDBC ResultSetMetaData
 *
 * @author Steve Ebersole
 */
class JdbcResultMetadata {
	private final SessionFactoryImplementor factory;
	private final ResultSet resultSet;
	private final ResultSetMetaData resultSetMetaData;

	public JdbcResultMetadata(SessionFactoryImplementor factory, ResultSet resultSet) throws HibernateException {
		try {
			this.factory = factory;
			this.resultSet = resultSet;
			this.resultSetMetaData = resultSet.getMetaData();
		}
		catch( SQLException e ) {
			throw new HibernateException( "Could not extract result set metadata", e );
		}
	}

	public int getColumnCount() throws HibernateException {
		try {
			return resultSetMetaData.getColumnCount();
		}
		catch( SQLException e ) {
			throw new HibernateException( "Could not determine result set column count", e );
		}
	}

	public int resolveColumnPosition(String columnName) throws HibernateException {
		try {
			return resultSet.findColumn( columnName );
		}
		catch( SQLException e ) {
			throw new HibernateException( "Could not resolve column name in result set [" + columnName + "]", e );
		}
	}

	public String getColumnName(int position) throws HibernateException {
		try {
			return factory.getDialect().getColumnAliasExtractor().extractColumnAlias( resultSetMetaData, position );
		}
		catch( SQLException e ) {
			throw new HibernateException( "Could not resolve column name [" + position + "]", e );
		}
	}

	public Type getHibernateType(int columnPos) throws SQLException {
		int columnType = resultSetMetaData.getColumnType( columnPos );
		int scale = resultSetMetaData.getScale( columnPos );
		int precision = resultSetMetaData.getPrecision( columnPos );

		int length = precision;
		if ( columnType == Types.CHAR && precision == 0 ) {
			length = resultSetMetaData.getColumnDisplaySize( columnPos );
		}

		String hibernateTypeName;

		//Get the contributed Hibernate Type first
		Set<String> hibernateTypeNames = factory.getMetamodel()
				.getTypeConfiguration()
				.getJdbcToHibernateTypeContributionMap()
				.get( columnType );

		//If the user has not supplied any JDBC Type to Hibernate Type mapping, use the Dialect-based mapping
		if ( hibernateTypeNames != null && !hibernateTypeNames.isEmpty() ) {
			if ( hibernateTypeNames.size() > 1 ) {
				throw new HibernateException(
						String.format(
								"There are multiple Hibernate types: [%s] registered for the [%d] JDBC type code",
								String.join( ", ", hibernateTypeNames ),
								columnType
						) );
			}
			else {
				hibernateTypeName = hibernateTypeNames.iterator().next();
			}
		}
		else {
			hibernateTypeName = factory.getDialect().getHibernateTypeName(
					columnType,
					length,
					precision,
					scale
			);
		}

		return factory.getTypeResolver().heuristicType(
				hibernateTypeName
		);
	}
}
