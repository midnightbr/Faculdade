/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.service.internal;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.SessionFactoryServiceContributor;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;
import org.hibernate.service.spi.SessionFactoryServiceRegistryFactory;

/**
 * Acts as a service in the {@link org.hibernate.boot.registry.internal.StandardServiceRegistryImpl} whose
 * function is to act as a factory for {@link SessionFactoryServiceRegistryImpl} implementations.
 *
 * @author Steve Ebersole
 */
public class SessionFactoryServiceRegistryFactoryImpl implements SessionFactoryServiceRegistryFactory {
	private final ServiceRegistryImplementor theBasicServiceRegistry;

	public SessionFactoryServiceRegistryFactoryImpl(ServiceRegistryImplementor theBasicServiceRegistry) {
		this.theBasicServiceRegistry = theBasicServiceRegistry;
	}

	@Override
	public SessionFactoryServiceRegistry buildServiceRegistry(
			SessionFactoryImplementor sessionFactory,
			SessionFactoryOptions options) {
		final ClassLoaderService cls = options.getServiceRegistry().getService( ClassLoaderService.class );
		final SessionFactoryServiceRegistryBuilderImpl builder = new SessionFactoryServiceRegistryBuilderImpl( theBasicServiceRegistry );

		for ( SessionFactoryServiceContributor contributor : cls.loadJavaServices( SessionFactoryServiceContributor.class ) ) {
			contributor.contribute( builder );
		}

		return builder.buildSessionFactoryServiceRegistry( sessionFactory, options );
	}
}
