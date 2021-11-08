/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.entity;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.Type;

/**
 * Implemented by a <tt>EntityPersister</tt> that may be loaded
 * using <tt>Loader</tt>.
 *
 * @see org.hibernate.loader.Loader
 * @author Gavin King
 */
public interface Loadable extends EntityPersister {
	
	String ROWID_ALIAS = "rowid_";

	/**
	 * Does this persistent class have subclasses?
	 */
	boolean hasSubclasses();

	/**
	 * Get the discriminator type
	 */
	Type getDiscriminatorType();

	/**
	 * Get the discriminator value
	 */
	Object getDiscriminatorValue();

	/**
	 * Get the concrete subclass corresponding to the given discriminator
	 * value
	 */
	String getSubclassForDiscriminatorValue(Object value);

	/**
	 * Get the names of columns used to persist the identifier
	 */
	String[] getIdentifierColumnNames();

	/**
	 * Get the result set aliases used for the identifier columns, given a suffix
	 */
	String[] getIdentifierAliases(String suffix);
	/**
	 * Get the result set aliases used for the property columns, given a suffix (properties of this class, only).
	 */
	String[] getPropertyAliases(String suffix, int i);
	
	/**
	 * Get the result set column names mapped for this property (properties of this class, only).
	 */
	String[] getPropertyColumnNames(int i);
	
	/**
	 * Get the result set aliases used for the identifier columns, given a suffix
	 */
	String getDiscriminatorAlias(String suffix);
	
	/**
	 * @return the column name for the discriminator as specified in the mapping.
	 */
	String getDiscriminatorColumnName();
	
	/**
	 * Does the result set contain rowids?
	 */
	boolean hasRowId();
	
	/**
	 * Retrieve property values from one row of a result set
	 */
	default Object[] hydrate(
			ResultSet rs,
			Serializable id,
			Object object,
			Loadable rootLoadable,
			String[][] suffixedPropertyColumns,
			boolean forceEager,
			SharedSessionContractImplementor session) throws SQLException, HibernateException {
		return hydrate( rs, id, object, rootLoadable, suffixedPropertyColumns, forceEager, null, session );
	}

	/**
	 * Retrieve property values from one row of a result set
	 */
	Object[] hydrate(
			ResultSet rs,
			Serializable id,
			Object object,
			Loadable rootLoadable,
			String[][] suffixedPropertyColumns,
			boolean forceEager,
			boolean[] propertiesForceEager,
			SharedSessionContractImplementor session) throws SQLException, HibernateException;

	boolean isAbstract();

	/**
	 * Register the name of a fetch profile determined to have an affect on the
	 * underlying loadable in regards to the fact that the underlying load SQL
	 * needs to be adjust when the given fetch profile is enabled.
	 * 
	 * @param fetchProfileName The name of the profile affecting this.
	 */
	void registerAffectingFetchProfile(String fetchProfileName);

	/**
	 * Given a column name and the root table alias in use for the entity hierarchy, determine the proper table alias
	 * for the table in that hierarchy that contains said column.
	 * <p/>
	 * NOTE : Generally speaking the column is not validated to exist.  Most implementations simply return the
	 * root alias; the exception is {@link JoinedSubclassEntityPersister}
	 *
	 * @param columnName The column name
	 * @param rootAlias The hierarchy root alias
	 *
	 * @return The proper table alias for qualifying the given column.
	 */
	String getTableAliasForColumn(String columnName, String rootAlias);
}
