/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.internal;

import org.hibernate.boot.model.naming.ObjectNameNormalizer;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.ClassLoaderAccess;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MappingDefaults;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.MetadataBuildingOptions;

/**
 * @author Steve Ebersole
 */
public class MetadataBuildingContextRootImpl implements MetadataBuildingContext {
	private final BootstrapContext bootstrapContext;
	private final MetadataBuildingOptions options;
	private final MappingDefaults mappingDefaults;
	private final InFlightMetadataCollector metadataCollector;
	private final ObjectNameNormalizer objectNameNormalizer;

	public MetadataBuildingContextRootImpl(
			BootstrapContext bootstrapContext,
			MetadataBuildingOptions options,
			InFlightMetadataCollector metadataCollector) {
		this.bootstrapContext = bootstrapContext;
		this.options = options;
		this.mappingDefaults = options.getMappingDefaults();
		this.metadataCollector = metadataCollector;
		this.objectNameNormalizer = new ObjectNameNormalizer() {
			@Override
			protected MetadataBuildingContext getBuildingContext() {
				return MetadataBuildingContextRootImpl.this;
			}
		};
	}

	@Override
	public BootstrapContext getBootstrapContext() {
		return bootstrapContext;
	}

	@Override
	public MetadataBuildingOptions getBuildingOptions() {
		return options;
	}

	@Override
	public MappingDefaults getMappingDefaults() {
		return mappingDefaults;
	}

	@Override
	public InFlightMetadataCollector getMetadataCollector() {
		return metadataCollector;
	}

	@Override
	public ClassLoaderAccess getClassLoaderAccess() {
		return bootstrapContext.getClassLoaderAccess();
	}

	@Override
	public ObjectNameNormalizer getObjectNameNormalizer() {
		return objectNameNormalizer;
	}
}
