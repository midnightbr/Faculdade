/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.service.internal;

import java.util.List;

import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.service.Service;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceBinding;
import org.hibernate.service.spi.ServiceInitiator;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.SessionFactoryServiceInitiator;
import org.hibernate.service.spi.SessionFactoryServiceInitiatorContext;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class SessionFactoryServiceRegistryImpl
		extends AbstractServiceRegistryImpl
		implements SessionFactoryServiceRegistry, SessionFactoryServiceInitiatorContext {

	private static final Logger log = Logger.getLogger( SessionFactoryServiceRegistryImpl.class );

	private final SessionFactoryOptions sessionFactoryOptions;
	private final SessionFactoryImplementor sessionFactory;

	@SuppressWarnings({"unchecked", "rawtypes"})
	public SessionFactoryServiceRegistryImpl(
			ServiceRegistryImplementor parent,
			List<SessionFactoryServiceInitiator> initiators,
			List<ProvidedService> providedServices,
			SessionFactoryImplementor sessionFactory,
			SessionFactoryOptions sessionFactoryOptions) {
		super( parent );

		this.sessionFactory = sessionFactory;
		this.sessionFactoryOptions = sessionFactoryOptions;

		// for now, just use the standard initiator list
		for ( SessionFactoryServiceInitiator initiator : initiators ) {
			// create the bindings up front to help identify to which registry services belong
			createServiceBinding( initiator );
		}

		for ( ProvidedService providedService : providedServices ) {
			createServiceBinding( providedService );
		}
	}

	@Override
	public <R extends Service> R initiateService(ServiceInitiator<R> serviceInitiator) {
		SessionFactoryServiceInitiator<R> sessionFactoryServiceInitiator = (SessionFactoryServiceInitiator<R>) serviceInitiator;
		return sessionFactoryServiceInitiator.initiateService( this );
	}

	@Override
	public <R extends Service> void configureService(ServiceBinding<R> serviceBinding) {
		if ( serviceBinding.getService() instanceof Configurable ) {
			( (Configurable) serviceBinding.getService() ).configure( getService( ConfigurationService.class ).getSettings() );
		}
	}

	@Override
	public SessionFactoryImplementor getSessionFactory() {
		return sessionFactory;
	}

	@Override
	public SessionFactoryOptions getSessionFactoryOptions() {
		return sessionFactoryOptions;
	}

	@Override
	public ServiceRegistryImplementor getServiceRegistry() {
		return this;
	}

	@Override
	public <R extends Service> R getService(Class<R> serviceRole) {
		if ( serviceRole.equals( EventListenerRegistry.class ) ) {
			log.debug(
					"EventListenerRegistry access via ServiceRegistry is deprecated.  " +
							"Use `sessionFactory.getEventEngine().getListenerRegistry()` instead"
			);

			//noinspection unchecked
			return (R) sessionFactory.getEventEngine().getListenerRegistry();
		}

		return super.getService( serviceRole );
	}
}
