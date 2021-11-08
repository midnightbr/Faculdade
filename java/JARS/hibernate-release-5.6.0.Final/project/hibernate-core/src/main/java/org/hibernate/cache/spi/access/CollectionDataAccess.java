/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.spi.access;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.collection.CollectionPersister;

/**
 * Contract for managing transactional and concurrent access to cached collection
 * data.  For cached collection data, all modification actions actually just
 * invalidate the entry(s).  The call sequence here is:
 * {@link #lockItem} -> {@link #remove} -> {@link #unlockItem}
 * <p/>
 * There is another usage pattern that is used to invalidate entries
 * afterQuery performing "bulk" HQL/SQL operations:
 * {@link #lockRegion} -> {@link #removeAll} -> {@link #unlockRegion}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public interface CollectionDataAccess extends CachedDomainDataAccess {
	/**
	 * To create instances of CollectionCacheKey for this region, Hibernate will invoke this method
	 * exclusively so that generated implementations can generate optimised keys.
	 * @param id the primary identifier of the Collection
	 * @param collectionDescriptor the descriptor of the collection for which a key is being generated
	 * @param factory a reference to the current SessionFactory
	 * @param tenantIdentifier the tenant id, or null if multi-tenancy is not being used.
	 *
	 * @return a key which can be used to identify this collection on this same region
	 */
	Object generateCacheKey(
			Object id,
			CollectionPersister collectionDescriptor,
			SessionFactoryImplementor factory,
			String tenantIdentifier);

	/**
	 * Performs reverse operation to {@link #generateCacheKey}
	 *
	 * @param cacheKey key previously returned from {@link #generateCacheKey}
	 *
	 * @return original key passed to {@link #generateCacheKey}
	 */
	Object getCacheKeyId(Object cacheKey);


}
