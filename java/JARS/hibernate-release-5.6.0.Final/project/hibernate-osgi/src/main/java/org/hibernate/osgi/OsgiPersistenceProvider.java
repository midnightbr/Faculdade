/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.osgi;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceUnitInfo;

import org.hibernate.boot.model.TypeContributor;
import org.hibernate.boot.registry.selector.StrategyRegistrationProvider;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder;
import org.hibernate.jpa.boot.spi.IntegratorProvider;
import org.hibernate.jpa.boot.spi.StrategyRegistrationProviderList;
import org.hibernate.jpa.boot.spi.TypeContributorList;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleReference;

/**
 * Acts as the PersistenceProvider service in OSGi environments
 *
 * @author Brett Meyer
 * @author Tim Ward
 */
public class OsgiPersistenceProvider extends HibernatePersistenceProvider {
	private OsgiClassLoader osgiClassLoader;
	private OsgiJtaPlatform osgiJtaPlatform;
	private OsgiServiceUtil osgiServiceUtil;
	private Bundle requestingBundle;

	/**
	 * Constructs a OsgiPersistenceProvider
	 *
	 * @param osgiClassLoader The ClassLoader we built from OSGi Bundles
	 * @param osgiJtaPlatform The OSGi-specific JtaPlatform impl we built
	 * @param requestingBundle The OSGi Bundle requesting the PersistenceProvider
	 */
	public OsgiPersistenceProvider(
			OsgiClassLoader osgiClassLoader,
			OsgiJtaPlatform osgiJtaPlatform,
			OsgiServiceUtil osgiServiceUtil,
			Bundle requestingBundle) {
		this.osgiClassLoader = osgiClassLoader;
		this.osgiJtaPlatform = osgiJtaPlatform;
		this.osgiServiceUtil = osgiServiceUtil;
		this.requestingBundle = requestingBundle;
	}

	// TODO: Does "hibernate.classloaders" and osgiClassLoader need added to the
	// EMFBuilder somehow?

	@Override
	@SuppressWarnings("unchecked")
	public EntityManagerFactory createEntityManagerFactory(String persistenceUnitName, Map properties) {
		final Map settings = generateSettings( properties );

		// TODO: This needs tested.
		settings.put( org.hibernate.cfg.AvailableSettings.SCANNER, new OsgiScanner( requestingBundle ) );
		// TODO: This is temporary -- for PersistenceXmlParser's use of
		// ClassLoaderServiceImpl#fromConfigSettings
		settings.put( AvailableSettings.ENVIRONMENT_CLASSLOADER, osgiClassLoader );

		osgiClassLoader.addBundle( requestingBundle );

		final EntityManagerFactoryBuilder builder = getEntityManagerFactoryBuilderOrNull( persistenceUnitName, settings,
				new OSGiClassLoaderServiceImpl( osgiClassLoader, osgiServiceUtil ) );
		return builder == null ? null : builder.build();
	}

	@Override
	@SuppressWarnings("unchecked")
	public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo info, Map properties) {
		final Map settings = generateSettings( properties );

		// OSGi ClassLoaders must implement BundleReference
		final ClassLoader classLoader = info.getClassLoader();
		settings.put(
				org.hibernate.cfg.AvailableSettings.SCANNER,
				new OsgiScanner( ( (BundleReference) classLoader).getBundle() )
		);

		osgiClassLoader.addClassLoader( classLoader );

		final ClassLoader prevCL = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader( classLoader );
			return Bootstrap.getEntityManagerFactoryBuilder( info, settings,
					new OSGiClassLoaderServiceImpl( osgiClassLoader, osgiServiceUtil ) ).build();
		}
		finally {
			Thread.currentThread().setContextClassLoader( prevCL );
		}
	}

	@SuppressWarnings("unchecked")
	private Map generateSettings(Map properties) {
		final Map settings = new HashMap();
		if ( properties != null ) {
			settings.putAll( properties );
		}

		settings.put( AvailableSettings.JTA_PLATFORM, osgiJtaPlatform );

		final Integrator[] integrators = osgiServiceUtil.getServiceImpls( Integrator.class );
		final IntegratorProvider integratorProvider = new IntegratorProvider() {
			@Override
			public List<Integrator> getIntegrators() {
				return Arrays.asList( integrators );
			}
		};
		settings.put( EntityManagerFactoryBuilderImpl.INTEGRATOR_PROVIDER, integratorProvider );

		final StrategyRegistrationProvider[] strategyRegistrationProviders = osgiServiceUtil.getServiceImpls(
				StrategyRegistrationProvider.class );
		final StrategyRegistrationProviderList strategyRegistrationProviderList = new StrategyRegistrationProviderList() {
			@Override
			public List<StrategyRegistrationProvider> getStrategyRegistrationProviders() {
				return Arrays.asList( strategyRegistrationProviders );
			}
		};
		settings.put( EntityManagerFactoryBuilderImpl.STRATEGY_REGISTRATION_PROVIDERS, strategyRegistrationProviderList );

		final TypeContributor[] typeContributors = osgiServiceUtil.getServiceImpls( TypeContributor.class );
		final TypeContributorList typeContributorList = new TypeContributorList() {
			@Override
			public List<TypeContributor> getTypeContributors() {
				return Arrays.asList( typeContributors );
			}
		};
		settings.put( EntityManagerFactoryBuilderImpl.TYPE_CONTRIBUTORS, typeContributorList );
		
		return settings;
	}
}
