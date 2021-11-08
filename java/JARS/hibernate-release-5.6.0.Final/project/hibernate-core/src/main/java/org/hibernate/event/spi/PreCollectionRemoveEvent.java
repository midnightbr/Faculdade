/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.spi;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.persister.collection.CollectionPersister;

/**
 * An event that occurs before a collection is removed
 *
 * @author Gail Badner
 */
public class PreCollectionRemoveEvent extends AbstractCollectionEvent {

	public PreCollectionRemoveEvent(CollectionPersister collectionPersister,
									PersistentCollection collection,
									EventSource source,
									Object loadedOwner) {
		super( collectionPersister, collection, source,
				loadedOwner,
				getOwnerIdOrNull( loadedOwner, source ) );
	}
}
