/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast.tree;

import java.util.List;

import org.hibernate.QueryException;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.hql.internal.antlr.HqlSqlTokenTypes;
import org.hibernate.hql.internal.antlr.SqlTokenTypes;
import org.hibernate.hql.internal.ast.util.ColumnHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.sql.JoinType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.Type;

import antlr.SemanticException;
import antlr.collections.AST;

/**
 * Represents an identifier all by itself, which may be a function name,
 * a class alias, or a form of naked property-ref depending on the
 * context.
 *
 * @author josh
 */
public class IdentNode extends FromReferenceNode implements SelectExpression {
	private static enum DereferenceType {
		UNKNOWN,
		PROPERTY_REF,
		COMPONENT_REF
	}

	private boolean nakedPropertyRef;
	private String[] columns;
	
	public String[] getColumns() {
		return columns;
	}

	public void resolveIndex(AST parent) throws SemanticException {
		// An ident node can represent an index expression if the ident
		// represents a naked property ref
		//      *Note*: this makes the assumption (which is currently the case
		//      in the hql-sql grammar) that the ident is first resolved
		//      itself (addrExpr -> resolve()).  The other option, if that
		//      changes, is to call resolve from here; but it is
		//      currently un-needed overhead.
		if (!(isResolved() && nakedPropertyRef)) {
			throw new UnsupportedOperationException();
		}

		String propertyName = getOriginalText();
		if (!getDataType().isCollectionType()) {
			throw new SemanticException("Collection expected; [" + propertyName + "] does not refer to a collection property");
		}

		// TODO : most of below was taken verbatim from DotNode; should either delegate this logic or super-type it
		CollectionType type = (CollectionType) getDataType();
		String role = type.getRole();
		QueryableCollection queryableCollection = getSessionFactoryHelper().requireQueryableCollection(role);

		String alias = null;  // DotNode uses null here...
		String columnTableAlias = getFromElement().getTableAlias();
		JoinType joinType = JoinType.INNER_JOIN;
		boolean fetch = false;

		FromElementFactory factory = new FromElementFactory(
				getWalker().getCurrentFromClause(),
				getFromElement(),
				propertyName,
				alias,
				getFromElement().toColumns(columnTableAlias, propertyName, false),
				true
		);
		FromElement elem = factory.createCollection(queryableCollection, role, joinType, fetch, true);
		setFromElement(elem);
		getWalker().addQuerySpaces(queryableCollection.getCollectionSpaces());	// Always add the collection's query spaces.
	}

	protected String[] resolveColumns(QueryableCollection collectionPersister) {
		final FromElement fromElement = getFromElement();
		return fromElement.toColumns(
				fromElement.getCollectionTableAlias(),
				"elements", // the JPA VALUE "qualifier" is the same concept as the HQL ELEMENTS function/property
				getWalker().isInSelect()
		);
	}
	
	private void initText(String[] columns) {
		String text = String.join( ", ", columns );
		if ( columns.length > 1 && getWalker().isComparativeExpressionClause() ) {
			text = "(" + text + ")";
		}
		setText( text );
	}
	
	public void resolve(boolean generateJoin, boolean implicitJoin, String classAlias, AST parent, AST parentPredicate) {
		if (!isResolved()) {
			if ( getWalker().getCurrentFromClause().isFromElementAlias( getText() ) ) {
				FromElement fromElement = getWalker().getCurrentFromClause().getFromElement( getText() );
				if ( fromElement.getQueryableCollection() != null && fromElement.getQueryableCollection().getElementType().isComponentType() ) {
					if ( getWalker().isInSelect() ) {
						// This is a reference to an element collection
						setFromElement( fromElement );
						super.setDataType( fromElement.getQueryableCollection().getElementType() );
						this.columns = resolveColumns( fromElement.getQueryableCollection() );
						initText( getColumns() );
						setFirstChild( null );
						// Don't resolve it
					}
					else {
						resolveAsAlias();
						// Don't resolve it
					}
				}
				else if ( resolveAsAlias() ) {
					setResolved();
					// We represent a from-clause alias
				}
			}
			else if (
					getColumns() != null
					&& ( getWalker().getAST() instanceof AbstractMapComponentNode || getWalker().getAST() instanceof IndexNode )
					&& getWalker().getCurrentFromClause().isFromElementAlias( getOriginalText() )
					) {
				// We might have to revert our decision that this is naked element collection reference when we encounter it is embedded in a map function
				setText( getOriginalText() );
				if ( resolveAsAlias() ) {
					setResolved();
				}
			}
			else if (parent != null && parent.getType() == SqlTokenTypes.DOT) {
				DotNode dot = (DotNode) parent;
				if (parent.getFirstChild() == this) {
					if (resolveAsNakedComponentPropertyRefLHS(dot)) {
						// we are the LHS of the DOT representing a naked comp-prop-ref
						setResolved();
					}
				}
				else {
					if (resolveAsNakedComponentPropertyRefRHS(dot)) {
						// we are the RHS of the DOT representing a naked comp-prop-ref
						setResolved();
					}
				}
			}
			else {
				DereferenceType result = resolveAsNakedPropertyRef();
				if (result == DereferenceType.PROPERTY_REF) {
					// we represent a naked (simple) prop-ref
					setResolved();
				}
				else if (result == DereferenceType.COMPONENT_REF) {
					// EARLY EXIT!!!  return so the resolve call explicitly coming from DotNode can
					// resolve this...
					return;
				}
			}

			// if we are still not resolved, we might represent a constant.
			//      needed to add this here because the allowance of
			//      naked-prop-refs in the grammar collides with the
			//      definition of literals/constants ("nondeterminism").
			//      TODO: cleanup the grammar so that "processConstants" is always just handled from here
			if (!isResolved()) {
				try {
					getWalker().getLiteralProcessor().processConstant(this, false);
				}
				catch (Throwable ignore) {
					// just ignore it for now, it'll get resolved later...
				}
			}
		}
	}

	private boolean resolveAsAlias() {
		final String alias = getText();

		// This is not actually a constant, but a reference to FROM element.
		final FromElement element = getWalker().getCurrentFromClause().getFromElement( alias );
		if ( element == null ) {
			return false;
		}

		element.applyTreatAsDeclarations( getWalker().getTreatAsDeclarationsByPath( alias ) );

		setType( SqlTokenTypes.ALIAS_REF );
		setFromElement( element );

		String[] columnExpressions = element.getIdentityColumns();

		final Dialect dialect = getWalker().getSessionFactoryHelper().getFactory().getDialect();
		final boolean isInCount = getWalker().isInCount();
		final boolean isInDistinctCount = isInCount && getWalker().isInCountDistinct();
		final boolean isInNonDistinctCount = isInCount && ! getWalker().isInCountDistinct();
		final boolean isCompositeValue = columnExpressions.length > 1;
		if ( isCompositeValue ) {
			if ( isInNonDistinctCount && ! dialect.supportsTupleCounts() ) {
				// TODO: #supportsTupleCounts currently false for all Dialects -- could this be cleaned up?
				setText( columnExpressions[0] );
			}
			else {
				String joinedFragment = String.join( ", ", columnExpressions );
				// avoid wrapping in parenthesis (explicit tuple treatment) if possible due to varied support for
				// tuple syntax across databases..
				final boolean shouldSkipWrappingInParenthesis =
						(isInDistinctCount && ! dialect.requiresParensForTupleDistinctCounts())
						|| isInNonDistinctCount
						|| getWalker().isInSelect() && !getWalker().isInCase() && !isInCount && dialect.supportsTuplesInSubqueries() // HHH-14156
						|| getWalker().getCurrentTopLevelClauseType() == HqlSqlTokenTypes.ORDER
						|| getWalker().getCurrentTopLevelClauseType() == HqlSqlTokenTypes.GROUP;
				if ( ! shouldSkipWrappingInParenthesis ) {
					joinedFragment = "(" + joinedFragment + ")";
				}
				setText( joinedFragment );
			}
			return true;
		}
		else if ( columnExpressions.length > 0 ) {
			setText( columnExpressions[0] );
			return true;
		}

		return false;
	}

	private Type getNakedPropertyType(FromElement fromElement) {
		if (fromElement == null) {
			return null;
		}
		String property = getOriginalText();
		Type propertyType = null;
		try {
			propertyType = fromElement.getPropertyType(property, property);
		}
		catch (Throwable ignore) {
		}
		return propertyType;
	}

	private DereferenceType resolveAsNakedPropertyRef() {
		FromElement fromElement = locateSingleFromElement();
		if (fromElement == null) {
			return DereferenceType.UNKNOWN;
		}
		Queryable persister = fromElement.getQueryable();
		if (persister == null) {
			return DereferenceType.UNKNOWN;
		}
		Type propertyType = getNakedPropertyType(fromElement);
		if (propertyType == null) {
			// assume this ident's text does *not* refer to a property on the given persister
			return DereferenceType.UNKNOWN;
		}

		if ((propertyType.isComponentType() || propertyType.isAssociationType() )) {
			return DereferenceType.COMPONENT_REF;
		}

		setFromElement(fromElement);
		String property = getText();
		String[] columns = getWalker().isSelectStatement()
				? persister.toColumns(fromElement.getTableAlias(), property)
				: persister.toColumns(property);
		String text = String.join(", ", columns);
		setText(columns.length == 1 ? text : "(" + text + ")");
		setType(SqlTokenTypes.SQL_TOKEN);

		// these pieces are needed for usage in select clause
		super.setDataType(propertyType);
		nakedPropertyRef = true;

		return DereferenceType.PROPERTY_REF;
	}

	private boolean resolveAsNakedComponentPropertyRefLHS(DotNode parent) {
		FromElement fromElement = locateSingleFromElement();
		if (fromElement == null) {
			return false;
		}

		Type componentType = getNakedPropertyType(fromElement);
		if ( componentType == null ) {
			throw new QueryException( "Unable to resolve path [" + parent.getPath() + "], unexpected token [" + getOriginalText() + "]" );
		}
		if (!componentType.isComponentType()) {
			throw new QueryException("Property '" + getOriginalText() + "' is not a component.  Use an alias to reference associations or collections.");
		}

		Type propertyType;
		String propertyPath = getText() + "." + getNextSibling().getText();
		try {
			// check to see if our "propPath" actually
			// represents a property on the persister
			propertyType = fromElement.getPropertyType(getText(), propertyPath);
		}
		catch (Throwable t) {
			// assume we do *not* refer to a property on the given persister
			return false;
		}

		setFromElement(fromElement);
		parent.setPropertyPath(propertyPath);
		parent.setDataType(propertyType);

		return true;
	}

	private boolean resolveAsNakedComponentPropertyRefRHS(DotNode parent) {
		FromElement fromElement = locateSingleFromElement();
		if (fromElement == null) {
			return false;
		}

		Type propertyType;
		String propertyPath = parent.getLhs().getText() + "." + getText();
		try {
			// check to see if our "propPath" actually
			// represents a property on the persister
			propertyType = fromElement.getPropertyType(getText(), propertyPath);
		}
		catch (Throwable t) {
			// assume we do *not* refer to a property on the given persister
			return false;
		}

		setFromElement(fromElement);
		// this piece is needed for usage in select clause
		super.setDataType(propertyType);
		nakedPropertyRef = true;

		return true;
	}

	private FromElement locateSingleFromElement() {
		List fromElements = getWalker().getCurrentFromClause().getFromElements();
		if (fromElements == null || fromElements.size() != 1) {
			// TODO : should this be an error?
			return null;
		}
		FromElement element = (FromElement) fromElements.get(0);
		if (element.getClassAlias() != null) {
			// naked property-refs cannot be used with an aliased from element
			return null;
		}
		return element;
	}

	@Override
	public Type getDataType() {
		Type type = super.getDataType();
		if ( type != null ) {
			return type;
		}
		FromElement fe = getFromElement();
		if ( fe != null ) {
			return fe.getDataType();
		}
		SQLFunction sf = getWalker().getSessionFactoryHelper().findSQLFunction( getText() );
		if ( sf != null ) {
			return sf.getReturnType( null, getWalker().getSessionFactoryHelper().getFactory() );
		}
		return null;
	}

	public void setScalarColumnText(int i) throws SemanticException {
		if (nakedPropertyRef) {
			// do *not* overwrite the column text, as that has already been
			// "rendered" during resolve
			ColumnHelper.generateSingleScalarColumn(this, i);
		}
		else {
			FromElement fe = getFromElement();
			if (fe != null) {
				if ( fe.getQueryableCollection() != null && fe.getQueryableCollection().getElementType().isComponentType() ) {
					ColumnHelper.generateScalarColumns( this, getColumns(), i );
				}
				else {
					setText(fe.renderScalarIdentifierSelect(i));
				}
			}
			else {
				ColumnHelper.generateSingleScalarColumn(this, i);
			}
		}
	}

	@Override
	public String getDisplayText() {
		StringBuilder buf = new StringBuilder();

		if (getType() == SqlTokenTypes.ALIAS_REF) {
			buf.append("{alias=").append(getOriginalText());
			if (getFromElement() == null) {
				buf.append(", no from element");
			}
			else {
				buf.append(", className=").append(getFromElement().getClassName());
				buf.append(", tableAlias=").append(getFromElement().getTableAlias());
			}
			buf.append("}");
		}
		else {
			buf.append( "{originalText=" ).append( getOriginalText() ).append( "}" );
		}
		return buf.toString();
	}

}
