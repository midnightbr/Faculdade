/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast.tree;

import java.sql.Types;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.QueryException;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.type.CompositeType;
import org.hibernate.type.Type;

import antlr.collections.AST;

/**
 * Represents an entity referenced in the INTO clause of an HQL
 * INSERT statement.
 *
 * @author Steve Ebersole
 */
public class IntoClause extends HqlSqlWalkerNode implements DisplayableNode {

	private Queryable persister;
	private String columnSpec = "";
	private Type[] types;

	private boolean discriminated;
	private boolean explicitIdInsertion;
	private boolean explicitVersionInsertion;

	private Set componentIds;
	private List explicitComponentIds;

	public void initialize(Queryable persister) {
		if ( persister.isAbstract() ) {
			throw new QueryException( "cannot insert into abstract class (no table)" );
		}
		this.persister = persister;
		initializeColumns();

		if ( getWalker().getSessionFactoryHelper().hasPhysicalDiscriminatorColumn( persister ) ) {
			discriminated = true;
			columnSpec += ", " + persister.getDiscriminatorColumnName();
		}

		resetText();
	}

	private void resetText() {
		setText( "into " + getTableName() + " ( " + columnSpec + " )" );
	}

	public String getTableName() {
		return persister.getSubclassTableName( 0 );
	}

	public Queryable getQueryable() {
		return persister;
	}

	public String getEntityName() {
		return persister.getEntityName();
	}

	public Type[] getInsertionTypes() {
		return types;
	}

	public boolean isDiscriminated() {
		return discriminated;
	}

	public boolean isExplicitIdInsertion() {
		return explicitIdInsertion;
	}

	public boolean isExplicitVersionInsertion() {
		return explicitVersionInsertion;
	}

	public void prependIdColumnSpec() {
		columnSpec = persister.getIdentifierColumnNames()[0] + ", " + columnSpec;
		resetText();
	}

	public void prependVersionColumnSpec() {
		columnSpec = persister.getPropertyColumnNames( persister.getVersionProperty() )[0] + ", " + columnSpec;
		resetText();
	}

	public void validateTypes(SelectClause selectClause) throws QueryException {
		Type[] selectTypes = selectClause.getQueryReturnTypes();
		if ( selectTypes.length + selectClause.getTotalParameterCount() != types.length ) {
			throw new QueryException( "number of select types did not match those for insert" );
		}

		int parameterCount = 0;
		for ( int i = 0; i < types.length; i++ ) {
			if ( selectClause.getParameterPositions().contains( i ) ) {
				parameterCount++;
			}
			else if ( !areCompatible( types[i], selectTypes[i - parameterCount] ) ) {
				throw new QueryException(
						"insertion type [" + types[i] + "] and selection type [" +
								selectTypes[i - parameterCount] + "] at position " + i + " are not compatible"
				);
			}
		}

		// otherwise, everything ok.
	}

	/**
	 * Returns additional display text for the AST node.
	 *
	 * @return String - The additional display text.
	 */
	public String getDisplayText() {
		return "IntoClause{"
				+ "entityName=" + getEntityName()
				+ ",tableName=" + getTableName()
				+ ",columns={" + columnSpec + "}"
				+ "}";
	}

	private void initializeColumns() {
		AST propertySpec = getFirstChild();
		List types = new ArrayList();
		visitPropertySpecNodes( propertySpec.getFirstChild(), types );
		this.types = ArrayHelper.toTypeArray( types );
		columnSpec = columnSpec.substring( 0, columnSpec.length() - 2 );
	}

	private void visitPropertySpecNodes(AST propertyNode, List types) {
		if ( propertyNode == null ) {
			return;
		}
		// TODO : we really need to be able to deal with component paths here also;
		// this is difficult because the hql-sql grammar expects all those node types
		// to be FromReferenceNodes.  One potential fix here would be to convert the
		// IntoClause to just use a FromClause/FromElement combo (as a child of the
		// InsertStatement) and move all this logic into the InsertStatement.  That's
		// probably the easiest approach (read: least amount of changes to the grammar
		// and code), but just doesn't feel right as then an insert would contain
		// 2 from-clauses
		String name = propertyNode.getText();
		if ( isSuperclassProperty( name ) ) {
			throw new QueryException( "INSERT statements cannot refer to superclass/joined properties [" + name + "]" );
		}

		if ( !explicitIdInsertion ) {
			if ( persister.getIdentifierType() instanceof CompositeType ) {
				if ( componentIds == null ) {
					String[] propertyNames = ( (CompositeType) persister.getIdentifierType() ).getPropertyNames();
					componentIds = new HashSet();
					for ( String propertyName : propertyNames ) {
						componentIds.add( propertyName );
					}
				}
				if ( componentIds.contains( name ) ) {
					if ( explicitComponentIds == null ) {
						explicitComponentIds = new ArrayList( componentIds.size() );
					}
					explicitComponentIds.add( name );
					explicitIdInsertion = explicitComponentIds.size() == componentIds.size();
				}
			}
			else if ( name.equals( persister.getIdentifierPropertyName() ) ) {
				explicitIdInsertion = true;
			}
		}

		if ( persister.isVersioned() ) {
			if ( name.equals( persister.getPropertyNames()[persister.getVersionProperty()] ) ) {
				explicitVersionInsertion = true;
			}
		}

		String[] columnNames = persister.toColumns( name );
		renderColumns( columnNames );
		types.add( persister.toType( name ) );

		// visit width-first, then depth
		visitPropertySpecNodes( propertyNode.getNextSibling(), types );
		visitPropertySpecNodes( propertyNode.getFirstChild(), types );
	}

	private void renderColumns(String[] columnNames) {
		for ( String columnName : columnNames ) {
			columnSpec += columnName + ", ";
		}
	}

	private boolean isSuperclassProperty(String propertyName) {
		// really there are two situations where it should be ok to allow the insertion
		// into properties defined on a superclass:
		//      1) union-subclass with an abstract root entity
		//      2) discriminator-subclass
		//
		// #1 is handled already because of the fact that
		// UnionSubclassPersister alreay always returns 0
		// for this call...
		//
		// we may want to disallow it for discriminator-subclass just for
		// consistency-sake (currently does not work anyway)...
		return persister.getSubclassPropertyTableNumber( propertyName ) != 0;
	}

	/**
	 * Determine whether the two types are "assignment compatible".
	 *
	 * @param target The type defined in the into-clause.
	 * @param source The type defined in the select clause.
	 *
	 * @return True if they are assignment compatible.
	 */
	private boolean areCompatible(Type target, Type source) {
		if ( target.equals( source ) ) {
			// if the types report logical equivalence, return true...
			return true;
		}

		// otherwise, doAfterTransactionCompletion a "deep equivalence" check...

		if ( !target.getReturnedClass().isAssignableFrom( source.getReturnedClass() ) ) {
			return false;
		}

		int[] targetDatatypes = target.sqlTypes( getSessionFactoryHelper().getFactory() );
		int[] sourceDatatypes = source.sqlTypes( getSessionFactoryHelper().getFactory() );

		if ( targetDatatypes.length != sourceDatatypes.length ) {
			return false;
		}

		for ( int i = 0; i < targetDatatypes.length; i++ ) {
			if ( !areSqlTypesCompatible( targetDatatypes[i], sourceDatatypes[i] ) ) {
				return false;
			}
		}

		return true;
	}

	private boolean areSqlTypesCompatible(int target, int source) {
		switch ( target ) {
			case Types.TIMESTAMP:
				return source == Types.DATE || source == Types.TIME || source == Types.TIMESTAMP;
			case Types.DATE:
				return source == Types.DATE || source == Types.TIMESTAMP;
			case Types.TIME:
				return source == Types.TIME || source == Types.TIMESTAMP;
			default:
				return target == source;
		}
	}
}
