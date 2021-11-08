/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.criteria.idclass;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;

/**
 * @author Erich Heard
 */
@Entity
@Table( name = "WIDGET" )
@IdClass( WidgetId.class )
public class Widget {
	@Id
	private String code;
	public String getCode( ) { return this.code; }
	public void setCode( String value ) { this.code = value; }
	
	@Id
	private String division;
	public String getDivision( ) { return this.division; }
	public void setDivision( String value ) { this.division = value; }
	
	@Column( name = "COST" )
	private Double cost;
	public Double getCost( ) { return this.cost; }
	public void setCost( Double value ) { this.cost = value; }
	
	@Override
	public String toString( ) {
		return "[Code:" + this.getCode( ) + "; Division: " + this.getDivision( ) + "; Cost: " + this.getCost( ) + "]";
	}
}
