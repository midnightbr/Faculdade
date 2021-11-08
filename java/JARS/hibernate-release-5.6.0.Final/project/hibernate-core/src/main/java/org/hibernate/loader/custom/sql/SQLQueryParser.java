/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.custom.sql;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.hibernate.QueryException;
import org.hibernate.engine.query.spi.ParameterParser;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.param.ParameterBinder;
import org.hibernate.persister.collection.SQLLoadableCollection;
import org.hibernate.persister.entity.SQLLoadable;

/**
 * @author Gavin King
 * @author Max Andersen
 * @author Steve Ebersole
 * @author Paul Benedict
 */
public class SQLQueryParser {
	private static final Pattern PREPARED_STATEMENT_PATTERN = Pattern.compile( "^\\{.*?\\}$" );
	private static final String HIBERNATE_PLACEHOLDER_PREFIX = "h-";
	private static final String DOMAIN_PLACEHOLDER = "h-domain";
	private static final String CATALOG_PLACEHOLDER = "h-catalog";
	private static final String SCHEMA_PLACEHOLDER = "h-schema";

	private final SessionFactoryImplementor factory;
	private final String originalQueryString;
	private final ParserContext context;

	private long aliasesFound;

	private List<ParameterBinder> paramValueBinders;

	interface ParserContext {
		boolean isEntityAlias(String aliasName);
		SQLLoadable getEntityPersisterByAlias(String alias);
		String getEntitySuffixByAlias(String alias);
		boolean isCollectionAlias(String aliasName);
		SQLLoadableCollection getCollectionPersisterByAlias(String alias);
		String getCollectionSuffixByAlias(String alias);
		Map getPropertyResultsMapByAlias(String alias);
	}

	public SQLQueryParser(String queryString, ParserContext context, SessionFactoryImplementor factory) {
		this.originalQueryString = queryString;
		this.context = context;
		this.factory = factory;
	}

	public List<ParameterBinder> getParameterValueBinders() {
		return paramValueBinders == null ? Collections.emptyList() : paramValueBinders;
	}

	public boolean queryHasAliases() {
		return aliasesFound>0;
	}

	protected String getOriginalQueryString() {
		return originalQueryString;
	}

	public String process() {
		String processedSql = substituteBrackets( originalQueryString );
		processedSql = substituteParams( processedSql );
		return processedSql;
	}

	// TODO: should "record" how many properties we have referred to - and if we
	//       don't get them all we throw an exception! Way better than trial and error ;)
	protected String substituteBrackets(String sqlQuery) throws QueryException {

		if ( PREPARED_STATEMENT_PATTERN.matcher( sqlQuery.trim() ).matches() ) {
			return sqlQuery;
		}

		StringBuilder result = new StringBuilder( sqlQuery.length() + 20 );
		int left, right;

		// replace {....} with corresponding column aliases
		for ( int curr = 0; curr < sqlQuery.length(); curr = right + 1 ) {
			if ( ( left = sqlQuery.indexOf( '{', curr ) ) < 0 ) {
				// No additional open braces found in the string, append the
				// rest of the string in its entirety and quit this loop
				result.append( sqlQuery.substring( curr ) );
				break;
			}

			// append everything up until the next encountered open brace
			result.append( sqlQuery.substring( curr, left ) );

			if ( ( right = sqlQuery.indexOf( '}', left + 1 ) ) < 0 ) {
				throw new QueryException( "Unmatched braces for alias path", sqlQuery );
			}

			final String aliasPath = sqlQuery.substring( left + 1, right );
			boolean isPlaceholder = aliasPath.startsWith( HIBERNATE_PLACEHOLDER_PREFIX );

			if ( isPlaceholder ) {
				// Domain replacement
				if ( DOMAIN_PLACEHOLDER.equals( aliasPath ) ) {
					final String catalogName = factory.getSettings().getDefaultCatalogName();
					if ( catalogName != null ) {
						result.append( catalogName );
						result.append( "." );
					}
					final String schemaName = factory.getSettings().getDefaultSchemaName();
					if ( schemaName != null ) {
						result.append( schemaName );
						result.append( "." );
					}
				}
				// Schema replacement
				else if ( SCHEMA_PLACEHOLDER.equals( aliasPath ) ) {
					final String schemaName = factory.getSettings().getDefaultSchemaName();
					if ( schemaName != null ) {
						result.append(schemaName);
						result.append(".");
					}
				}
				// Catalog replacement
				else if ( CATALOG_PLACEHOLDER.equals( aliasPath ) ) {
					final String catalogName = factory.getSettings().getDefaultCatalogName();
					if ( catalogName != null ) {
						result.append( catalogName );
						result.append( "." );
					}
				}
				else {
					throw new QueryException( "Unknown placeholder ", aliasPath );
				}
			}
			else if (context != null) {
				int firstDot = aliasPath.indexOf( '.' );
				if ( firstDot == -1 ) {
					if ( context.isEntityAlias( aliasPath ) ) {
						// it is a simple table alias {foo}
						result.append( aliasPath );
						aliasesFound++;
					}
					else {
						// passing through anything we do not know : to support jdbc escape sequences HB-898
						result.append( '{' ).append(aliasPath).append( '}' );
					}
				}
				else {
					final String aliasName = aliasPath.substring( 0, firstDot );
					if ( context.isCollectionAlias( aliasName ) ) {
						// The current alias is referencing the collection to be eagerly fetched
						String propertyName = aliasPath.substring( firstDot + 1 );
						result.append( resolveCollectionProperties( aliasName, propertyName ) );
						aliasesFound++;
					}
					else if ( context.isEntityAlias( aliasName ) ) {
						// it is a property reference {foo.bar}
						String propertyName = aliasPath.substring( firstDot + 1 );
						result.append( resolveProperties( aliasName, propertyName ) );
						aliasesFound++;
					}
					else {
						// passing through anything we do not know : to support jdbc escape sequences HB-898
						result.append( '{' ).append(aliasPath).append( '}' );
					}
				}
			}
			else {
				result.append( '{' ).append(aliasPath).append( '}' );
			}
		}

		// Possibly handle :something parameters for the query ?

		return result.toString();
	}

	private String resolveCollectionProperties(
			String aliasName,
			String propertyName) {

		Map fieldResults = context.getPropertyResultsMapByAlias( aliasName );
		SQLLoadableCollection collectionPersister = context.getCollectionPersisterByAlias( aliasName );
		String collectionSuffix = context.getCollectionSuffixByAlias( aliasName );

		if ( "*".equals( propertyName ) ) {
			if( !fieldResults.isEmpty() ) {
				throw new QueryException("Using return-propertys together with * syntax is not supported.");
			}

			String selectFragment = collectionPersister.selectFragment( aliasName, collectionSuffix );
			aliasesFound++;
			return selectFragment
						+ ", "
						+ resolveProperties( aliasName, propertyName );
		}
		else if ( "element.*".equals( propertyName ) ) {
			return resolveProperties( aliasName, "*" );
		}
		else {
			String[] columnAliases;

			// Let return-properties override whatever the persister has for aliases.
			columnAliases = ( String[] ) fieldResults.get(propertyName);
			if ( columnAliases==null ) {
				columnAliases = collectionPersister.getCollectionPropertyColumnAliases( propertyName, collectionSuffix );
			}

			if ( columnAliases == null || columnAliases.length == 0 ) {
				throw new QueryException(
						"No column name found for property [" + propertyName + "] for alias [" + aliasName + "]",
						originalQueryString
				);
			}
			if ( columnAliases.length != 1 ) {
				// TODO: better error message since we actually support composites if names are explicitly listed.
				throw new QueryException(
						"SQL queries only support properties mapped to a single column - property [" +
						propertyName + "] is mapped to " + columnAliases.length + " columns.",
						originalQueryString
				);
			}
			aliasesFound++;
			return columnAliases[0];

		}
	}
	private String resolveProperties(String aliasName, String propertyName) {
		Map fieldResults = context.getPropertyResultsMapByAlias( aliasName );
		SQLLoadable persister = context.getEntityPersisterByAlias( aliasName );
		String suffix = context.getEntitySuffixByAlias( aliasName );

		if ( "*".equals( propertyName ) ) {
			if( !fieldResults.isEmpty() ) {
				throw new QueryException("Using return-propertys together with * syntax is not supported.");
			}
			aliasesFound++;
			return persister.selectFragment( aliasName, suffix ) ;
		}
		else {

			String[] columnAliases;

			// Let return-propertiess override whatever the persister has for aliases.
			columnAliases = (String[]) fieldResults.get( propertyName );
			if ( columnAliases == null ) {
				columnAliases = persister.getSubclassPropertyColumnAliases( propertyName, suffix );
			}

			if ( columnAliases == null || columnAliases.length == 0 ) {
				throw new QueryException(
						"No column name found for property [" + propertyName + "] for alias [" + aliasName + "]",
						originalQueryString
				);
			}
			if ( columnAliases.length != 1 ) {
				// TODO: better error message since we actually support composites if names are explicitly listed.
				throw new QueryException(
						"SQL queries only support properties mapped to a single column - property [" + propertyName + "] is mapped to " + columnAliases.length + " columns.",
						originalQueryString
				);
			}
			aliasesFound++;
			return columnAliases[0];
		}
	}

	/**
	 * Substitutes JDBC parameter placeholders (?) for all encountered
	 * parameter specifications.  It also tracks the positions of these
	 * parameter specifications within the query string.  This accounts for
	 * ordinal-params, named-params, and ejb3-positional-params.
	 *
	 * @param sqlString The query string.
	 * @return The SQL query with parameter substitution complete.
	 */
	private String substituteParams(String sqlString) {
		final ParameterSubstitutionRecognizer recognizer = new ParameterSubstitutionRecognizer( factory );
		ParameterParser.parse( sqlString, recognizer );

		paramValueBinders = recognizer.getParameterValueBinders();

		return recognizer.result.toString();
	}

	public static class ParameterSubstitutionRecognizer implements ParameterParser.Recognizer {
		StringBuilder result = new StringBuilder();

		int jdbcPositionalParamCount;
		private List<ParameterBinder> paramValueBinders;

		public ParameterSubstitutionRecognizer(SessionFactoryImplementor factory) {
			this.jdbcPositionalParamCount = factory.getSessionFactoryOptions().jdbcStyleParamsZeroBased()
					? 0
					: 1;
		}

		@Override
		public void outParameter(int position) {
			result.append( '?' );
		}

		@Override
		public void ordinalParameter(int position) {
			result.append( '?' );
			registerPositionParamBinder( jdbcPositionalParamCount++ );
		}

		private void registerPositionParamBinder(int label) {
			if ( paramValueBinders == null ) {
				paramValueBinders = new ArrayList<>();
			}

			paramValueBinders.add( new PositionalParamBinder( label ) );
		}

		@Override
		public void jpaPositionalParameter(int name, int position) {
			result.append( '?' );
			registerPositionParamBinder( name );
		}

		@Override
		public void namedParameter(String name, int position) {
			result.append( '?' );
			registerNamedParamBinder( name );
		}

		private void registerNamedParamBinder(String name) {
			if ( paramValueBinders == null ) {
				paramValueBinders = new ArrayList<>();
			}

			paramValueBinders.add( new NamedParamBinder( name ) );
		}

		@Override
		public void other(char character) {
			result.append( character );
		}

		public List<ParameterBinder> getParameterValueBinders() {
			return paramValueBinders;
		}

		@Override
		public void complete() {
		}
	}
}
