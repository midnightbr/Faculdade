/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.internal.ForeignKeys;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.persister.entity.EntityPersister;

/**
 * A one-to-one association to an entity
 * @author Gavin King
 */
public class OneToOneType extends EntityType {

	private final ForeignKeyDirection foreignKeyType;
	private final String propertyName;
	private final String entityName;
	private final boolean constrained;

	/**
	 * @deprecated Use {@link #OneToOneType(TypeFactory.TypeScope, String, ForeignKeyDirection, boolean, String, boolean, boolean, String, String, boolean)}
	 *  instead.
	 */
	@Deprecated
	public OneToOneType(
			TypeFactory.TypeScope scope,
			String referencedEntityName,
			ForeignKeyDirection foreignKeyType,
			String uniqueKeyPropertyName,
			boolean lazy,
			boolean unwrapProxy,
			String entityName,
			String propertyName) {
		this( scope, referencedEntityName, foreignKeyType, uniqueKeyPropertyName == null, uniqueKeyPropertyName, lazy, unwrapProxy, entityName, propertyName );
	}

	/**
	 * @deprecated Use {@link #OneToOneType(TypeFactory.TypeScope, String, ForeignKeyDirection, boolean, String, boolean, boolean, String, String, boolean)}
	 *  instead.
	 */
	@Deprecated
	public OneToOneType(
			TypeFactory.TypeScope scope,
			String referencedEntityName,
			ForeignKeyDirection foreignKeyType,
			boolean referenceToPrimaryKey,
			String uniqueKeyPropertyName,
			boolean lazy,
			boolean unwrapProxy,
			String entityName,
			String propertyName) {
		this( scope, referencedEntityName, foreignKeyType, referenceToPrimaryKey, uniqueKeyPropertyName, lazy, unwrapProxy, entityName, propertyName, foreignKeyType != ForeignKeyDirection.TO_PARENT );
	}

	public OneToOneType(
			TypeFactory.TypeScope scope,
			String referencedEntityName,
			ForeignKeyDirection foreignKeyType,
			boolean referenceToPrimaryKey,
			String uniqueKeyPropertyName,
			boolean lazy,
			boolean unwrapProxy,
			String entityName,
			String propertyName,
			boolean constrained) {
		super( scope, referencedEntityName, referenceToPrimaryKey, uniqueKeyPropertyName, !lazy, unwrapProxy );
		this.foreignKeyType = foreignKeyType;
		this.propertyName = propertyName;
		this.entityName = entityName;
		this.constrained = constrained;
	}

	public OneToOneType(OneToOneType original, String superTypeEntityName) {
		super( original, superTypeEntityName );
		this.foreignKeyType = original.foreignKeyType;
		this.propertyName = original.propertyName;
		this.entityName = original.entityName;
		this.constrained = original.constrained;
	}

	@Override
	public String getPropertyName() {
		return propertyName;
	}

	@Override
	public boolean isNull(Object owner, SharedSessionContractImplementor session) {
		if ( propertyName != null ) {
			final EntityPersister ownerPersister = session.getFactory().getMetamodel().entityPersister( entityName );
			final Serializable id = session.getContextEntityIdentifier( owner );
			final EntityKey entityKey = session.generateEntityKey( id, ownerPersister );
			return session.getPersistenceContextInternal().isPropertyNull( entityKey, getPropertyName() );
		}
		else {
			return false;
		}
	}

	@Override
	public int getColumnSpan(Mapping session) throws MappingException {
		return 0;
	}

	@Override
	public int[] sqlTypes(Mapping session) throws MappingException {
		return ArrayHelper.EMPTY_INT_ARRAY;
	}

	private static final Size[] SIZES = new Size[0];

	@Override
	public Size[] dictatedSizes(Mapping mapping) throws MappingException {
		return SIZES;
	}

	@Override
	public Size[] defaultSizes(Mapping mapping) throws MappingException {
		return SIZES;
	}

	@Override
	public boolean[] toColumnNullness(Object value, Mapping mapping) {
		return ArrayHelper.EMPTY_BOOLEAN_ARRAY;
	}

	@Override
	public void nullSafeSet(PreparedStatement st, Object value, int index, boolean[] settable, SharedSessionContractImplementor session) {
		//nothing to do
	}

	@Override
	public boolean isOneToOne() {
		return true;
	}

	@Override
	public boolean isDirty(Object old, Object current, SharedSessionContractImplementor session) {
		if ( isSame( old, current ) ) {
			return false;
		}

		Object oldid = getIdentifier( old, session );
		Object newid = getIdentifier( current, session );

		return getIdentifierType( session ).isDirty( oldid, newid, session );
	}

	@Override
	public boolean isDirty(Object old, Object current, boolean[] checkable, SharedSessionContractImplementor session) {
		return isDirty(old, current, session);
	}

	@Override
	public boolean isModified(Object old, Object current, boolean[] checkable, SharedSessionContractImplementor session) {
		return false;
	}

	@Override
	public ForeignKeyDirection getForeignKeyDirection() {
		return foreignKeyType;
	}

	@Override
	public Object hydrate(
		ResultSet rs,
		String[] names,
		SharedSessionContractImplementor session,
		Object owner) throws HibernateException, SQLException {
		return session.getContextEntityIdentifier(owner);
	}

	@Override
	public boolean isNullable() {
		return !constrained;
	}

	@Override
	public boolean useLHSPrimaryKey() {
		return true;
	}

	@Override
	public Serializable disassemble(Object value, SharedSessionContractImplementor session, Object owner) throws HibernateException {
		if (value == null) {
			return null;
		}

		Object id = ForeignKeys.getEntityIdentifierIfNotUnsaved( getAssociatedEntityName(), value, session );

		if ( id == null ) {
			throw new AssertionFailure(
				"cannot cache a reference to an object with a null id: " +
				getAssociatedEntityName()
			);
		}

		return getIdentifierType( session ).disassemble( id, session, owner );
	}

	@Override
	public Object assemble(Serializable oid, SharedSessionContractImplementor session, Object owner) throws HibernateException {

		if ( oid == null ) {
			if ( uniqueKeyPropertyName != null ) {
				return resolve( session.getContextEntityIdentifier( owner ), session, owner );
			}
			return null;
		}

		//the owner of the association is not the owner of the id
		Serializable id = ( Serializable ) getIdentifierType( session ).assemble( oid, session, null );

		if ( id == null ) {
			return null;
		}

		return resolveIdentifier( id, session );
	}
	
	@Override
	public boolean isAlwaysDirtyChecked() {
		return true;
	}
}
