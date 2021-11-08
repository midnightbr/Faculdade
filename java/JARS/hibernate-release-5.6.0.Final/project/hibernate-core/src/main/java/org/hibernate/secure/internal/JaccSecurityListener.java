/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.secure.internal;

/**
 * Marker interface for JACC event listeners.  Used in event listener duplication strategy checks; see
 * {@link org.hibernate.secure.spi.JaccIntegrator} for details.
 * 
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 *
 * @deprecated Support for JACC will be removed in 6.0
 */
@Deprecated
public interface JaccSecurityListener {
}
