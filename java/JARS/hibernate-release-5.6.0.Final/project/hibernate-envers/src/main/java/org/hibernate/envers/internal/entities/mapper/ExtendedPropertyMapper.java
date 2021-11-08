/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.entities.mapper;

import java.util.Map;

import org.hibernate.engine.spi.SessionImplementor;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public interface ExtendedPropertyMapper extends PropertyMapper, CompositeMapperBuilder {
	boolean map(
			SessionImplementor session,
			Map<String, Object> data,
			String[] propertyNames,
			Object[] newState,
			Object[] oldState);
}
