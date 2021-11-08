/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.action.internal;

import java.io.Serializable;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.service.spi.EventListenerGroup;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostCollectionUpdateEvent;
import org.hibernate.event.spi.PostCollectionUpdateEventListener;
import org.hibernate.event.spi.PreCollectionUpdateEvent;
import org.hibernate.event.spi.PreCollectionUpdateEventListener;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.stat.spi.StatisticsImplementor;

/**
 * The action for updating a collection
 */
public final class CollectionUpdateAction extends CollectionAction {
	private final boolean emptySnapshot;

	/**
	 * Constructs a CollectionUpdateAction
	 *
	 * @param collection The collection to update
	 * @param persister The collection persister
	 * @param id The collection key
	 * @param emptySnapshot Indicates if the snapshot is empty
	 * @param session The session
	 */
	public CollectionUpdateAction(
				final PersistentCollection collection,
				final CollectionPersister persister,
				final Serializable id,
				final boolean emptySnapshot,
				final SharedSessionContractImplementor session) {
		super( persister, collection, id, session );
		this.emptySnapshot = emptySnapshot;
	}

	@Override
	public void execute() throws HibernateException {
		final Serializable id = getKey();
		final SharedSessionContractImplementor session = getSession();
		final CollectionPersister persister = getPersister();
		final PersistentCollection collection = getCollection();
		final boolean affectedByFilters = persister.isAffectedByEnabledFilters( session );

		preUpdate();

		if ( !collection.wasInitialized() ) {
			// If there were queued operations, they would have been processed
			// and cleared by now.
			// The collection should still be dirty.
			if ( !collection.isDirty() ) {
				throw new AssertionFailure( "collection is not dirty" );
			}
			//do nothing - we only need to notify the cache... 
		}
		else if ( !affectedByFilters && collection.empty() ) {
			if ( !emptySnapshot ) {
				persister.remove( id, session );
			}
		}
		else if ( collection.needsRecreate( persister ) ) {
			if ( affectedByFilters ) {
				throw new HibernateException(
						"cannot recreate collection while filter is enabled: " +
								MessageHelper.collectionInfoString( persister, collection, id, session )
				);
			}
			if ( !emptySnapshot ) {
				persister.remove( id, session );
			}
			persister.recreate( collection, id, session );
		}
		else {
			persister.deleteRows( collection, id, session );
			persister.updateRows( collection, id, session );
			persister.insertRows( collection, id, session );
		}

		session.getPersistenceContextInternal().getCollectionEntry( collection ).afterAction( collection );
		evict();
		postUpdate();

		final StatisticsImplementor statistics = session.getFactory().getStatistics();
		if ( statistics.isStatisticsEnabled() ) {
			statistics.updateCollection( persister.getRole() );
		}
	}
	
	private void preUpdate() {
		getFastSessionServices()
				.eventListenerGroup_PRE_COLLECTION_UPDATE
				.fireLazyEventOnEachListener( this::newPreCollectionUpdateEvent, PreCollectionUpdateEventListener::onPreUpdateCollection );
	}

	private PreCollectionUpdateEvent newPreCollectionUpdateEvent() {
		return new PreCollectionUpdateEvent(
				getPersister(),
				getCollection(),
				eventSource()
		);
	}

	private void postUpdate() {
		getFastSessionServices()
				.eventListenerGroup_POST_COLLECTION_UPDATE
				.fireLazyEventOnEachListener( this::newPostCollectionUpdateEvent, PostCollectionUpdateEventListener::onPostUpdateCollection );
	}

	private PostCollectionUpdateEvent newPostCollectionUpdateEvent() {
		return new PostCollectionUpdateEvent(
				getPersister(),
				getCollection(),
				eventSource()
		);
	}

}
