/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.lazy.proxy;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.MapsId;
import javax.persistence.OneToOne;

import org.hibernate.Hibernate;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.stat.Statistics;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author Gail Badner
 */

@TestForIssue(jiraKey = "HHH-13814")
@RunWith( BytecodeEnhancerRunner.class )
@EnhancementOptions( lazyLoading = true )
public class MapsIdProxyBidirectionalTest extends BaseNonConfigCoreFunctionalTestCase {

	@Test
	@TestForIssue(jiraKey = "HHH-13814")
	public void testAssociation() {
		inTransaction(
				session -> {
					final Statistics statistics = sessionFactory().getStatistics();
					statistics.clear();
					EmployerInfo employerInfo = session.get( EmployerInfo.class, 1 );

					assertEquals( 1, statistics.getPrepareStatementCount() );

					assertTrue( Hibernate.isPropertyInitialized( employerInfo, "employer" ) );
					final Employer employer = employerInfo.employer;
					assertFalse( Hibernate.isInitialized( employer ) );
					Hibernate.initialize( employer );
					assertEquals( "Employer #" + employer.id, employer.name );

					assertThat( statistics.getEntityLoadCount(), is( 2L ) );
					assertThat( statistics.getPrepareStatementCount(), is( 2L ) );
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-13814")
	@FailureExpected( jiraKey = "HHH-13814")
	public void testMappedByAssociation() {
		inTransaction(
				session -> {
					final Statistics statistics = sessionFactory().getStatistics();
					statistics.clear();
					Employer employer = session.get( Employer.class, 1 );

					assertEquals( 1, statistics.getPrepareStatementCount() );

					assertTrue( Hibernate.isPropertyInitialized( employer, "employerInfo" ) );
					EmployerInfo employerInfo = employer.employerInfo;
					assertFalse( Hibernate.isInitialized( employerInfo ) );
					Hibernate.initialize( employerInfo );
					assertTrue( Hibernate.isPropertyInitialized( employerInfo, "employer" ) );
					assertSame( employer, employerInfo.employer );

					assertThat( statistics.getEntityLoadCount(), is( 2L ) );
					assertThat( statistics.getPrepareStatementCount(), is( 2L ) );
				}
		);
	}


	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				EmployerInfo.class,
				Employer.class
		};
	}

	@Before
	public void setUpData() {
		inTransaction(
				session -> {
					final Employer employer = new Employer();
					employer.id = 1;
					employer.name = "Employer #" + employer.id;
					final EmployerInfo employerInfo = new EmployerInfo();
					employer.employerInfo = employerInfo;
					employerInfo.employer = employer;
					session.persist( employer );
				}
		);
	}

	@After
	public void cleanupDate() {
		inTransaction(
				session -> {
					session.createQuery( "delete from EmployerInfo" ).executeUpdate();
					session.createQuery( "delete from Employer" ).executeUpdate();
				}
		);
	}

	@Override
	protected void configureStandardServiceRegistryBuilder(StandardServiceRegistryBuilder ssrb) {
		super.configureStandardServiceRegistryBuilder( ssrb );
		ssrb.applySetting( AvailableSettings.FORMAT_SQL, "false" );
		ssrb.applySetting( AvailableSettings.GENERATE_STATISTICS, "true" );
		ssrb.applySetting( AvailableSettings.SHOW_SQL, true );
	}

	@Override
	protected void configureSessionFactoryBuilder(SessionFactoryBuilder sfb) {
		super.configureSessionFactoryBuilder( sfb );
		sfb.applyStatisticsSupport( true );
		sfb.applySecondLevelCacheSupport( false );
		sfb.applyQueryCacheSupport( false );
	}

	@Entity(name = "EmployerInfo")
	public static class EmployerInfo {
		@Id
		private int id;

		@MapsId
		@OneToOne(optional = false, fetch = FetchType.LAZY)
		private Employer employer;

		private String info;
	}

	@Entity(name = "Employer")
	public static class Employer {
		@Id
		private int id;

		@OneToOne(optional = false, fetch = FetchType.LAZY, mappedBy = "employer", cascade = CascadeType.ALL)
		private EmployerInfo employerInfo;

		private String name;
	}
}