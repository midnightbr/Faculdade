/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal.util.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Various help for handling collections.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public final class CollectionHelper {
	public static final int MINIMUM_INITIAL_CAPACITY = 16;
	public static final float LOAD_FACTOR = 0.75f;

	/**
	 * @deprecated use  {@link java.util.Collections#EMPTY_LIST} or {@link java.util.Collections#emptyList()}  instead
	 */
	@Deprecated
	public static final List EMPTY_LIST = Collections.EMPTY_LIST;
	/**
	 * @deprecated use {@link java.util.Collections#EMPTY_LIST} or {@link java.util.Collections#emptyList()}  instead
	 */
	@Deprecated
	public static final Collection EMPTY_COLLECTION = Collections.EMPTY_LIST;
	/**
	 * @deprecated use {@link java.util.Collections#EMPTY_MAP} or {@link java.util.Collections#emptyMap()}  instead
	 */
	@Deprecated
	public static final Map EMPTY_MAP = Collections.EMPTY_MAP;

	private CollectionHelper() {
	}

	/**
	 * Build a properly sized map, especially handling load size and load factor to prevent immediate resizing.
	 * <p/>
	 * Especially helpful for copy map contents.
	 *
	 * @param size The size to make the map.
	 *
	 * @return The sized map.
	 */
	public static <K, V> Map<K, V> mapOfSize(int size) {
		return new HashMap<>( determineProperSizing( size ), LOAD_FACTOR );
	}

	/**
	 * Given a map, determine the proper initial size for a new Map to hold the same number of values.
	 * Specifically we want to account for load size and load factor to prevent immediate resizing.
	 *
	 * @param original The original map
	 *
	 * @return The proper size.
	 */
	public static int determineProperSizing(Map original) {
		return determineProperSizing( original.size() );
	}

	public static <X, Y> Map<X, Y> makeCopy(Map<X, Y> map) {
		final Map<X, Y> copy = mapOfSize( map.size() + 1 );
		copy.putAll( map );
		return copy;
	}

	public static <K, V> HashMap<K, V> makeCopy(
			Map<K, V> original,
			Function<K, K> keyTransformer,
			Function<V, V> valueTransformer) {
		if ( original == null ) {
			return null;
		}

		final HashMap<K, V> copy = new HashMap<>( determineProperSizing( original ) );

		original.forEach(
				(key, value) -> copy.put(
						keyTransformer.apply( key ),
						valueTransformer.apply( value )
				)
		);

		return copy;
	}

	public static <K, V> Map<K, V> makeMap(
			Collection<V> collection,
			Function<V,K> keyProducer) {
		return makeMap( collection, keyProducer, v -> v );
	}

	public static <K, V, E> Map<K, V> makeMap(
			Collection<E> collection,
			Function<E,K> keyProducer,
			Function<E,V> valueProducer) {
		if ( isEmpty( collection ) ) {
			return Collections.emptyMap();
		}

		final Map<K, V> map = new HashMap<>( determineProperSizing( collection.size() ));

		for ( E element : collection ) {
			map.put(
					keyProducer.apply( element ),
					valueProducer.apply( element )
			);
		}

		return map;
	}

	/**
	 * Given a set, determine the proper initial size for a new set to hold the same number of values.
	 * Specifically we want to account for load size and load factor to prevent immediate resizing.
	 *
	 * @param original The original set
	 *
	 * @return The proper size.
	 */
	public static int determineProperSizing(Set original) {
		return determineProperSizing( original.size() );
	}

	/**
	 * Determine the proper initial size for a new collection in order for it to hold the given a number of elements.
	 * Specifically we want to account for load size and load factor to prevent immediate resizing.
	 *
	 * @param numberOfElements The number of elements to be stored.
	 *
	 * @return The proper size.
	 */
	public static int determineProperSizing(int numberOfElements) {
		int actual = ( (int) ( numberOfElements / LOAD_FACTOR ) ) + 1;
		return Math.max( actual, MINIMUM_INITIAL_CAPACITY );
	}

	/**
	 * Create a properly sized {@link ConcurrentHashMap} based on the given expected number of elements.
	 *
	 * @param expectedNumberOfElements The expected number of elements for the created map
	 * @param <K> The map key type
	 * @param <V> The map value type
	 *
	 * @return The created map.
	 */
	public static <K, V> ConcurrentHashMap<K, V> concurrentMap(int expectedNumberOfElements) {
		return concurrentMap( expectedNumberOfElements, LOAD_FACTOR );
	}

	/**
	 * Create a properly sized {@link ConcurrentHashMap} based on the given expected number of elements and an
	 * explicit load factor
	 *
	 * @param expectedNumberOfElements The expected number of elements for the created map
	 * @param loadFactor The collection load factor
	 * @param <K> The map key type
	 * @param <V> The map value type
	 *
	 * @return The created map.
	 */
	public static <K, V> ConcurrentHashMap<K, V> concurrentMap(int expectedNumberOfElements, float loadFactor) {
		final int size = expectedNumberOfElements + 1 + (int) ( expectedNumberOfElements * loadFactor );
		return new ConcurrentHashMap<K, V>( size, loadFactor );
	}

	public static <T> ArrayList<T> arrayList(int anticipatedSize) {
		return new ArrayList<T>( anticipatedSize );
	}

	public static <T> Set<T> makeCopy(Set<T> source) {
		if ( source == null ) {
			return null;
		}

		final int size = source.size();
		final Set<T> copy = new HashSet<T>( size + 1 );
		copy.addAll( source );
		return copy;
	}

	public static boolean isEmpty(Collection collection) {
		return collection == null || collection.isEmpty();
	}

	public static boolean isEmpty(Map map) {
		return map == null || map.isEmpty();
	}

	public static boolean isNotEmpty(Collection collection) {
		return !isEmpty( collection );
	}

	public static boolean isNotEmpty(Map map) {
		return !isEmpty( map );
	}

	public static boolean isEmpty(Object[] objects) {
		return objects == null || objects.length == 0;
	}

	/**
	 * Use to convert sets which will be retained for a long time,
	 * such as for the lifetime of the Hibernate ORM instance.
	 * The returned Set might be immutable, but there is no guarantee of this:
	 * consider it immutable but don't rely on this.
	 * The goal is to save memory.
	 * @param set
	 * @param <T>
	 * @return
	 */
	public static <T> Set<T> toSmallSet(Set<T> set) {
		switch ( set.size() ) {
			case 0:
				return Collections.EMPTY_SET;
			case 1:
				return Collections.singleton( set.iterator().next() );
			default:
				//TODO assert tests pass even if this is set to return an unmodifiable Set
				return set;
		}
	}

	/**
	 * Use to convert Maps which will be retained for a long time,
	 * such as for the lifetime of the Hibernate ORM instance.
	 * The returned Map might be immutable, but there is no guarantee of this:
	 * consider it immutable but don't rely on this.
	 * The goal is to save memory.
	 * @param map
	 * @param <K>
	 * @param <V>
	 * @return
	 */
	public static <K, V> Map<K, V> toSmallMap(final Map<K, V> map) {
		switch ( map.size() ) {
			case 0:
				return Collections.EMPTY_MAP;
			case 1:
				Map.Entry<K, V> entry = map.entrySet().iterator().next();
				return Collections.singletonMap( entry.getKey(), entry.getValue() );
			default:
				//TODO assert tests pass even if this is set to return an unmodifiable Map
				return map;
		}
	}

	/**
	 * Use to convert ArrayList instances which will be retained for a long time,
	 * such as for the lifetime of the Hibernate ORM instance.
	 * The returned List might be immutable, but there is no guarantee of this:
	 * consider it immutable but don't rely on this.
	 * The goal is to save memory.
	 * @param arrayList
	 * @param <V>
	 * @return
	 */
	public static <V> List<V> toSmallList(ArrayList<V> arrayList) {
		switch ( arrayList.size() ) {
			case 0:
				return Collections.EMPTY_LIST;
			case 1:
				return Collections.singletonList( arrayList.get( 0 ) );
			default:
				arrayList.trimToSize();
				return arrayList;
		}
	}

}
