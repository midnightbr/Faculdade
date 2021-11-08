/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.graph.GraphSemantic;

import static org.hibernate.annotations.QueryHints.CACHEABLE;
import static org.hibernate.annotations.QueryHints.CACHE_MODE;
import static org.hibernate.annotations.QueryHints.CACHE_REGION;
import static org.hibernate.annotations.QueryHints.COMMENT;
import static org.hibernate.annotations.QueryHints.FETCH_SIZE;
import static org.hibernate.annotations.QueryHints.FLUSH_MODE;
import static org.hibernate.annotations.QueryHints.FOLLOW_ON_LOCKING;
import static org.hibernate.annotations.QueryHints.NATIVE_LOCKMODE;
import static org.hibernate.annotations.QueryHints.NATIVE_SPACES;
import static org.hibernate.annotations.QueryHints.PASS_DISTINCT_THROUGH;
import static org.hibernate.annotations.QueryHints.READ_ONLY;
import static org.hibernate.annotations.QueryHints.TIMEOUT_HIBERNATE;
import static org.hibernate.annotations.QueryHints.TIMEOUT_JAKARTA_JPA;
import static org.hibernate.annotations.QueryHints.TIMEOUT_JPA;

/**
 * Defines the supported JPA query hints
 */
public class QueryHints {
	/**
	 * The hint key for specifying a query timeout per Hibernate O/RM, which defines the timeout in seconds.
	 *
	 * @deprecated use {@link #SPEC_HINT_TIMEOUT} instead
	 */
	@Deprecated
	public static final String HINT_TIMEOUT = TIMEOUT_HIBERNATE;

	/**
	 * The hint key for specifying a query timeout per JPA, which defines the timeout in milliseconds
	 */
	public static final String SPEC_HINT_TIMEOUT = TIMEOUT_JPA;

	/**
	 * The hint key for specifying a query timeout per JPA, which defines the timeout in milliseconds
	 */
	public static final String JAKARTA_SPEC_HINT_TIMEOUT = TIMEOUT_JAKARTA_JPA;

	/**
	 * The hint key for specifying a comment which is to be embedded into the SQL sent to the database.
	 */
	public static final String HINT_COMMENT = COMMENT;

	/**
	 * The hint key for specifying a JDBC fetch size, used when executing the resulting SQL.
	 */
	public static final String HINT_FETCH_SIZE = FETCH_SIZE;

	/**
	 * The hint key for specifying whether the query results should be cached for the next (cached) execution of the
	 * "same query".
	 */
	public static final String HINT_CACHEABLE = CACHEABLE;

	/**
	 * The hint key for specifying the name of the cache region (within Hibernate's query result cache region)
	 * to use for storing the query results.
	 */
	public static final String HINT_CACHE_REGION = CACHE_REGION;

	/**
	 * The hint key for specifying that objects loaded into the persistence context as a result of this query execution
	 * should be associated with the persistence context as read-only.
	 */
	public static final String HINT_READONLY = READ_ONLY;

	/**
	 * The hint key for specifying the cache mode ({@link org.hibernate.CacheMode}) to be in effect for the
	 * execution of the hinted query.
	 */
	public static final String HINT_CACHE_MODE = CACHE_MODE;

	/**
	 * The hint key for specifying the flush mode ({@link org.hibernate.FlushMode}) to be in effect for the
	 * execution of the hinted query.
	 */
	public static final String HINT_FLUSH_MODE = FLUSH_MODE;

	public static final String HINT_NATIVE_LOCKMODE = NATIVE_LOCKMODE;
	
	/**
	 * Hint providing a "fetchgraph" EntityGraph.  Attributes explicitly specified as AttributeNodes are treated as
	 * FetchType.EAGER (via join fetch or subsequent select).
	 * 
	 * Note: Currently, attributes that are not specified are treated as FetchType.LAZY or FetchType.EAGER depending
	 * on the attribute's definition in metadata, rather than forcing FetchType.LAZY.
	 */
	public static final String HINT_FETCHGRAPH = GraphSemantic.FETCH.getJpaHintName();
	
	/**
	 * Hint providing a "loadgraph" EntityGraph.  Attributes explicitly specified as AttributeNodes are treated as
	 * FetchType.EAGER (via join fetch or subsequent select).  Attributes that are not specified are treated as
	 * FetchType.LAZY or FetchType.EAGER depending on the attribute's definition in metadata
	 */
	public static final String HINT_LOADGRAPH = GraphSemantic.LOAD.getJpaHintName();

	/**
	 * Hint providing a "fetchgraph" EntityGraph.  Attributes explicitly specified as AttributeNodes are treated as
	 * FetchType.EAGER (via join fetch or subsequent select).
	 *
	 * Note: Currently, attributes that are not specified are treated as FetchType.LAZY or FetchType.EAGER depending
	 * on the attribute's definition in metadata, rather than forcing FetchType.LAZY.
	 */
	public static final String JAKARTA_HINT_FETCHGRAPH = GraphSemantic.FETCH.getJakartaJpaHintName();

	/**
	 * Hint providing a "loadgraph" EntityGraph.  Attributes explicitly specified as AttributeNodes are treated as
	 * FetchType.EAGER (via join fetch or subsequent select).  Attributes that are not specified are treated as
	 * FetchType.LAZY or FetchType.EAGER depending on the attribute's definition in metadata
	 */
	public static final String JAKARTA_HINT_LOADGRAPH = GraphSemantic.LOAD.getJakartaJpaHintName();

	public static final String HINT_FOLLOW_ON_LOCKING = FOLLOW_ON_LOCKING;

	public static final String HINT_PASS_DISTINCT_THROUGH = PASS_DISTINCT_THROUGH;

	public static final String HINT_NATIVE_SPACES = NATIVE_SPACES;


	private static final Set<String> HINTS = buildHintsSet();

	private static Set<String> buildHintsSet() {
		HashSet<String> hints = new HashSet<>();
		hints.add( HINT_TIMEOUT );
		hints.add( SPEC_HINT_TIMEOUT );
		hints.add( JAKARTA_SPEC_HINT_TIMEOUT );
		hints.add( HINT_COMMENT );
		hints.add( HINT_FETCH_SIZE );
		hints.add( HINT_CACHE_REGION );
		hints.add( HINT_CACHEABLE );
		hints.add( HINT_READONLY );
		hints.add( HINT_CACHE_MODE );
		hints.add( HINT_FLUSH_MODE );
		hints.add( HINT_NATIVE_LOCKMODE );
		hints.add( HINT_FETCHGRAPH );
		hints.add( HINT_LOADGRAPH );
		hints.add( JAKARTA_HINT_FETCHGRAPH );
		hints.add( JAKARTA_HINT_LOADGRAPH );
		hints.add( HINT_NATIVE_SPACES );
		return java.util.Collections.unmodifiableSet( hints );
	}

	public static Set<String> getDefinedHints() {
		return HINTS;
	}

	protected QueryHints() {
	}
}
