/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

import java.util.Iterator;

import org.hibernate.bytecode.enhance.spi.interceptor.BytecodeLazyAttributeInterceptor;
import org.hibernate.bytecode.enhance.spi.interceptor.EnhancementAsProxyLazinessInterceptor;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.HibernateIterator;
import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;

/**
 * <ul>
 * <li>Provides access to the full range of Hibernate built-in types. <tt>Type</tt>
 * instances may be used to bind values to query parameters.
 * <li>A factory for new <tt>Blob</tt>s and <tt>Clob</tt>s.
 * <li>Defines static methods for manipulation of proxies.
 * </ul>
 *
 * @author Gavin King
 * @see java.sql.Clob
 * @see java.sql.Blob
 * @see org.hibernate.type.Type
 */

public final class Hibernate {
	/**
	 * Cannot be instantiated.
	 */
	private Hibernate() {
		throw new UnsupportedOperationException();
	}


	/**
	 * Force initialization of a proxy or persistent collection.
	 * <p/>
	 * Note: This only ensures initialization of a proxy object or collection;
	 * it is not guaranteed that the elements INSIDE the collection will be initialized/materialized.
	 *
	 * @param proxy a persistable object, proxy, persistent collection or <tt>null</tt>
	 * @throws HibernateException if we can't initialize the proxy at this time, eg. the <tt>Session</tt> was closed
	 */
	public static void initialize(Object proxy) throws HibernateException {
		if ( proxy == null ) {
			return;
		}

		if ( proxy instanceof HibernateProxy ) {
			( (HibernateProxy) proxy ).getHibernateLazyInitializer().initialize();
		}
		else if ( proxy instanceof PersistentCollection ) {
			( (PersistentCollection) proxy ).forceInitialization();
		}
		else if ( proxy instanceof PersistentAttributeInterceptable ) {
			final PersistentAttributeInterceptable interceptable = (PersistentAttributeInterceptable) proxy;
			final PersistentAttributeInterceptor interceptor = interceptable.$$_hibernate_getInterceptor();
			if ( interceptor instanceof EnhancementAsProxyLazinessInterceptor ) {
				( (EnhancementAsProxyLazinessInterceptor) interceptor ).forceInitialize( proxy, null );
			}
		}
	}

	/**
	 * Check if the proxy or persistent collection is initialized.
	 *
	 * @param proxy a persistable object, proxy, persistent collection or <tt>null</tt>
	 * @return true if the argument is already initialized, or is not a proxy or collection
	 */
	@SuppressWarnings("SimplifiableIfStatement")
	public static boolean isInitialized(Object proxy) {
		if ( proxy instanceof HibernateProxy ) {
			return !( (HibernateProxy) proxy ).getHibernateLazyInitializer().isUninitialized();
		}
		else if ( proxy instanceof PersistentAttributeInterceptable ) {
			final PersistentAttributeInterceptor interceptor = ( (PersistentAttributeInterceptable) proxy ).$$_hibernate_getInterceptor();
			if ( interceptor instanceof EnhancementAsProxyLazinessInterceptor ) {
				return false;
			}
			return true;
		}
		else if ( proxy instanceof PersistentCollection ) {
			return ( (PersistentCollection) proxy ).wasInitialized();
		}
		else {
			return true;
		}
	}

	/**
	 * Get the true, underlying class of a proxied persistent class. This operation
	 * will initialize a proxy by side-effect.
	 *
	 * @param proxy a persistable object or proxy
	 * @return the true class of the instance
	 * @throws HibernateException
	 */
	public static Class getClass(Object proxy) {
		if ( proxy instanceof HibernateProxy ) {
			return ( (HibernateProxy) proxy ).getHibernateLazyInitializer()
					.getImplementation()
					.getClass();
		}
		else {
			return proxy.getClass();
		}
	}

	/**
	 * Obtain a lob creator for the given session.
	 *
	 * @param session The session for which to obtain a lob creator
	 *
	 * @return The log creator reference
	 */
	public static LobCreator getLobCreator(Session session) {
		return getLobCreator( (SessionImplementor) session );
	}

	/**
	 * Obtain a lob creator for the given session.
	 *
	 * @param session The session for which to obtain a lob creator
	 *
	 * @return The log creator reference
	 */
	public static LobCreator getLobCreator(SharedSessionContractImplementor session) {
		return session.getFactory()
				.getServiceRegistry()
				.getService( JdbcServices.class )
				.getLobCreator( session );
	}

	/**
	 * Obtain a lob creator for the given session.
	 *
	 * @param session The session for which to obtain a lob creator
	 *
	 * @return The log creator reference
	 */
	public static LobCreator getLobCreator(SessionImplementor session) {
		return session.getFactory()
				.getServiceRegistry()
				.getService( JdbcServices.class )
				.getLobCreator( session );
	}

	/**
	 * Close an {@link Iterator} instances obtained from {@link org.hibernate.Query#iterate()} immediately
	 * instead of waiting until the session is closed or disconnected.
	 *
	 * @param iterator an Iterator created by iterate()
	 *
	 * @throws HibernateException Indicates a problem closing the Hibernate iterator.
	 * @throws IllegalArgumentException If the Iterator is not a "Hibernate Iterator".
	 *
	 * @see Query#iterate()
	 */
	public static void close(Iterator iterator) throws HibernateException {
		if ( iterator instanceof HibernateIterator ) {
			( (HibernateIterator) iterator ).close();
		}
		else {
			throw new IllegalArgumentException( "not a Hibernate iterator" );
		}
	}

	/**
	 * Check if the property is initialized. If the named property does not exist
	 * or is not persistent, this method always returns <tt>true</tt>.
	 *
	 * @param proxy The potential proxy
	 * @param propertyName the name of a persistent attribute of the object
	 * @return true if the named property of the object is not listed as uninitialized; false otherwise
	 */
	public static boolean isPropertyInitialized(Object proxy, String propertyName) {
		final Object entity;
		if ( proxy instanceof HibernateProxy ) {
			final LazyInitializer li = ( (HibernateProxy) proxy ).getHibernateLazyInitializer();
			if ( li.isUninitialized() ) {
				return false;
			}
			else {
				entity = li.getImplementation();
			}
		}
		else {
			entity = proxy;
		}

		if ( entity instanceof PersistentAttributeInterceptable ) {
			PersistentAttributeInterceptor interceptor = ( (PersistentAttributeInterceptable) entity ).$$_hibernate_getInterceptor();
			if ( interceptor instanceof BytecodeLazyAttributeInterceptor ) {
				return ( (BytecodeLazyAttributeInterceptor) interceptor ).isAttributeLoaded( propertyName );
			}
		}

		return true;
	}

    /**
     * Unproxies a {@link HibernateProxy}. If the proxy is uninitialized, it automatically triggers an initialization.
     * In case the supplied object is null or not a proxy, the object will be returned as-is.
     *
     * @param proxy the {@link HibernateProxy} to be unproxied
     * @return the proxy's underlying implementation object, or the supplied object otherwise
     */
	public static Object unproxy(Object proxy) {
		if ( proxy instanceof HibernateProxy ) {
			HibernateProxy hibernateProxy = (HibernateProxy) proxy;
			LazyInitializer initializer = hibernateProxy.getHibernateLazyInitializer();
			return initializer.getImplementation();
		}
		else {
			return proxy;
		}
	}

	/**
	 * Unproxies a {@link HibernateProxy}. If the proxy is uninitialized, it automatically triggers an initialization.
	 * In case the supplied object is null or not a proxy, the object will be returned as-is.
	 *
	 * @param proxy the {@link HibernateProxy} to be unproxied
	 * @param entityClass the entity type
	 * @return the proxy's underlying implementation object, or the supplied object otherwise
	 */
	public static <T> T unproxy(T proxy, Class<T> entityClass) {
		return entityClass.cast( unproxy( proxy ) );
	}
}
