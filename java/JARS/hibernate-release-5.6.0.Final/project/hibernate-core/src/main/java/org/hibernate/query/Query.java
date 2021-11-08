/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Parameter;
import javax.persistence.TemporalType;
import javax.persistence.TypedQuery;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.RootGraph;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.type.BigDecimalType;
import org.hibernate.type.BigIntegerType;
import org.hibernate.type.BinaryType;
import org.hibernate.type.BooleanType;
import org.hibernate.type.ByteType;
import org.hibernate.type.CharacterType;
import org.hibernate.type.DateType;
import org.hibernate.type.DoubleType;
import org.hibernate.type.FloatType;
import org.hibernate.type.IntegerType;
import org.hibernate.type.LocaleType;
import org.hibernate.type.LongType;
import org.hibernate.type.ShortType;
import org.hibernate.type.StringType;
import org.hibernate.type.TextType;
import org.hibernate.type.TimeType;
import org.hibernate.type.TimestampType;
import org.hibernate.type.Type;

/**
 * Represents an HQL/JPQL query or a compiled Criteria query.  Also acts as the Hibernate
 * extension to the JPA Query/TypedQuery contract
 * <p/>
 * NOTE: {@link org.hibernate.Query} is deprecated, and slated for removal in 6.0.
 * For the time being we leave all methods defined on {@link org.hibernate.Query}
 * rather than here because it was previously the public API so we want to leave that
 * unchanged in 5.x.  For 6.0 we will move those methods here and then delete that class.
 *
 * @author Steve Ebersole
 * @author Gavin King
 */
@SuppressWarnings("UnusedDeclaratiqon")
public interface Query<R> extends TypedQuery<R>, org.hibernate.Query<R>, CommonQueryContract {
	/**
	 * Get the QueryProducer this Query originates from.
	 */
	QueryProducer getProducer();

	Optional<R> uniqueResultOptional();

	/**
	 * Retrieve a Stream over the query results.
	 * <p/>
	 * In the initial implementation (5.2) this returns a simple sequential Stream. The plan
	 * is to return a smarter stream in 6.x leveraging the SQM model.
	 *
	 * <p>
	 *
	 * You should call {@link java.util.stream.Stream#close()} after processing the stream
	 * so that the underlying resources are deallocated right away.
	 *
	 * @return The results Stream
	 *
	 * @since 5.2
	 */
	Stream<R> stream();

	/**
	 * Apply the given graph using the given semantic
	 *
	 * @param graph The graph the apply.
	 * @param semantic The semantic to use when applying the graph
	 *
	 * @return this - for method chaining
	 */
	Query<R> applyGraph(RootGraph graph, GraphSemantic semantic);

	/**
	 * Apply the given graph using {@linkplain GraphSemantic#FETCH fetch semantics}
	 *
	 * @apiNote This method calls {@link #applyGraph(RootGraph, GraphSemantic)} using
	 * {@link GraphSemantic#FETCH} as the semantic
	 */
	default Query<R> applyFetchGraph(RootGraph graph) {
		return applyGraph( graph, GraphSemantic.FETCH );
	}

	/**
	 * Apply the given graph using {@linkplain GraphSemantic#LOAD load semantics}
	 *
	 * @apiNote This method calls {@link #applyGraph(RootGraph, GraphSemantic)} using
	 * {@link GraphSemantic#LOAD} as the semantic
	 */
	default Query<R> applyLoadGraph(RootGraph graph) {
		return applyGraph( graph, GraphSemantic.LOAD );
	}


	Query<R> setParameter(Parameter<Instant> param, Instant value, TemporalType temporalType);

	Query<R> setParameter(Parameter<LocalDateTime> param, LocalDateTime value, TemporalType temporalType);

	Query<R> setParameter(Parameter<ZonedDateTime> param, ZonedDateTime value, TemporalType temporalType);

	Query<R> setParameter(Parameter<OffsetDateTime> param, OffsetDateTime value, TemporalType temporalType);

	Query<R> setParameter(String name, Instant value, TemporalType temporalType);

	Query<R> setParameter(String name, LocalDateTime value, TemporalType temporalType);

	Query<R> setParameter(String name, ZonedDateTime value, TemporalType temporalType);

	Query<R> setParameter(String name, OffsetDateTime value, TemporalType temporalType);

	Query<R> setParameter(int position, Instant value, TemporalType temporalType);

	Query<R> setParameter(int position, LocalDateTime value, TemporalType temporalType);

	Query<R> setParameter(int position, ZonedDateTime value, TemporalType temporalType);

	Query<R> setParameter(int position, OffsetDateTime value, TemporalType temporalType);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Overrides for methods we do not want deprecated

	@Override
	ScrollableResults scroll();

	@Override
	ScrollableResults scroll(ScrollMode scrollMode);

	@Override
	List<R> list();

	default List<R> getResultList() {
		return list();
	}

	@Override
	R uniqueResult();

	default R getSingleResult() {
		return uniqueResult();
	}

	@Override
	FlushMode getHibernateFlushMode();

	@Override
	CacheMode getCacheMode();

	@Override
	String getCacheRegion();

	@Override
	Integer getFetchSize();

	@Override
	LockOptions getLockOptions();

	@Override
	String getComment();

	@Override
	String getQueryString();

	@Override
	ParameterMetadata getParameterMetadata();
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


	// covariant overrides

	@Override
	Query<R> setMaxResults(int maxResult);

	@Override
	Query<R> setFirstResult(int startPosition);

	@Override
	Query<R> setHint(String hintName, Object value);

	@Override
	<T> Query<R> setParameter(Parameter<T> param, T value);

	@Override
	Query<R> setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType);

	@Override
	Query<R> setParameter(Parameter<Date> param, Date value, TemporalType temporalType);

	@Override
	Query<R> setParameter(String name, Object value);

	@Override
	Query<R> setParameter(String name, Object val, Type type);

	@Override
	Query<R> setParameter(String name, Calendar value, TemporalType temporalType);

	@Override
	Query<R> setParameter(String name, Date value, TemporalType temporalType);

	@Override
	Query<R> setParameter(int position, Object value);

	@Override
	Query<R> setParameter(int position, Calendar value, TemporalType temporalType);

	@Override
	Query<R> setParameter(int position, Date value, TemporalType temporalType);

	@Override
	<T> Query<R> setParameter(QueryParameter<T> parameter, T val);

	@Override
	<P> Query<R> setParameter(int position, P val, TemporalType temporalType);

	@Override
	<P> Query<R> setParameter(QueryParameter<P> parameter, P val, Type type);

	@Override
	Query<R> setParameter(int position, Object val, Type type);

	@Override
	<P> Query<R> setParameter(QueryParameter<P> parameter, P val, TemporalType temporalType);

	@Override
	<P> Query<R> setParameter(String name, P val, TemporalType temporalType);

	@Override
	Query<R> setFlushMode(FlushModeType flushMode);

	@Override
	Query<R> setLockMode(LockModeType lockMode);

	@Override
	Query<R> setReadOnly(boolean readOnly);

	@Override
	Query<R> setHibernateFlushMode(FlushMode flushMode);

	@Override
	Query<R> setCacheMode(CacheMode cacheMode);

	@Override
	Query<R> setCacheable(boolean cacheable);

	@Override
	Query<R> setCacheRegion(String cacheRegion);

	@Override
	Query<R> setTimeout(int timeout);

	@Override
	Query<R> setFetchSize(int fetchSize);

	@Override
	Query<R> setLockOptions(LockOptions lockOptions);

	@Override
	Query<R> setLockMode(String alias, LockMode lockMode);

	@Override
	Query<R> setComment(String comment);

	@Override
	Query<R> addQueryHint(String hint);

	@Override
	<P> Query<R> setParameterList(QueryParameter<P> parameter, Collection<P> values);

	@Override
	Query<R> setParameterList(String name, Collection values);

	@Override
	Query<R> setParameterList(String name, Collection values, Type type);

	@Override
	Query<R> setParameterList(String name, Object[] values, Type type);

	@Override
	Query<R> setParameterList(String name, Object[] values);

	@Override
	Query<R> setProperties(Object bean);

	@Override
	Query<R> setProperties(Map bean);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// deprecations

	/**
	 * (Re)set the current FlushMode in effect for this query.
	 *
	 * @param flushMode The new FlushMode to use.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see #getHibernateFlushMode()
	 *
	 * @deprecated (since 5.2) use {@link #setHibernateFlushMode} instead
	 */
	@Override
	@Deprecated
	default Query<R> setFlushMode(FlushMode flushMode) {
		setHibernateFlushMode( flushMode );
		return this;
	}

	/**
	 * Bind a positional String-valued parameter.
	 *
	 * @param position The parameter position
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #setParameter(int, Object)} or {@link #setParameter(int, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setString(int position, String val) {
		setParameter( position, val, StringType.INSTANCE );
		return this;
	}

	/**
	 * Bind a positional char-valued parameter.
	 *
	 * @param position The parameter position
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #setParameter(int, Object)} or {@link #setParameter(int, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setCharacter(int position, char val) {
		setParameter( position, val, CharacterType.INSTANCE );
		return this;
	}

	/**
	 * Bind a positional boolean-valued parameter.
	 *
	 * @param position The parameter position
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #setParameter(int, Object)} or {@link #setParameter(int, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setBoolean(int position, boolean val) {
		setParameter( position, val, determineProperBooleanType( position, val, BooleanType.INSTANCE ) );
		return this;
	}

	/**
	 * Bind a positional byte-valued parameter.
	 *
	 * @param position The parameter position
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #setParameter(int, Object)} or {@link #setParameter(int, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setByte(int position, byte val) {
		setParameter( position, val, ByteType.INSTANCE );
		return this;
	}

	/**
	 * Bind a positional short-valued parameter.
	 *
	 * @param position The parameter position
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #setParameter(int, Object)} or {@link #setParameter(int, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setShort(int position, short val) {
		setParameter( position, val, ShortType.INSTANCE );
		return this;
	}

	/**
	 * Bind a positional int-valued parameter.
	 *
	 * @param position The parameter position
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #setParameter(int, Object)} or {@link #setParameter(int, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setInteger(int position, int val) {
		setParameter( position, val, IntegerType.INSTANCE );
		return this;
	}

	/**
	 * Bind a positional long-valued parameter.
	 *
	 * @param position The parameter position
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #setParameter(int, Object)} or {@link #setParameter(int, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setLong(int position, long val) {
		setParameter( position, val, LongType.INSTANCE );
		return this;
	}

	/**
	 * Bind a positional float-valued parameter.
	 *
	 * @param position The parameter position
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #setParameter(int, Object)} or {@link #setParameter(int, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setFloat(int position, float val) {
		setParameter( position, val, FloatType.INSTANCE );
		return this;
	}

	/**
	 * Bind a positional double-valued parameter.
	 *
	 * @param position The parameter position
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #setParameter(int, Object)} or {@link #setParameter(int, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setDouble(int position, double val) {
		setParameter( position, val, DoubleType.INSTANCE );
		return this;
	}

	/**
	 * Bind a positional binary-valued parameter.
	 *
	 * @param position The parameter position
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #setParameter(int, Object)} or {@link #setParameter(int, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setBinary(int position, byte[] val) {
		setParameter( position, val, BinaryType.INSTANCE );
		return this;
	}

	/**
	 * Bind a positional String-valued parameter using streaming.
	 *
	 * @param position The parameter position
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #setParameter(int, Object)} or {@link #setParameter(int, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setText(int position, String val) {
		setParameter( position, val, TextType.INSTANCE );
		return this;
	}

	/**
	 * Bind a positional binary-valued parameter using serialization.
	 *
	 * @param position The parameter position
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #setParameter(int, Object)} or {@link #setParameter(int, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setSerializable(int position, Serializable val) {
		setParameter( position, val );
		return this;
	}

	/**
	 * Bind a positional Locale-valued parameter.
	 *
	 * @param position The parameter position
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #setParameter(int, Object)} or {@link #setParameter(int, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setLocale(int position, Locale val) {
		setParameter( position, val, LocaleType.INSTANCE );
		return this;
	}

	/**
	 * Bind a positional BigDecimal-valued parameter.
	 *
	 * @param position The parameter position
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #setParameter(int, Object)} or {@link #setParameter(int, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setBigDecimal(int position, BigDecimal val) {
		setParameter( position, val, BigDecimalType.INSTANCE );
		return this;
	}

	/**
	 * Bind a positional BigDecimal-valued parameter.
	 *
	 * @param position The parameter position
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #setParameter(int, Object)} or {@link #setParameter(int, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setBigInteger(int position, BigInteger val) {
		setParameter( position, val, BigIntegerType.INSTANCE );
		return this;
	}

	/**
	 * Bind a positional Date-valued parameter using just the Date portion.
	 *
	 * @param position The parameter position
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #setParameter(int, Object)} or {@link #setParameter(int, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setDate(int position, Date val) {
		setParameter( position, val, DateType.INSTANCE );
		return this;
	}

	/**
	 * Bind a positional Date-valued parameter using just the Time portion.
	 *
	 * @param position The parameter position
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #setParameter(int, Object)} or {@link #setParameter(int, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setTime(int position, Date val) {
		setParameter( position, val, TimeType.INSTANCE );
		return this;
	}

	/**
	 * Bind a positional Date-valued parameter using the full Timestamp.
	 *
	 * @param position The parameter position
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #setParameter(int, Object)} or {@link #setParameter(int, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setTimestamp(int position, Date val) {
		setParameter( position, val, TimestampType.INSTANCE );
		return this;
	}

	/**
	 * Bind a positional Calendar-valued parameter using the full Timestamp portion.
	 *
	 * @param position The parameter position
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #setParameter(int, Object)} or {@link #setParameter(int, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setCalendar(int position, Calendar val) {
		setParameter( position, val, TimestampType.INSTANCE );
		return this;
	}

	/**
	 * Bind a positional Calendar-valued parameter using just the Date portion.
	 *
	 * @param position The parameter position
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #setParameter(int, Object)} or {@link #setParameter(int, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setCalendarDate(int position, Calendar val) {
		setParameter( position, val, DateType.INSTANCE );
		return this;
	}

	/**
	 * Bind a named String-valued parameter.
	 *
	 * @param name The parameter name
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #setParameter(String, Object)} or {@link #setParameter(String, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setString(String name, String val) {
		setParameter( name, val, StringType.INSTANCE );
		return this;
	}

	/**
	 * Bind a named char-valued parameter.
	 *
	 * @param name The parameter name
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #setParameter(String, Object)} or {@link #setParameter(String, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setCharacter(String name, char val) {
		setParameter( name, val, CharacterType.INSTANCE );
		return this;
	}

	/**
	 * Bind a named boolean-valued parameter.
	 *
	 * @param name The parameter name
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #setParameter(String, Object)} or {@link #setParameter(String, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setBoolean(String name, boolean val) {
		setParameter( name, val, determineProperBooleanType( name, val, BooleanType.INSTANCE ) );
		return this;
	}

	/**
	 * Bind a named byte-valued parameter.
	 *
	 * @param name The parameter name
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #setParameter(String, Object)} or {@link #setParameter(String, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setByte(String name, byte val) {
		setParameter( name, val, ByteType.INSTANCE );
		return this;
	}

	/**
	 * Bind a named short-valued parameter.
	 *
	 * @param name The parameter name
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #setParameter(String, Object)} or {@link #setParameter(String, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setShort(String name, short val) {
		setParameter( name, val, ShortType.INSTANCE );
		return this;
	}

	/**
	 * Bind a named int-valued parameter.
	 *
	 * @param name The parameter name
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #setParameter(String, Object)} or {@link #setParameter(String, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setInteger(String name, int val) {
		setParameter( name, val, IntegerType.INSTANCE );
		return this;
	}

	/**
	 * Bind a named long-valued parameter.
	 *
	 * @param name The parameter name
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #setParameter(String, Object)} or {@link #setParameter(String, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setLong(String name, long val) {
		setParameter( name, val, LongType.INSTANCE );
		return this;
	}

	/**
	 * Bind a named float-valued parameter.
	 *
	 * @param name The parameter name
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #setParameter(String, Object)} or {@link #setParameter(String, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setFloat(String name, float val) {
		setParameter( name, val, FloatType.INSTANCE );
		return this;
	}

	/**
	 * Bind a named double-valued parameter.
	 *
	 * @param name The parameter name
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #setParameter(String, Object)} or {@link #setParameter(String, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setDouble(String name, double val) {
		setParameter( name, val, DoubleType.INSTANCE );
		return this;
	}

	/**
	 * Bind a named binary-valued parameter.
	 *
	 * @param name The parameter name
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #setParameter(String, Object)} or {@link #setParameter(String, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setBinary(String name, byte[] val) {
		setParameter( name, val, BinaryType.INSTANCE );
		return this;
	}

	/**
	 * Bind a named String-valued parameter using streaming.
	 *
	 * @param name The parameter name
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #setParameter(String, Object)} or {@link #setParameter(String, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setText(String name, String val) {
		setParameter( name, val, TextType.INSTANCE );
		return this;
	}

	/**
	 * Bind a named binary-valued parameter using serialization.
	 *
	 * @param name The parameter name
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #setParameter(String, Object)} or {@link #setParameter(String, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setSerializable(String name, Serializable val) {
		setParameter( name, val );
		return this;
	}

	/**
	 * Bind a named Locale-valued parameter.
	 *
	 * @param name The parameter name
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 * @deprecated (since 5.2) use {@link #setParameter(String, Object)} or {@link #setParameter(String, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setLocale(String name, Locale val) {
		setParameter( name, val, TextType.INSTANCE );
		return this;
	}

	/**
	 * Bind a named BigDecimal-valued parameter.
	 *
	 * @param name The parameter name
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 * @deprecated (since 5.2) use {@link #setParameter(String, Object)} or {@link #setParameter(String, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setBigDecimal(String name, BigDecimal val) {
		setParameter( name, val, BigDecimalType.INSTANCE );
		return this;
	}

	/**
	 * Bind a named BigInteger-valued parameter.
	 *
	 * @param name The parameter name
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 * @deprecated (since 5.2) use {@link #setParameter(String, Object)} or {@link #setParameter(String, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setBigInteger(String name, BigInteger val) {
		setParameter( name, val, BigIntegerType.INSTANCE );
		return this;
	}

	/**
	 * Bind the val (time is truncated) of a given Date object to a named query parameter.
	 *
	 * @param name The name of the parameter
	 * @param val The val object
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #setParameter(String, Object)} or {@link #setParameter(String, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setDate(String name, Date val) {
		setParameter( name, val, DateType.INSTANCE );
		return this;
	}

	/**
	 * Bind the time (val is truncated) of a given Date object to a named query parameter.
	 *
	 * @param name The name of the parameter
	 * @param val The val object
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #setParameter(String, Object)} or {@link #setParameter(String, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setTime(String name, Date val) {
		setParameter( name, val, TimeType.INSTANCE );
		return this;
	}

	/**
	 * Bind the value and the time of a given Date object to a named query parameter.
	 *
	 * @param name The name of the parameter
	 * @param value The value object
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #setParameter(String, Object)} or {@link #setParameter(String, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setTimestamp(String name, Date value) {
		setParameter( name, value, TimestampType.INSTANCE );
		return this;
	}

	/**
	 * Bind a named Calendar-valued parameter using the full Timestamp.
	 *
	 * @param name The parameter name
	 * @param value The bind value
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #setParameter(String, Object)} or {@link #setParameter(String, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setCalendar(String name, Calendar value) {
		setParameter( name, value, TimestampType.INSTANCE );
		return this;
	}

	/**
	 * Bind a named Calendar-valued parameter using just the Date portion.
	 *
	 * @param name The parameter name
	 * @param value The bind value
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #setParameter(String, Object)} or {@link #setParameter(String, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setCalendarDate(String name, Calendar value) {
		setParameter( name, value, DateType.INSTANCE );
		return this;
	}

	/**
	 * Bind an instance of a mapped persistent class to a JDBC-style query parameter.
	 * Use {@link #setParameter(int, Object)} for null values.
	 *
	 * @param position the position of the parameter in the query
	 * string, numbered from <tt>0</tt>.
	 * @param val a non-null instance of a persistent class
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #setParameter(String, Object)} or {@link #setParameter(String, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	Query<R> setEntity(int position, Object val);

	/**
	 * Bind an instance of a mapped persistent class to a named query parameter.  Use
	 * {@link #setParameter(String, Object)} for null values.
	 *
	 * @param name the name of the parameter
	 * @param val a non-null instance of a persistent class
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #setParameter(String, Object)} or {@link #setParameter(String, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	Query<R> setEntity(String name, Object val);

	/**
	 * Set a strategy for handling the query results. This can be used to change
	 * "shape" of the query result.
	 *
	 * @param transformer The transformer to apply
	 *
	 * @return this (for method chaining)
	 *
	 * @deprecated (since 5.2)
	 * @todo develop a new approach to result transformers
	 */
	@Deprecated
	Query<R> setResultTransformer(ResultTransformer transformer);

	/**
	 * Bind values and types to positional parameters.  Allows binding more than one at a time; no real performance
	 * impact.
	 *
	 * The number of elements in each array should match.  That is, element number-0 in types array corresponds to
	 * element-0 in the values array, etc,
	 *
	 * @param types The types
	 * @param values The values
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) Bind values individually
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setParameters(Object[] values, Type[] types) {
		assert values.length == types.length;
		for ( int i = 0; i < values.length; i++ ) {
			setParameter( i, values[i], types[i] );
		}

		return this;
	}

	/**
	 * JPA 2.2 defines the {@code getResultStream} method so to get a {@link Stream} from the JDBC {@link java.sql.ResultSet}.
	 *
	 * Hibernate 5.2 already defines the {@link Query#stream()} method, so {@code getResultStream} can delegate to it.
	 *
	 * @return The results Stream
	 * @since 5.2.11
	 */
	default Stream<R> getResultStream() {
		return stream();
	}
}
