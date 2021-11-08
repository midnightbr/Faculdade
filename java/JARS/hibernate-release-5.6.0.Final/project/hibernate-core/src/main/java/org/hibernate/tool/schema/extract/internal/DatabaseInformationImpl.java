/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.extract.internal;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.QualifiedSequenceName;
import org.hibernate.boot.model.relational.QualifiedTableName;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.resource.transaction.spi.DdlTransactionIsolator;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.schema.extract.spi.DatabaseInformation;
import org.hibernate.tool.schema.extract.spi.ExtractionContext;
import org.hibernate.tool.schema.extract.spi.InformationExtractor;
import org.hibernate.tool.schema.extract.spi.NameSpaceTablesInformation;
import org.hibernate.tool.schema.extract.spi.SequenceInformation;
import org.hibernate.tool.schema.extract.spi.TableInformation;
import org.hibernate.tool.schema.spi.SchemaManagementTool;

/**
 * @author Steve Ebersole
 */
public class DatabaseInformationImpl
		implements DatabaseInformation, ExtractionContext.DatabaseObjectAccess {
	private final JdbcEnvironment jdbcEnvironment;
	private final ExtractionContext extractionContext;
	private final InformationExtractor extractor;

	private final Map<QualifiedSequenceName, SequenceInformation> sequenceInformationMap = new HashMap<>();

	public DatabaseInformationImpl(
			ServiceRegistry serviceRegistry,
			JdbcEnvironment jdbcEnvironment,
			DdlTransactionIsolator ddlTransactionIsolator,
			Namespace.Name defaultNamespace,
			SchemaManagementTool tool) throws SQLException {
		this.jdbcEnvironment = jdbcEnvironment;
		this.extractionContext = tool.getExtractionTool().createExtractionContext(
				serviceRegistry,
				jdbcEnvironment,
				ddlTransactionIsolator,
				defaultNamespace.getCatalog(),
				defaultNamespace.getSchema(),
				this
		);

		this.extractor = tool.getExtractionTool().createInformationExtractor( extractionContext );

		// because we do not have defined a way to locate sequence info by name
		initializeSequences();
	}

	private void initializeSequences() throws SQLException {
		Iterable<SequenceInformation> itr = jdbcEnvironment.getDialect()
				.getSequenceInformationExtractor()
				.extractMetadata( extractionContext );
		for ( SequenceInformation sequenceInformation : itr ) {
			sequenceInformationMap.put(
					// for now, follow the legacy behavior of storing just the
					// unqualified sequence name.
					new QualifiedSequenceName(
							null,
							null,
							sequenceInformation.getSequenceName().getSequenceName()
					),
					sequenceInformation
			);
		}
	}

	@Override
	public boolean catalogExists(Identifier catalog) {
		return extractor.catalogExists( catalog );
	}

	@Override
	public boolean schemaExists(Namespace.Name namespace) {
		return extractor.schemaExists( namespace.getCatalog(), namespace.getSchema() );
	}

	@Override
	public TableInformation getTableInformation(
			Identifier catalogName,
			Identifier schemaName,
			Identifier tableName) {
		return getTableInformation( new QualifiedTableName( catalogName, schemaName, tableName ) );
	}

	@Override
	public TableInformation getTableInformation(
			Namespace.Name namespace,
			Identifier tableName) {
		return getTableInformation( new QualifiedTableName( namespace, tableName ) );
	}

	@Override
	public TableInformation getTableInformation(QualifiedTableName tableName) {
		if ( tableName.getObjectName() == null ) {
			throw new IllegalArgumentException( "Passed table name cannot be null" );
		}

		return extractor.getTable(
				tableName.getCatalogName(),
				tableName.getSchemaName(),
				tableName.getTableName()
		);
	}

	@Override
	public NameSpaceTablesInformation getTablesInformation(Namespace namespace) {
		return extractor.getTables( namespace.getPhysicalName().getCatalog(), namespace.getPhysicalName().getSchema() );
	}

	@Override
	public SequenceInformation getSequenceInformation(
			Identifier catalogName,
			Identifier schemaName,
			Identifier sequenceName) {
		return getSequenceInformation( new QualifiedSequenceName( catalogName, schemaName, sequenceName ) );
	}

	@Override
	public SequenceInformation getSequenceInformation(Namespace.Name schemaName, Identifier sequenceName) {
		return getSequenceInformation( new QualifiedSequenceName( schemaName, sequenceName ) );
	}

	@Override
	public SequenceInformation getSequenceInformation(QualifiedSequenceName sequenceName) {
		return locateSequenceInformation( sequenceName );
	}

	@Override
	public void cleanup() {
		extractionContext.cleanup();
	}

	@Override
	public TableInformation locateTableInformation(QualifiedTableName tableName) {
		return getTableInformation( tableName );
	}

	@Override
	public SequenceInformation locateSequenceInformation(QualifiedSequenceName sequenceName) {
		// again, follow legacy behavior
		if ( sequenceName.getCatalogName() != null || sequenceName.getSchemaName() != null ) {
			sequenceName = new QualifiedSequenceName( null, null, sequenceName.getSequenceName() );
		}

		return sequenceInformationMap.get( sequenceName );
	}
}
