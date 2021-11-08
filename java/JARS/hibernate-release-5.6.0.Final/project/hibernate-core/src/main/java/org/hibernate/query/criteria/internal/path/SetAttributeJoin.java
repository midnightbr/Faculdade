/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria.internal.path;

import java.io.Serializable;
import java.util.Set;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.SetAttribute;

import org.hibernate.query.criteria.internal.CriteriaBuilderImpl;
import org.hibernate.query.criteria.internal.CriteriaSubqueryImpl;
import org.hibernate.query.criteria.internal.FromImplementor;
import org.hibernate.query.criteria.internal.PathImplementor;
import org.hibernate.query.criteria.internal.PathSource;
import org.hibernate.query.criteria.internal.SetJoinImplementor;
import org.hibernate.query.criteria.internal.compile.RenderingContext;

/**
 * Models a join based on a set-style plural association attribute.
 *
 * @param <O> Represents the parameterized type of the set owner
 * @param <E> Represents the parameterized type of the set elements
 *
 * @author Steve Ebersole
 */
public class SetAttributeJoin<O,E>
		extends PluralAttributeJoinSupport<O, Set<E>,E>
		implements SetJoinImplementor<O,E>, Serializable {

	public SetAttributeJoin(
			CriteriaBuilderImpl criteriaBuilder,
			Class<E> javaType,
			PathSource<O> pathSource,
			SetAttribute<? super O, E> joinAttribute,
			JoinType joinType) {
		super( criteriaBuilder, javaType, pathSource, joinAttribute, joinType );
	}

	@Override
	public SetAttribute<? super O, E> getAttribute() {
		return (SetAttribute<? super O, E>) super.getAttribute();
	}

	@Override
	public SetAttribute<? super O, E> getModel() {
		return getAttribute();
	}

	@Override
	public final SetAttributeJoin<O,E> correlateTo(CriteriaSubqueryImpl subquery) {
		return (SetAttributeJoin<O,E>) super.correlateTo( subquery );
	}

	@Override
	protected FromImplementor<O, E> createCorrelationDelegate() {
		return new SetAttributeJoin<O,E>(
				criteriaBuilder(),
				getJavaType(),
				(PathImplementor<O>) getParentPath(),
				getAttribute(),
				getJoinType()
		);
	}

	@Override
	public SetJoinImplementor<O, E> on(Predicate... restrictions) {
		return (SetJoinImplementor<O, E>) super.on( restrictions );
	}

	@Override
	public SetJoinImplementor<O, E> on(Expression<Boolean> restriction) {
		return (SetJoinImplementor<O, E>) super.on( restriction );
	}

	@Override
	public <T extends E> SetAttributeJoin<O,T> treatAs(Class<T> treatAsType) {
		return new TreatedSetAttributeJoin<O,T>( this, treatAsType );
	}

	public static class TreatedSetAttributeJoin<O,T> extends SetAttributeJoin<O, T> {
		private final SetAttributeJoin<O, ? super T> original;
		private final Class<T> treatAsType;

		@SuppressWarnings("unchecked")
		public TreatedSetAttributeJoin(SetAttributeJoin<O, ? super T> original, Class<T> treatAsType) {
			super(
					original.criteriaBuilder(),
					treatAsType,
					original.getPathSource(),
					(SetAttribute<? super O, T>) original.getAttribute(),
					original.getJoinType()
			);
			this.original = original;
			this.treatAsType = treatAsType;
		}

		@Override
		public String getAlias() {
			return isCorrelated() ? getCorrelationParent().getAlias() : super.getAlias();
		}

		@Override
		public void prepareAlias(RenderingContext renderingContext) {
			if ( getAlias() == null ) {
				if ( isCorrelated() ) {
					setAlias( getCorrelationParent().getAlias() );
				}
				else {
					setAlias( renderingContext.generateAlias() );
				}
			}
		}

		@Override
		protected void setAlias(String alias) {
			super.setAlias( alias );
			original.setAlias( alias );
		}

		@Override
		public String render(RenderingContext renderingContext) {
			return "treat(" + original.render( renderingContext ) + " as " + treatAsType.getName() + ")";
		}

		@Override
		protected ManagedType<T> locateManagedType() {
			return criteriaBuilder().getEntityManagerFactory().getMetamodel().managedType( treatAsType );
		}

		@Override
		public String getPathIdentifier() {
			return "treat(" + getAlias() + " as " + treatAsType.getName() + ")";
		}

		@Override
		protected PathSource getPathSourceForSubPaths() {
			return this;
		}
	}
}
