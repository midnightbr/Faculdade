/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.ejb3configuration.id;

import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

/**
 * @author <a href="mailto:emmanuel@hibernate.org">Emmanuel Bernard</a>
 */
public class FunkyIdGenerator implements IdentifierGenerator {
	public Serializable generate(SharedSessionContractImplementor session, Object object) throws HibernateException {
		throw new FunkyException();
	}
}
