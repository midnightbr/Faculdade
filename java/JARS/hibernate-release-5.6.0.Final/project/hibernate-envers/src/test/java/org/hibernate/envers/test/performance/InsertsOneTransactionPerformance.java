/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.envers.test.performance;

import java.io.IOException;
import javax.persistence.EntityManager;

import org.hibernate.envers.test.entities.StrTestEntity;

import org.junit.Ignore;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Ignore
public class InsertsOneTransactionPerformance extends AbstractPerformanceTest {
	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {StrTestEntity.class};
	}

	private final static int NUMBER_INSERTS = 5000;

	protected void doTest() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		start();
		for ( int i = 0; i < NUMBER_INSERTS; i++ ) {
			entityManager.persist( new StrTestEntity( "x" + i ) );
		}
		entityManager.getTransaction().commit();
		stop();
	}

	public static void main(String[] args) throws IOException {
		InsertsOneTransactionPerformance insertsOneTransactionPerformance = new InsertsOneTransactionPerformance();
		insertsOneTransactionPerformance.test( 3 );
	}
}