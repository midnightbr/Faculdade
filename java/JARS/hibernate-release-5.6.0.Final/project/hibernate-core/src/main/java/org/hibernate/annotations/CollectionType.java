/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Names a custom collection type for a persistent collection.  The collection can also name a @Type, which defines
 * the Hibernate Type of the collection elements.
 *
 * @see org.hibernate.type.CollectionType
 * @see org.hibernate.usertype.UserCollectionType
 *
 * @author Steve Ebersole
 *
 * @deprecated Custom handling for "collection types" will be handled differently in 6.0
 */
@java.lang.annotation.Target({FIELD, METHOD})
@Retention(RUNTIME)
@Deprecated
public @interface CollectionType {
	/**
	 * Names the type.
	 *
	 * Could name the implementation class (an implementation of {@link org.hibernate.type.CollectionType} or
	 * {@link org.hibernate.usertype.UserCollectionType}).  Could also name a custom type defined via a
	 * {@link TypeDef @TypeDef}
	 */
	String type();

	/**
	 * Specifies the class to use the semantics of.
	 *
	 * For example, specifying {@link java.util.Set} will use Set semantics.
	 *
	 * When not specified, will be inferred from the interfaces on the property
	 * as long as it extends a standard {@link java.util.Collection} or {@link java.util.Map}.
	 *
	 * @return the class to use the semantics of.
	 */
	Class<?> semantics() default void.class;

	/**
	 * Specifies configuration information for the type.  Note that if the named type is a
	 * {@link org.hibernate.usertype.UserCollectionType}, it must also implement
	 * {@link org.hibernate.usertype.ParameterizedType} in order to receive these values.
	 */
	Parameter[] parameters() default {};
}
