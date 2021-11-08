/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.enhanced;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.MappedSuperclass;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.ModifiedEntityNames;

/**
 * Extension of standard {@link SequenceIdRevisionEntity} that allows tracking entity names changed in each revision.
 * This revision entity is implicitly used when {@code org.hibernate.envers.track_entities_changed_in_revision}
 * parameter is set to {@code true}.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@MappedSuperclass
public class SequenceIdTrackingModifiedEntitiesRevisionEntity extends SequenceIdRevisionEntity {
	@ElementCollection(fetch = FetchType.EAGER)
	@JoinTable(name = "REVCHANGES", joinColumns = @JoinColumn(name = "REV"))
	@Column(name = "ENTITYNAME")
	@Fetch(FetchMode.JOIN)
	@ModifiedEntityNames
	private Set<String> modifiedEntityNames = new HashSet<>();

	@SuppressWarnings("UnusedDeclaration")
	public Set<String> getModifiedEntityNames() {
		return modifiedEntityNames;
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setModifiedEntityNames(Set<String> modifiedEntityNames) {
		this.modifiedEntityNames = modifiedEntityNames;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof SequenceIdTrackingModifiedEntitiesRevisionEntity) ) {
			return false;
		}
		if ( !super.equals( o ) ) {
			return false;
		}

		final SequenceIdTrackingModifiedEntitiesRevisionEntity that = (SequenceIdTrackingModifiedEntitiesRevisionEntity) o;

		if ( modifiedEntityNames == null ) {
			return that.modifiedEntityNames == null;
		}
		else {
			return modifiedEntityNames.equals( that.modifiedEntityNames );
		}
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + (modifiedEntityNames != null ? modifiedEntityNames.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "SequenceIdTrackingModifiedEntitiesRevisionEntity(" + super.toString()
				+ ", modifiedEntityNames = " + modifiedEntityNames + ")";
	}
}
