/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.converter.custom;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

/**
 * @author Steve Ebersole
 */
@Converter( autoApply = true )
public class MyCustomConverter implements AttributeConverter<MyCustomJavaType, String> {
	@Override
	public String convertToDatabaseColumn(MyCustomJavaType attribute) {
		return null;
	}

	@Override
	public MyCustomJavaType convertToEntityAttribute(String dbData) {
		return null;
	}
}
