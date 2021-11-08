/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast.tree;

import java.util.Arrays;

import org.hibernate.HibernateException;
import org.hibernate.TypeMismatchException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.internal.antlr.HqlSqlTokenTypes;
import org.hibernate.hql.internal.ast.QuerySyntaxException;
import org.hibernate.hql.internal.ast.util.ColumnHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.param.ParameterSpecification;
import org.hibernate.type.OneToOneType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;

import antlr.SemanticException;
import antlr.collections.AST;

/**
 * Contract for nodes representing binary operators.
 *
 * @author Steve Ebersole
 */
public class BinaryLogicOperatorNode extends AbstractSelectExpression implements BinaryOperatorNode {
	/**
	 * Performs the operator node initialization by seeking out any parameter
	 * nodes and setting their expected type, if possible.
	 */
	@Override
	public void initialize() throws SemanticException {
		final Node lhs = getLeftHandOperand();
		if ( lhs == null ) {
			throw new SemanticException( "left-hand operand of a binary operator was null" );
		}

		final Node rhs = getRightHandOperand();
		if ( rhs == null ) {
			throw new SemanticException( "right-hand operand of a binary operator was null" );
		}

		Type lhsType = extractDataType( lhs );
		Type rhsType = extractDataType( rhs );

		if ( lhsType == null ) {
			lhsType = rhsType;
		}
		if ( rhsType == null ) {
			rhsType = lhsType;
		}

		if ( ExpectedTypeAwareNode.class.isAssignableFrom( lhs.getClass() ) ) {
			( (ExpectedTypeAwareNode) lhs ).setExpectedType( rhsType );
		}
		if ( ExpectedTypeAwareNode.class.isAssignableFrom( rhs.getClass() ) ) {
			( (ExpectedTypeAwareNode) rhs ).setExpectedType( lhsType );
		}

		mutateRowValueConstructorSyntaxesIfNecessary( lhsType, rhsType );
	}

	protected final void mutateRowValueConstructorSyntaxesIfNecessary(Type lhsType, Type rhsType) {
		// TODO : this really needs to be delayed until after we definitively know all node types
		// where this is currently a problem is parameters for which we cannot unequivocally
		// resolve an expected type
		SessionFactoryImplementor sessionFactory = getSessionFactoryHelper().getFactory();
		if ( lhsType != null && rhsType != null ) {
			int lhsColumnSpan = getColumnSpan( lhsType, sessionFactory );
			if ( lhsColumnSpan != getColumnSpan( rhsType, sessionFactory ) ) {
				throw new TypeMismatchException(
						"left and right hand sides of a binary logic operator were incompatible [" +
								lhsType.getName() + " : " + rhsType.getName() + "]"
				);
			}
			if ( lhsColumnSpan > 1 ) {
				// for dialects which are known to not support ANSI-SQL row-value-constructor syntax,
				// we should mutate the tree.
				if ( !useRowValueConstructorSyntax( sessionFactory.getDialect() ) ) {
					mutateRowValueConstructorSyntax( lhsColumnSpan );
				}
			}
		}
	}

	private boolean useRowValueConstructorSyntax(Dialect dialect) {
		if ( isInsideSetClause() ) {
			return dialect.supportsRowValueConstructorSyntaxInSet();
		}
		return dialect.supportsRowValueConstructorSyntax();
	}

	private int getColumnSpan(Type type, SessionFactoryImplementor sfi) {
		int columnSpan = type.getColumnSpan( sfi );
		if ( columnSpan == 0 && type instanceof OneToOneType ) {
			columnSpan = ( (OneToOneType) type ).getIdentifierOrUniqueKeyType( sfi ).getColumnSpan( sfi );
		}
		return columnSpan;
	}

	/**
	 * Mutate the subtree relating to a row-value-constructor to instead use
	 * a series of ANDed predicates.  This allows multi-column type comparisons
	 * and explicit row-value-constructor syntax even on databases which do
	 * not support row-value-constructor.
	 * <p/>
	 * For example, here we'd mutate "... where (col1, col2) = ('val1', 'val2) ..." to
	 * "... where col1 = 'val1' and col2 = 'val2' ..."
	 *
	 * @param valueElements The number of elements in the row value constructor list.
	 */
	private void mutateRowValueConstructorSyntax(int valueElements) {
		// mutation depends on the types of nodes involved...
		int comparisonType = getType();
		String comparisonText = getText();
		if ( !isInsideSetClause() ) {
			switch ( comparisonType ) {
				case HqlSqlTokenTypes.EQ:
					setType( HqlSqlTokenTypes.AND );
					setText( "AND" );
					break;

				case HqlSqlTokenTypes.NE:
					setType( HqlSqlTokenTypes.OR );
					setText( "OR" );
					break;

				default:
					throw new QuerySyntaxException( comparisonText + " operator not supported on composite types." );
			}
		}

		String[] lhsElementTexts = extractMutationTexts( getLeftHandOperand(), valueElements );
		String[] rhsElementTexts = extractMutationTexts( getRightHandOperand(), valueElements );

		ParameterSpecification lhsEmbeddedCompositeParameterSpecification =
				getLeftHandOperand() == null || ( !ParameterNode.class.isInstance( getLeftHandOperand() ) )
						? null
						: ( (ParameterNode) getLeftHandOperand() ).getHqlParameterSpecification();

		ParameterSpecification rhsEmbeddedCompositeParameterSpecification =
				getRightHandOperand() == null || ( !ParameterNode.class.isInstance( getRightHandOperand() ) )
						? null
						: ( (ParameterNode) getRightHandOperand() ).getHqlParameterSpecification();

		translate(
				valueElements,
				comparisonType,
				comparisonText,
				lhsElementTexts,
				rhsElementTexts,
				lhsEmbeddedCompositeParameterSpecification,
				rhsEmbeddedCompositeParameterSpecification,
				this
		);
	}

	protected void translate(
			int valueElements, 
			int comparisonType,
			String comparisonText, 
			String[] lhsElementTexts,
			String[] rhsElementTexts,
			ParameterSpecification lhsEmbeddedCompositeParameterSpecification,
			ParameterSpecification rhsEmbeddedCompositeParameterSpecification,
			AST container) {
		Node leftHandOperand = this.getLeftHandOperand();
		Node rightHandOperand = this.getRightHandOperand();

		for ( int i = valueElements - 1; i > 0; i-- ) {
			if ( i == 1 ) {
				final AST op1;
				if ( isInsideSetClause() ) {
					op1 = container;
				}
				else {
					op1 = getASTFactory().create( comparisonType, comparisonText );
				}
				
				SqlFragment lhs1 = (SqlFragment) getASTFactory().create( HqlSqlTokenTypes.SQL_TOKEN, lhsElementTexts[0] );
				SqlFragment rhs1 = (SqlFragment) getASTFactory().create( HqlSqlTokenTypes.SQL_TOKEN, rhsElementTexts[0] );
				copyReferencedTables( leftHandOperand, lhs1 );
				copyReferencedTables( rightHandOperand, rhs1 );
				op1.setFirstChild( lhs1 );
				lhs1.setNextSibling( rhs1 );
				
				AST op2 = getASTFactory().create( comparisonType, comparisonText );
				SqlFragment lhs2 = (SqlFragment) getASTFactory().create( HqlSqlTokenTypes.SQL_TOKEN, lhsElementTexts[1] );
				SqlFragment rhs2 = (SqlFragment) getASTFactory().create( HqlSqlTokenTypes.SQL_TOKEN, rhsElementTexts[1] );
				copyReferencedTables( leftHandOperand, lhs2 );
				copyReferencedTables( rightHandOperand, rhs2 );
				op2.setFirstChild( lhs2 );
				lhs2.setNextSibling( rhs2 );
				op1.setNextSibling( op2 );

				if ( !isInsideSetClause() ) {
					container.setFirstChild( op1 );
				}

				// "pass along" our initial embedded parameter node(s) to the first generated
				// sql fragment so that it can be handled later for parameter binding...
				if ( lhsEmbeddedCompositeParameterSpecification != null ) {
					lhs1.addEmbeddedParameter( lhsEmbeddedCompositeParameterSpecification );
				}
				if ( rhsEmbeddedCompositeParameterSpecification != null ) {
					lhs1.addEmbeddedParameter( rhsEmbeddedCompositeParameterSpecification );
				}
			}
			else {
				AST op = getASTFactory().create( comparisonType, comparisonText );
				SqlFragment lhs = (SqlFragment) getASTFactory().create( HqlSqlTokenTypes.SQL_TOKEN, lhsElementTexts[i] );
				SqlFragment rhs = (SqlFragment) getASTFactory().create( HqlSqlTokenTypes.SQL_TOKEN, rhsElementTexts[i] );
				copyReferencedTables( leftHandOperand, lhs );
				copyReferencedTables( rightHandOperand, rhs );
				op.setFirstChild( lhs );
				lhs.setNextSibling( rhs );
				AST newContainer = getASTFactory().create( container.getType(), container.getText() );
				container.setFirstChild( newContainer );
				newContainer.setNextSibling( op );
				container = newContainer;
			}
		}
	}

	private boolean isInsideSetClause() {
		return getWalker().getCurrentClauseType() == HqlSqlTokenTypes.SET;
	}

	private static void copyReferencedTables(Node from, SqlFragment to) {
		if (from instanceof TableReferenceNode) {
			TableReferenceNode tableReferenceNode = (TableReferenceNode) from;
			to.setReferencedTables( tableReferenceNode.getReferencedTables() );
		}
	}

	protected static String[] extractMutationTexts(Node operand, int count) {
		if ( operand instanceof ParameterNode ) {
			String[] rtn = new String[count];
			Arrays.fill( rtn, "?" );
			return rtn;
		}
		else if ( operand.getType() == HqlSqlTokenTypes.VECTOR_EXPR ) {
			String[] rtn = new String[operand.getNumberOfChildren()];
			int x = 0;
			AST node = operand.getFirstChild();
			while ( node != null ) {
				rtn[x++] = node.getText();
				node = node.getNextSibling();
			}
			return rtn;
		}
		else if ( operand instanceof SqlNode ) {
			String nodeText = operand.getText();
			if ( nodeText.startsWith( "(" ) ) {
				nodeText = nodeText.substring( 1 );
			}
			if ( nodeText.endsWith( ")" ) ) {
				nodeText = nodeText.substring( 0, nodeText.length() - 1 );
			}
			String[] splits = StringHelper.split( ", ", nodeText );
			if ( count != splits.length ) {
				throw new HibernateException( "SqlNode's text did not reference expected number of columns" );
			}
			return splits;
		}
		else {
			throw new HibernateException( "dont know how to extract row value elements from node : " + operand );
		}
	}

	protected Type extractDataType(Node operand) {
		Type type = null;
		if ( operand instanceof SqlNode ) {
			type = ( (SqlNode) operand ).getDataType();
		}
		if ( type == null && operand instanceof ExpectedTypeAwareNode ) {
			type = ( (ExpectedTypeAwareNode) operand ).getExpectedType();
		}
		return type;
	}

	@Override
	public Type getDataType() {
		// logic operators by definition resolve to booleans
		return StandardBasicTypes.BOOLEAN;
	}

	/**
	 * Retrieves the left-hand operand of the operator.
	 *
	 * @return The left-hand operand
	 */
	public Node getLeftHandOperand() {
		return (Node) getFirstChild();
	}

	/**
	 * Retrieves the right-hand operand of the operator.
	 *
	 * @return The right-hand operand
	 */
	public Node getRightHandOperand() {
		return (Node) getFirstChild().getNextSibling();
	}

	public void setScalarColumnText(int i) throws SemanticException {
		ColumnHelper.generateSingleScalarColumn( this, i );
	}
}
