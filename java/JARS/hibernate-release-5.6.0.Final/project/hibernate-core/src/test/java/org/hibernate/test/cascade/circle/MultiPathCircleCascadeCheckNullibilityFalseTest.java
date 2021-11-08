/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cascade.circle;

import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

/**
 * @author Gail Badner
 */
public class MultiPathCircleCascadeCheckNullibilityFalseTest extends MultiPathCircleCascadeTest {
	@Override
	 public void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.CHECK_NULLABILITY, "false" );
	}
}
