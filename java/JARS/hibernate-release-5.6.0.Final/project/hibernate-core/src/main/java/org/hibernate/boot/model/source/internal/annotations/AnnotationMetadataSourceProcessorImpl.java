/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.internal.annotations;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;

import org.hibernate.annotations.common.reflection.MetadataProviderInjector;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.boot.AttributeConverterInfo;
import org.hibernate.boot.internal.MetadataBuildingContextRootImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappings;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.model.source.spi.MetadataSourceProcessor;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.JpaOrmXmlPersistenceUnitDefaultAware;
import org.hibernate.boot.spi.JpaOrmXmlPersistenceUnitDefaultAware.JpaOrmXmlPersistenceUnitDefaults;
import org.hibernate.boot.spi.MetadataBuildingOptions;
import org.hibernate.cfg.AnnotationBinder;
import org.hibernate.cfg.InheritanceState;
import org.hibernate.cfg.annotations.reflection.AttributeConverterDefinitionCollector;
import org.hibernate.cfg.annotations.reflection.internal.JPAXMLOverriddenMetadataProvider;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;

import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class AnnotationMetadataSourceProcessorImpl implements MetadataSourceProcessor {
	private static final Logger log = Logger.getLogger( AnnotationMetadataSourceProcessorImpl.class );

	private final MetadataBuildingContextRootImpl rootMetadataBuildingContext;

	@SuppressWarnings("FieldCanBeLocal")
	private final IndexView jandexView;

	private final ReflectionManager reflectionManager;

	private final LinkedHashSet<String> annotatedPackages = new LinkedHashSet<String>();

	private final List<XClass> xClasses = new ArrayList<XClass>();
	private final ClassLoaderService classLoaderService;

	public AnnotationMetadataSourceProcessorImpl(
			ManagedResources managedResources,
			final MetadataBuildingContextRootImpl rootMetadataBuildingContext,
			IndexView jandexView) {
		this.rootMetadataBuildingContext = rootMetadataBuildingContext;
		this.jandexView = jandexView;

		this.reflectionManager = rootMetadataBuildingContext.getBootstrapContext().getReflectionManager();

		if ( CollectionHelper.isNotEmpty( managedResources.getAnnotatedPackageNames() ) ) {
			annotatedPackages.addAll( managedResources.getAnnotatedPackageNames() );
		}

		final AttributeConverterManager attributeConverterManager = new AttributeConverterManager( rootMetadataBuildingContext );
		this.classLoaderService = rootMetadataBuildingContext.getBuildingOptions().getServiceRegistry().getService( ClassLoaderService.class );

		MetadataBuildingOptions metadataBuildingOptions = rootMetadataBuildingContext.getBuildingOptions();
		if ( metadataBuildingOptions.isXmlMappingEnabled() ) {
			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			// Ewww.  This is temporary until we migrate to Jandex + StAX for annotation binding
			final JPAXMLOverriddenMetadataProvider jpaMetadataProvider = (JPAXMLOverriddenMetadataProvider) ( (MetadataProviderInjector) reflectionManager )
					.getMetadataProvider();
			for ( Binding<?> xmlBinding : managedResources.getXmlMappingBindings() ) {
				Object root = xmlBinding.getRoot();
				if ( !(root instanceof JaxbEntityMappings) ) {
					continue;
				}
				JaxbEntityMappings entityMappings = (JaxbEntityMappings) xmlBinding.getRoot();

				final List<String> classNames = jpaMetadataProvider.getXMLContext().addDocument( entityMappings );
				for ( String className : classNames ) {
					xClasses.add( toXClass( className, reflectionManager, classLoaderService ) );
				}
			}
			jpaMetadataProvider.getXMLContext().applyDiscoveredAttributeConverters( attributeConverterManager );
			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		}

		for ( String className : managedResources.getAnnotatedClassNames() ) {
			final Class annotatedClass = classLoaderService.classForName( className );
			categorizeAnnotatedClass( annotatedClass, attributeConverterManager, classLoaderService );
		}

		for ( Class annotatedClass : managedResources.getAnnotatedClassReferences() ) {
			categorizeAnnotatedClass( annotatedClass, attributeConverterManager, classLoaderService );
		}
	}

	private void categorizeAnnotatedClass(Class annotatedClass, AttributeConverterManager attributeConverterManager, ClassLoaderService cls) {
		final XClass xClass = reflectionManager.toXClass( annotatedClass );
		// categorize it, based on assumption it does not fall into multiple categories
		if ( xClass.isAnnotationPresent( Converter.class ) ) {
			//noinspection unchecked
			attributeConverterManager.addAttributeConverter( annotatedClass );
		}
		else if ( xClass.isAnnotationPresent( Entity.class )
				|| xClass.isAnnotationPresent( MappedSuperclass.class ) ) {
			xClasses.add( xClass );
		}
		else if ( xClass.isAnnotationPresent( Embeddable.class ) ) {
			xClasses.add( xClass );
		}
		else {
			log.debugf( "Encountered a non-categorized annotated class [%s]; ignoring", annotatedClass.getName() );
		}
	}

	@SuppressWarnings("deprecation")
	private XClass toXClass(String className, ReflectionManager reflectionManager, ClassLoaderService cls) {
		return reflectionManager.toXClass( cls.classForName( className ) );
	}

	@Override
	public void prepare() {
		// use any persistence-unit-defaults defined in orm.xml
		( (JpaOrmXmlPersistenceUnitDefaultAware) rootMetadataBuildingContext.getBuildingOptions() ).apply(
				new JpaOrmXmlPersistenceUnitDefaults() {
					final Map persistenceUnitDefaults = reflectionManager.getDefaults();

					@Override
					public String getDefaultSchemaName() {
						return StringHelper.nullIfEmpty( (String) persistenceUnitDefaults.get( "schema" ) );
					}

					@Override
					public String getDefaultCatalogName() {
						return StringHelper.nullIfEmpty( (String) persistenceUnitDefaults.get( "catalog" ) );
					}

					@Override
					public boolean shouldImplicitlyQuoteIdentifiers() {
						final Object isDelimited = persistenceUnitDefaults.get( "delimited-identifier" );
						return isDelimited != null && isDelimited == Boolean.TRUE;
					}
				}
		);

		rootMetadataBuildingContext.getMetadataCollector().getDatabase().adjustDefaultNamespace(
				rootMetadataBuildingContext.getBuildingOptions().getMappingDefaults().getImplicitCatalogName(),
				rootMetadataBuildingContext.getBuildingOptions().getMappingDefaults().getImplicitSchemaName()
		);

		AnnotationBinder.bindDefaults( rootMetadataBuildingContext );
		for ( String annotatedPackage : annotatedPackages ) {
			AnnotationBinder.bindPackage( classLoaderService, annotatedPackage, rootMetadataBuildingContext );
		}
	}

	@Override
	public void processTypeDefinitions() {

	}

	@Override
	public void processQueryRenames() {

	}

	@Override
	public void processNamedQueries() {

	}

	@Override
	public void processAuxiliaryDatabaseObjectDefinitions() {

	}

	@Override
	public void processIdentifierGenerators() {

	}

	@Override
	public void processFilterDefinitions() {

	}

	@Override
	public void processFetchProfiles() {

	}

	@Override
	public void prepareForEntityHierarchyProcessing() {

	}

	@Override
	public void processEntityHierarchies(Set<String> processedEntityNames) {
		final List<XClass> orderedClasses = orderAndFillHierarchy( xClasses );
		Map<XClass, InheritanceState> inheritanceStatePerClass = AnnotationBinder.buildInheritanceStates(
				orderedClasses,
				rootMetadataBuildingContext
		);


		for ( XClass clazz : orderedClasses ) {
			if ( processedEntityNames.contains( clazz.getName() ) ) {
				log.debugf( "Skipping annotated class processing of entity [%s], as it has already been processed", clazz );
				continue;
			}

			AnnotationBinder.bindClass( clazz, inheritanceStatePerClass, rootMetadataBuildingContext );
			AnnotationBinder.bindFetchProfilesForClass( clazz, rootMetadataBuildingContext );
			processedEntityNames.add( clazz.getName() );
		}
	}

	private List<XClass> orderAndFillHierarchy(List<XClass> original) {
		List<XClass> copy = new ArrayList<>( original.size() );
		insertMappedSuperclasses( original, copy );

		// order the hierarchy
		List<XClass> workingCopy = new ArrayList<XClass>( copy );
		List<XClass> newList = new ArrayList<XClass>( copy.size() );
		while ( workingCopy.size() > 0 ) {
			XClass clazz = workingCopy.get( 0 );
			orderHierarchy( workingCopy, newList, copy, clazz );
		}
		return newList;
	}

	private void insertMappedSuperclasses(List<XClass> original, List<XClass> copy) {
		final boolean debug = log.isDebugEnabled();
		for ( XClass clazz : original ) {
			if ( clazz.isAnnotationPresent( javax.persistence.MappedSuperclass.class ) ) {
				if ( debug ) {
					log.debugf(
							"Skipping explicit MappedSuperclass %s, the class will be discovered analyzing the implementing class",
							clazz
					);
				}
			}
			else {
				copy.add( clazz );
				XClass superClass = clazz.getSuperclass();
				while ( superClass != null
						&& !reflectionManager.equals( superClass, Object.class )
						&& !copy.contains( superClass ) ) {
					if ( superClass.isAnnotationPresent( Entity.class )
							|| superClass.isAnnotationPresent( javax.persistence.MappedSuperclass.class ) ) {
						copy.add( superClass );
					}
					superClass = superClass.getSuperclass();
				}
			}
		}
	}

	private void orderHierarchy(List<XClass> copy, List<XClass> newList, List<XClass> original, XClass clazz) {
		if ( clazz == null || reflectionManager.equals( clazz, Object.class ) ) {
			return;
		}
		//process superclass first
		orderHierarchy( copy, newList, original, clazz.getSuperclass() );
		if ( original.contains( clazz ) ) {
			if ( !newList.contains( clazz ) ) {
				newList.add( clazz );
			}
			copy.remove( clazz );
		}
	}

	@Override
	public void postProcessEntityHierarchies() {
		for ( String annotatedPackage : annotatedPackages ) {
			AnnotationBinder.bindFetchProfilesForPackage( classLoaderService, annotatedPackage, rootMetadataBuildingContext );
		}
	}

	@Override
	public void processResultSetMappings() {

	}

	@Override
	public void finishUp() {

	}

	private static class AttributeConverterManager implements AttributeConverterDefinitionCollector {
		private final MetadataBuildingContextRootImpl rootMetadataBuildingContext;

		public AttributeConverterManager(MetadataBuildingContextRootImpl rootMetadataBuildingContext) {
			this.rootMetadataBuildingContext = rootMetadataBuildingContext;
		}

		@Override
		public void addAttributeConverter(AttributeConverterInfo info) {
			rootMetadataBuildingContext.getMetadataCollector().addAttributeConverter(
					info.toConverterDescriptor( rootMetadataBuildingContext )
			);
		}

		@Override
		public void addAttributeConverter(ConverterDescriptor descriptor) {
			rootMetadataBuildingContext.getMetadataCollector().addAttributeConverter( descriptor );
		}

		public void addAttributeConverter(Class<? extends AttributeConverter> converterClass) {
			rootMetadataBuildingContext.getMetadataCollector().addAttributeConverter( converterClass );
		}
	}
}
