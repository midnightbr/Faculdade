/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tuple;

import org.hibernate.property.access.spi.Getter;

/**
 * A tuplizer defines the contract for things which know how to manage
 * a particular representation of a piece of data, given that
 * representation's {@link org.hibernate.EntityMode} (the entity-mode
 * essentially defining which representation).
 * </p>
 * If that given piece of data is thought of as a data structure, then a tuplizer
 * is the thing which knows how to<ul>
 * <li>create such a data structure appropriately
 * <li>extract values from and inject values into such a data structure
 * </ul>
 * </p>
 * For example, a given piece of data might be represented as a POJO class.
 * Here, it's representation and entity-mode is POJO.  Well a tuplizer for POJO
 * entity-modes would know how to<ul>
 * <li>create the data structure by calling the POJO's constructor
 * <li>extract and inject values through getters/setter, or by direct field access, etc
 * </ul>
 * </p>
 *
 * @see org.hibernate.tuple.entity.EntityTuplizer
 * @see org.hibernate.tuple.component.ComponentTuplizer
 *
 * @author Steve Ebersole
 */
public interface Tuplizer {
	/**
	 * Extract the current values contained on the given entity.
	 *
	 * @param entity The entity from which to extract values.
	 * @return The current property values.
	 */
	public Object[] getPropertyValues(Object entity);

	/**
	 * Inject the given values into the given entity.
	 *
	 * @param entity The entity.
	 * @param values The values to be injected.
	 */
	public void setPropertyValues(Object entity, Object[] values);

	/**
	 * Extract the value of a particular property from the given entity.
	 *
	 * @param entity The entity from which to extract the property value.
	 * @param i The index of the property for which to extract the value.
	 * @return The current value of the given property on the given entity.
	 */
	public Object getPropertyValue(Object entity, int i);

	/**
	 * Generate a new, empty entity.
	 *
	 * @return The new, empty entity instance.
	 */
	public Object instantiate();

	/**
	 * Is the given object considered an instance of the the entity (accounting
	 * for entity-mode) managed by this tuplizer.
	 *
	 * @param object The object to be checked.
	 * @return True if the object is considered as an instance of this entity
	 *      within the given mode.
	 */
	public boolean isInstance(Object object);

	/**
	 * Return the pojo class managed by this tuplizer.
	 * </p>
	 * Need to determine how to best handle this for the Tuplizers for EntityModes
	 * other than POJO.
	 * </p>
	 * todo : be really nice to not have this here since it is essentially pojo specific...
	 *
	 * @return The persistent class.
	 */
	public Class getMappedClass();

	/**
	 * Retrieve the getter for the specified property.
	 *
	 * @param i The property index.
	 * @return The property getter.
	 */
	public Getter getGetter(int i);
}
