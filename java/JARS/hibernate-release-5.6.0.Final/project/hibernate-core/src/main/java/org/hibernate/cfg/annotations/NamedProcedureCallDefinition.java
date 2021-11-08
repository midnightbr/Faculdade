/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg.annotations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.NamedStoredProcedureQuery;
import javax.persistence.ParameterMode;
import javax.persistence.StoredProcedureParameter;

import org.hibernate.MappingException;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.ResultSetMappingDefinition;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryReturn;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.procedure.ProcedureCallMemento;
import org.hibernate.procedure.internal.ProcedureCallMementoImpl;
import org.hibernate.procedure.internal.Util;
import org.hibernate.procedure.spi.ParameterStrategy;

import static org.hibernate.procedure.internal.ProcedureCallMementoImpl.ParameterMemento;

/**
 * Holds all the information needed from a named procedure call declaration in order to create a
 * {@link org.hibernate.procedure.internal.ProcedureCallImpl}
 *
 * @author Steve Ebersole
 *
 * @see javax.persistence.NamedStoredProcedureQuery
 */
public class NamedProcedureCallDefinition {
	private final String registeredName;
	private final String procedureName;
	private final Class[] resultClasses;
	private final String[] resultSetMappings;
	private final ParameterDefinitions parameterDefinitions;
	private final Map<String, Object> hints;

	NamedProcedureCallDefinition(NamedStoredProcedureQuery annotation) {
		this.registeredName = annotation.name();
		this.procedureName = annotation.procedureName();
		this.hints = new QueryHintDefinition( annotation.hints() ).getHintsMap();
		this.resultClasses = annotation.resultClasses();
		this.resultSetMappings = annotation.resultSetMappings();

		this.parameterDefinitions = new ParameterDefinitions( annotation.parameters(), hints );

		final boolean specifiesResultClasses = resultClasses != null && resultClasses.length > 0;
		final boolean specifiesResultSetMappings = resultSetMappings != null && resultSetMappings.length > 0;

		if ( specifiesResultClasses && specifiesResultSetMappings ) {
			throw new MappingException(
					String.format(
							"NamedStoredProcedureQuery [%s] specified both resultClasses and resultSetMappings",
							registeredName
					)
			);
		}
	}

	public String getRegisteredName() {
		return registeredName;
	}

	public String getProcedureName() {
		return procedureName;
	}

	public ProcedureCallMemento toMemento(
			final SessionFactoryImpl sessionFactory,
			final Map<String,ResultSetMappingDefinition> resultSetMappingDefinitions) {
		final List<NativeSQLQueryReturn> collectedQueryReturns = new ArrayList<NativeSQLQueryReturn>();
		final Set<String> collectedQuerySpaces = new HashSet<String>();

		final boolean specifiesResultClasses = resultClasses != null && resultClasses.length > 0;
		final boolean specifiesResultSetMappings = resultSetMappings != null && resultSetMappings.length > 0;

		if ( specifiesResultClasses ) {
			Util.resolveResultClasses(
					new Util.ResultClassesResolutionContext() {
						@Override
						public SessionFactoryImplementor getSessionFactory() {
							return sessionFactory;
						}

						@Override
						public void addQueryReturns(NativeSQLQueryReturn... queryReturns) {
							Collections.addAll( collectedQueryReturns, queryReturns );
						}

						@Override
						public void addQuerySpaces(String... spaces) {
							Collections.addAll( collectedQuerySpaces, spaces );
						}
					},
					resultClasses
			);
		}
		else if ( specifiesResultSetMappings ) {
			Util.resolveResultSetMappings(
					new Util.ResultSetMappingResolutionContext() {
						@Override
						public SessionFactoryImplementor getSessionFactory() {
							return sessionFactory;
						}

						@Override
						public ResultSetMappingDefinition findResultSetMapping(String name) {
							return resultSetMappingDefinitions.get( name );
						}

						@Override
						public void addQueryReturns(NativeSQLQueryReturn... queryReturns) {
							Collections.addAll( collectedQueryReturns, queryReturns );
						}

						@Override
						public void addQuerySpaces(String... spaces) {
							Collections.addAll( collectedQuerySpaces, spaces );
						}
					},
					resultSetMappings
			);
		}

		return new ProcedureCallMementoImpl(
				procedureName,
				collectedQueryReturns.toArray( new NativeSQLQueryReturn[ collectedQueryReturns.size() ] ),
				parameterDefinitions.getParameterStrategy(),
				parameterDefinitions.toMementos( sessionFactory ),
				collectedQuerySpaces,
				hints
		);
	}

	static class ParameterDefinitions {
		private final ParameterStrategy parameterStrategy;
		private final ParameterDefinition[] parameterDefinitions;

		ParameterDefinitions(StoredProcedureParameter[] parameters, Map<String, Object> queryHintMap) {
			if ( parameters == null || parameters.length == 0 ) {
				parameterStrategy = ParameterStrategy.POSITIONAL;
				parameterDefinitions = new ParameterDefinition[0];
			}
			else {
				parameterStrategy = StringHelper.isNotEmpty( parameters[0].name() )
						? ParameterStrategy.NAMED
						: ParameterStrategy.POSITIONAL;
				parameterDefinitions = new ParameterDefinition[ parameters.length ];

				for ( int i = 0; i < parameters.length; i++ ) {
					parameterDefinitions[i] = ParameterDefinition.from(
							parameterStrategy,
							parameters[i],
							// i+1 for the position because the apis say the numbers are 1-based, not zero
							i+1,
							queryHintMap
					);
				}
			}
		}

		public ParameterStrategy getParameterStrategy() {
			return parameterStrategy;
		}

		public List<ParameterMemento> toMementos(SessionFactoryImpl sessionFactory) {
			final List<ParameterMemento> mementos = new ArrayList<ParameterMemento>();
			for ( ParameterDefinition definition : parameterDefinitions ) {
				mementos.add(definition.toMemento( sessionFactory ));
			}
			return mementos;
		}
	}

	static class ParameterDefinition {
		private final Integer position;
		private final String name;
		private final ParameterMode parameterMode;
		private final Class type;
		private final Boolean explicitPassNullSetting;

		static ParameterDefinition from(
				ParameterStrategy parameterStrategy,
				StoredProcedureParameter parameterAnnotation,
				int adjustedPosition,
				Map<String, Object> queryHintMap) {
			// see if there was an explicit hint for this parameter in regards to NULL passing
			final Object explicitNullPassingHint;
			if ( parameterStrategy == ParameterStrategy.NAMED ) {
				explicitNullPassingHint = queryHintMap.get( AvailableSettings.PROCEDURE_NULL_PARAM_PASSING + '.' + parameterAnnotation.name() );
			}
			else {
				explicitNullPassingHint = queryHintMap.get( AvailableSettings.PROCEDURE_NULL_PARAM_PASSING + '.' + adjustedPosition );
			}

			return new ParameterDefinition(
					adjustedPosition,
					parameterAnnotation,
					interpretBoolean( explicitNullPassingHint )
			);
		}

		private static Boolean interpretBoolean(Object value) {
			if ( value == null ) {
				return null;
			}

			if ( value instanceof Boolean ) {
				return (Boolean) value;
			}

			return Boolean.valueOf( value.toString() );
		}

		ParameterDefinition(int position, StoredProcedureParameter annotation, Boolean explicitPassNullSetting) {
			this.position = position;
			this.name = normalize( annotation.name() );
			this.parameterMode = annotation.mode();
			this.type = annotation.type();
			this.explicitPassNullSetting = explicitPassNullSetting;
		}

		@SuppressWarnings("UnnecessaryUnboxing")
		public ParameterMemento toMemento(SessionFactoryImpl sessionFactory) {
			final boolean initialPassNullSetting = explicitPassNullSetting != null
					? explicitPassNullSetting.booleanValue()
					: sessionFactory.getSessionFactoryOptions().isProcedureParameterNullPassingEnabled();

			return new ParameterMemento(
					position,
					name,
					parameterMode,
					type,
					sessionFactory.getTypeResolver().heuristicType( type.getName() ),
					initialPassNullSetting
			);
		}
	}

	private static String normalize(String name) {
		return StringHelper.isNotEmpty( name ) ? name : null;
	}
}
