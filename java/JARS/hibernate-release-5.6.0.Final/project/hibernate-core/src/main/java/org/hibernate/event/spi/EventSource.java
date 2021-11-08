/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.spi;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.ActionQueue;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.persister.entity.EntityPersister;

/**
 * @author Gavin King
 */
public interface EventSource extends SessionImplementor {
	
	/**
	 * Get the ActionQueue for this session
	 */
	ActionQueue getActionQueue();

	/**
	 * Instantiate an entity instance, using either an interceptor,
	 * or the given persister
	 */
	Object instantiate(EntityPersister persister, Serializable id) throws HibernateException;

	/**
	 * Force an immediate flush
	 */
	void forceFlush(EntityEntry e) throws HibernateException;

	/**
	 * Cascade merge an entity instance
	 */
	void merge(String entityName, Object object, Map copiedAlready) throws HibernateException;
	/**
	 * Cascade persist an entity instance
	 */
	void persist(String entityName, Object object, Map createdAlready) throws HibernateException;

	/**
	 * Cascade persist an entity instance during the flush process
	 */
	void persistOnFlush(String entityName, Object object, Map copiedAlready);
	/**
	 * Cascade refresh an entity instance
	 */
	void refresh(String entityName, Object object, Map refreshedAlready) throws HibernateException;
	/**
	 * Cascade delete an entity instance
	 */
	void delete(String entityName, Object child, boolean isCascadeDeleteEnabled, Set transientEntities);
	/**
	 * A specialized type of deletion for orphan removal that must occur prior to queued inserts and updates.
	 */
	// TODO: The removeOrphan concept is a temporary "hack" for HHH-6484.  This should be removed once action/task
	// ordering is improved.
	void removeOrphanBeforeUpdates(String entityName, Object child);

}
