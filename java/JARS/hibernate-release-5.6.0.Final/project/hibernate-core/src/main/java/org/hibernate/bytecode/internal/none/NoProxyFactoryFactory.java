/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.internal.none;

import org.hibernate.bytecode.spi.BasicProxyFactory;
import org.hibernate.bytecode.spi.ProxyFactoryFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.proxy.ProxyFactory;


/**
 * When entities are enhanced in advance, proxies are not needed.
 */
final class NoProxyFactoryFactory implements ProxyFactoryFactory {

	@Override
	public ProxyFactory buildProxyFactory(SessionFactoryImplementor sessionFactory) {
		return DisallowedProxyFactory.INSTANCE;
	}

	@Override
	public BasicProxyFactory buildBasicProxyFactory(Class superClass, Class[] interfaces) {
		return new NoneBasicProxyFactory( superClass, interfaces );
	}

	@Override
	public BasicProxyFactory buildBasicProxyFactory(Class superClassOrInterface) {
		return new NoneBasicProxyFactory( superClassOrInterface );
	}
}
