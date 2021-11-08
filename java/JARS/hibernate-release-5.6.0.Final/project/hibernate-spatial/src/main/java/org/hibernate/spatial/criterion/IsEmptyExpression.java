/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.criterion;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.criterion.CriteriaQuery;
import org.hibernate.criterion.Criterion;
import org.hibernate.engine.spi.TypedValue;
import org.hibernate.spatial.SpatialDialect;
import org.hibernate.spatial.SpatialFunction;

/**
 * A {@code Criterion} constraining a geometry property to be (non-)empty.
 *
 * @author Karel Maesen, Geovise BVBA
 */
public class IsEmptyExpression implements Criterion {

	private static final TypedValue[] NO_VALUES = new TypedValue[0];

	private final String propertyName;
	private final boolean isEmpty;

	/**
	 * Constructs an instance for the specified property
	 *
	 * @param propertyName The name of the property being constrained
	 * @param isEmpty Whether to constrain the property to be empty or non-empty
	 */
	public IsEmptyExpression(String propertyName, boolean isEmpty) {
		this.propertyName = propertyName;
		this.isEmpty = isEmpty;
	}

	@Override
	public String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
		final String column = ExpressionUtil.findColumn( propertyName, criteria, criteriaQuery );
		final SpatialDialect spatialDialect = ExpressionUtil.getSpatialDialect(
				criteriaQuery,
				SpatialFunction.isempty
		);
		return spatialDialect.getIsEmptySQL( column, isEmpty );
	}

	@Override
	public TypedValue[] getTypedValues(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
		return NO_VALUES;
	}

}
