/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria.internal.expression.function;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import javax.persistence.criteria.Expression;

import org.hibernate.query.criteria.internal.CriteriaBuilderImpl;
import org.hibernate.query.criteria.internal.ParameterContainer;
import org.hibernate.query.criteria.internal.ParameterRegistry;
import org.hibernate.query.criteria.internal.Renderable;
import org.hibernate.query.criteria.internal.compile.RenderingContext;

/**
 * Support for functions with parameters.
 *
 * @author Steve Ebersole
 */
public class ParameterizedFunctionExpression<X>
		extends BasicFunctionExpression<X>
		implements FunctionExpression<X> {

	public static final List<String> STANDARD_JPA_FUNCTION_NAMES = Arrays.asList(
			// 4.6.17.2.1
			"CONCAT",
			"SUBSTRING",
			"TRIM",
			"UPPER",
			"LOWER",
			"LOCATE",
			"LENGTH",
			//4.6.17.2.2
			"ABS",
			"SQRT",
			"MOD",
			"SIZE",
			"INDEX",
			// 4.6.17.2.3
			"CURRENT_DATE",
			"CURRENT_TIME",
			"CURRENT_TIMESTAMP"
	);

	private final List<Expression<?>> argumentExpressions;
	private final boolean isStandardJpaFunction;

	public ParameterizedFunctionExpression(
			CriteriaBuilderImpl criteriaBuilder,
			Class<X> javaType,
			String functionName,
			List<Expression<?>> argumentExpressions) {
		super( criteriaBuilder, javaType, functionName );
		this.argumentExpressions = argumentExpressions;
		this.isStandardJpaFunction = STANDARD_JPA_FUNCTION_NAMES.contains( functionName.toUpperCase(Locale.ROOT) );
	}

	public ParameterizedFunctionExpression(
			CriteriaBuilderImpl criteriaBuilder,
			Class<X> javaType,
			String functionName,
			Expression<?>... argumentExpressions) {
		super( criteriaBuilder, javaType, functionName );
		this.argumentExpressions = Arrays.asList( argumentExpressions );
		this.isStandardJpaFunction = STANDARD_JPA_FUNCTION_NAMES.contains( functionName.toUpperCase(Locale.ROOT) );
	}

	protected boolean isStandardJpaFunction() {
		return isStandardJpaFunction;
	}

	protected  static int properSize(int number) {
		return number + (int)( number*.75 ) + 1;
	}

	public List<Expression<?>> getArgumentExpressions() {
		return argumentExpressions;
	}

	@Override
	public void registerParameters(ParameterRegistry registry) {
		for ( Expression argument : getArgumentExpressions() ) {
			if ( ParameterContainer.class.isInstance( argument ) ) {
				( (ParameterContainer) argument ).registerParameters(registry);
			}
		}
	}

	@Override
	public String render(RenderingContext renderingContext) {
		renderingContext.getFunctionStack().push( this );

		try {
			final StringBuilder buffer = new StringBuilder();
			if ( isStandardJpaFunction() ) {
				buffer.append( getFunctionName() ).append( "(" );
			}
			else {
				buffer.append( "function('" )
						.append( getFunctionName() )
						.append( "', " );
			}

			renderArguments( buffer, renderingContext );

			return buffer.append( ')' ).toString();
		}
		finally {
			renderingContext.getFunctionStack().pop();
		}
	}

	protected void renderArguments(StringBuilder buffer, RenderingContext renderingContext) {
		String sep = "";
		for ( Expression argument : argumentExpressions ) {
			buffer.append( sep ).append( ( (Renderable) argument ).render( renderingContext ) );
			sep = ", ";
		}
	}



}
