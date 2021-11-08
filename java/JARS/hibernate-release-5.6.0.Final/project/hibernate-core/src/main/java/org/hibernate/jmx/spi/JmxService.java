/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jmx.spi;

import javax.management.ObjectName;

import org.hibernate.service.Service;
import org.hibernate.service.spi.Manageable;

/**
 * Service providing simplified access to JMX related features needed by Hibernate.
 *
 * @author Steve Ebersole
 *
 * @deprecated Scheduled for removal in 6.0; see https://hibernate.atlassian.net/browse/HHH-14847
 * and https://hibernate.atlassian.net/browse/HHH-14846
 */
@Deprecated
public interface JmxService extends Service {
	/**
	 * Handles registration of a manageable service.
	 *
	 * @param service The manageable service
	 * @param serviceRole The service's role.
	 */
	void registerService(Manageable service, Class<? extends Service> serviceRole);

	/**
	 * Registers the given {@code mBean} under the given {@code objectName}
	 *
	 * @param objectName The name under which to register the MBean
	 * @param mBean The MBean to register
	 */
	void registerMBean(ObjectName objectName, Object mBean);
}
