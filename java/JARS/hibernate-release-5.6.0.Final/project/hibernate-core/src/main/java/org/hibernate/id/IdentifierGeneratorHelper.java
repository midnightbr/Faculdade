/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.type.CustomType;
import org.hibernate.type.Type;

/**
 * Factory and helper methods for {@link IdentifierGenerator} framework.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public final class IdentifierGeneratorHelper {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( IdentifierGeneratorHelper.class );

	/**
	 * Marker object returned from {@link IdentifierGenerator#generate} to indicate that we should short-circuit any
	 * continued generated id checking.  Currently this is only used in the case of the
	 * {@link org.hibernate.id.ForeignGenerator foreign} generator as a way to signal that we should use the associated
	 * entity's id value.
	 */
	public static final Serializable SHORT_CIRCUIT_INDICATOR = new Serializable() {
		@Override
		public String toString() {
			return "SHORT_CIRCUIT_INDICATOR";
		}
	};

	/**
	 * Marker object returned from {@link IdentifierGenerator#generate} to indicate that the entity's identifier will
	 * be generated as part of the database insertion.
	 */
	public static final Serializable POST_INSERT_INDICATOR = new Serializable() {
		@Override
		public String toString() {
			return "POST_INSERT_INDICATOR";
		}
	};


	/**
	 * Get the generated identifier when using identity columns
	 *
	 * @param rs The result set from which to extract the the generated identity.
	 * @param identifier The name of the identifier column
	 * @param type The expected type mapping for the identity value.
	 * @param dialect The current database dialect.
	 *
	 * @return The generated identity value
	 *
	 * @throws SQLException Can be thrown while accessing the result set
	 * @throws HibernateException Indicates a problem reading back a generated identity value.
	 */
	public static Serializable getGeneratedIdentity(ResultSet rs, String identifier, Type type, Dialect dialect)
			throws SQLException, HibernateException {
		if ( !rs.next() ) {
			throw new HibernateException( "The database returned no natively generated identity value" );
		}
		final Serializable id = get( rs, identifier, type, dialect );
		LOG.debugf( "Natively generated identity: %s", id );
		return id;
	}

	/**
	 * Extract the value from the result set (which is assumed to already have been positioned to the appropriate row)
	 * and wrp it in the appropriate Java numeric type.
	 *
	 * @param rs The result set from which to extract the value.
	 * @param identifier The name of the identifier column
	 * @param type The expected type of the value.
	 * @param dialect The current database dialect.
	 *
	 * @return The extracted value.
	 *
	 * @throws SQLException Indicates problems access the result set
	 * @throws IdentifierGenerationException Indicates an unknown type.
	 */
	public static Serializable get(ResultSet rs, String identifier, Type type, Dialect dialect)
			throws SQLException, IdentifierGenerationException {
		if ( ResultSetIdentifierConsumer.class.isInstance( type ) ) {
			return ( (ResultSetIdentifierConsumer) type ).consumeIdentifier( rs );
		}
		if ( CustomType.class.isInstance( type ) ) {
			final CustomType customType = (CustomType) type;
			if ( ResultSetIdentifierConsumer.class.isInstance( customType.getUserType() ) ) {
				return ( (ResultSetIdentifierConsumer) customType.getUserType() ).consumeIdentifier( rs );
			}
		}
		ResultSetMetaData resultSetMetaData = null;
		int columnCount = 1;
		try {
			resultSetMetaData = rs.getMetaData();
			columnCount = resultSetMetaData.getColumnCount();
		}
		catch (Exception e) {
			//Oracle driver will throw NPE
		}

		Class clazz = type.getReturnedClass();
		if ( columnCount == 1 ) {
			if ( clazz == Long.class ) {
				return rs.getLong( 1 );
			}
			else if ( clazz == Integer.class ) {
				return rs.getInt( 1 );
			}
			else if ( clazz == Short.class ) {
				return rs.getShort( 1 );
			}
			else if ( clazz == String.class ) {
				return rs.getString( 1 );
			}
			else if ( clazz == BigInteger.class ) {
				return rs.getBigDecimal( 1 ).setScale( 0, BigDecimal.ROUND_UNNECESSARY ).toBigInteger();
			}
			else if ( clazz == BigDecimal.class ) {
				return rs.getBigDecimal( 1 ).setScale( 0, BigDecimal.ROUND_UNNECESSARY );
			}
			else {
				throw new IdentifierGenerationException(
						"unrecognized id type : " + type.getName() + " -> " + clazz.getName()
				);
			}
		}
		else {
			try {
				return extractIdentifier( rs, identifier, type, clazz );
			}
			catch (SQLException e) {
				if ( StringHelper.isQuoted( identifier, dialect ) ) {
					return extractIdentifier( rs, StringHelper.unquote( identifier, dialect ), type, clazz );
				}
				throw e;
			}
		}
	}

	private static Serializable extractIdentifier(ResultSet rs, String identifier, Type type, Class clazz)
			throws SQLException {
		if ( clazz == Long.class ) {
			return rs.getLong( identifier );
		}
		else if ( clazz == Integer.class ) {
			return rs.getInt( identifier );
		}
		else if ( clazz == Short.class ) {
			return rs.getShort( identifier );
		}
		else if ( clazz == String.class ) {
			return rs.getString( identifier );
		}
		else if ( clazz == BigInteger.class ) {
			return rs.getBigDecimal( identifier ).setScale( 0, BigDecimal.ROUND_UNNECESSARY ).toBigInteger();
		}
		else if ( clazz == BigDecimal.class ) {
			return rs.getBigDecimal( identifier ).setScale( 0, BigDecimal.ROUND_UNNECESSARY );
		}
		else {
			throw new IdentifierGenerationException(
					"unrecognized id type : " + type.getName() + " -> " + clazz.getName()
			);
		}
	}

	/**
	 * Wrap the given value in the given Java numeric class.
	 *
	 * @param value The primitive value to wrap.
	 * @param clazz The Java numeric type in which to wrap the value.
	 *
	 * @return The wrapped type.
	 *
	 * @throws IdentifierGenerationException Indicates an unhandled 'clazz'.
	 * @deprecated Use the {@link #getIntegralDataTypeHolder holders} instead.
	 */
	@Deprecated
	public static Number createNumber(long value, Class clazz) throws IdentifierGenerationException {
		if ( clazz == Long.class ) {
			return value;
		}
		else if ( clazz == Integer.class ) {
			return (int) value;
		}
		else if ( clazz == Short.class ) {
			return (short) value;
		}
		else {
			throw new IdentifierGenerationException( "unrecognized id type : " + clazz.getName() );
		}
	}

	public static IntegralDataTypeHolder getIntegralDataTypeHolder(Class integralType) {
		if ( integralType == Long.class
				|| integralType == Integer.class
				|| integralType == Short.class ) {
			return new BasicHolder( integralType );
		}
		else if ( integralType == BigInteger.class ) {
			return new BigIntegerHolder();
		}
		else if ( integralType == BigDecimal.class ) {
			return new BigDecimalHolder();
		}
		else {
			throw new IdentifierGenerationException(
					"Unknown integral data type for ids : " + integralType.getName()
			);
		}
	}

	public static long extractLong(IntegralDataTypeHolder holder) {
		if ( holder.getClass() == BasicHolder.class ) {
			( (BasicHolder) holder ).checkInitialized();
			return ( (BasicHolder) holder ).value;
		}
		else if ( holder.getClass() == BigIntegerHolder.class ) {
			( (BigIntegerHolder) holder ).checkInitialized();
			return ( (BigIntegerHolder) holder ).value.longValue();
		}
		else if ( holder.getClass() == BigDecimalHolder.class ) {
			( (BigDecimalHolder) holder ).checkInitialized();
			return ( (BigDecimalHolder) holder ).value.longValue();
		}
		throw new IdentifierGenerationException( "Unknown IntegralDataTypeHolder impl [" + holder + "]" );
	}

	public static BigInteger extractBigInteger(IntegralDataTypeHolder holder) {
		if ( holder.getClass() == BasicHolder.class ) {
			( (BasicHolder) holder ).checkInitialized();
			return BigInteger.valueOf( ( (BasicHolder) holder ).value );
		}
		else if ( holder.getClass() == BigIntegerHolder.class ) {
			( (BigIntegerHolder) holder ).checkInitialized();
			return ( (BigIntegerHolder) holder ).value;
		}
		else if ( holder.getClass() == BigDecimalHolder.class ) {
			( (BigDecimalHolder) holder ).checkInitialized();
			// scale should already be set...
			return ( (BigDecimalHolder) holder ).value.toBigInteger();
		}
		throw new IdentifierGenerationException( "Unknown IntegralDataTypeHolder impl [" + holder + "]" );
	}

	public static BigDecimal extractBigDecimal(IntegralDataTypeHolder holder) {
		if ( holder.getClass() == BasicHolder.class ) {
			( (BasicHolder) holder ).checkInitialized();
			return BigDecimal.valueOf( ( (BasicHolder) holder ).value );
		}
		else if ( holder.getClass() == BigIntegerHolder.class ) {
			( (BigIntegerHolder) holder ).checkInitialized();
			return new BigDecimal( ( (BigIntegerHolder) holder ).value );
		}
		else if ( holder.getClass() == BigDecimalHolder.class ) {
			( (BigDecimalHolder) holder ).checkInitialized();
			// scale should already be set...
			return ( (BigDecimalHolder) holder ).value;
		}
		throw new IdentifierGenerationException( "Unknown IntegralDataTypeHolder impl [" + holder + "]" );
	}

	public static class BasicHolder implements IntegralDataTypeHolder {
		private final Class exactType;
		private long value = Long.MIN_VALUE;

		public BasicHolder(Class exactType) {
			this.exactType = exactType;
			if ( exactType != Long.class && exactType != Integer.class && exactType != Short.class ) {
				throw new IdentifierGenerationException( "Invalid type for basic integral holder : " + exactType );
			}
		}

		public long getActualLongValue() {
			return value;
		}

		public IntegralDataTypeHolder initialize(long value) {
			this.value = value;
			return this;
		}

		public IntegralDataTypeHolder initialize(ResultSet resultSet, long defaultValue) throws SQLException {
			long value = resultSet.getLong( 1 );
			if ( resultSet.wasNull() ) {
				value = defaultValue;
			}
			return initialize( value );
		}

		public void bind(PreparedStatement preparedStatement, int position) throws SQLException {
			// TODO : bind it as 'exact type'?  Not sure if that gains us anything...
			LOG.tracef( "binding parameter [%s] - [%s]", position, value );
			preparedStatement.setLong( position, value );
		}

		public IntegralDataTypeHolder increment() {
			checkInitialized();
			value++;
			return this;
		}

		private void checkInitialized() {
			if ( value == Long.MIN_VALUE ) {
				throw new IdentifierGenerationException( "integral holder was not initialized" );
			}
		}

		public IntegralDataTypeHolder add(long addend) {
			checkInitialized();
			value += addend;
			return this;
		}

		public IntegralDataTypeHolder decrement() {
			checkInitialized();
			value--;
			return this;
		}

		public IntegralDataTypeHolder subtract(long subtrahend) {
			checkInitialized();
			value -= subtrahend;
			return this;
		}

		public IntegralDataTypeHolder multiplyBy(IntegralDataTypeHolder factor) {
			return multiplyBy( extractLong( factor ) );
		}

		public IntegralDataTypeHolder multiplyBy(long factor) {
			checkInitialized();
			value *= factor;
			return this;
		}

		public boolean eq(IntegralDataTypeHolder other) {
			return eq( extractLong( other ) );
		}

		public boolean eq(long value) {
			checkInitialized();
			return this.value == value;
		}

		public boolean lt(IntegralDataTypeHolder other) {
			return lt( extractLong( other ) );
		}

		public boolean lt(long value) {
			checkInitialized();
			return this.value < value;
		}

		public boolean gt(IntegralDataTypeHolder other) {
			return gt( extractLong( other ) );
		}

		public boolean gt(long value) {
			checkInitialized();
			return this.value > value;
		}

		public IntegralDataTypeHolder copy() {
			BasicHolder copy = new BasicHolder( exactType );
			copy.value = value;
			return copy;
		}

		public Number makeValue() {
			// TODO : should we check for truncation?
			checkInitialized();
			if ( exactType == Long.class ) {
				return value;
			}
			else if ( exactType == Integer.class ) {
				return (int) value;
			}
			else {
				return (short) value;
			}
		}

		public Number makeValueThenIncrement() {
			final Number result = makeValue();
			value++;
			return result;
		}

		public Number makeValueThenAdd(long addend) {
			final Number result = makeValue();
			value += addend;
			return result;
		}

		@Override
		public String toString() {
			return "BasicHolder[" + exactType.getName() + "[" + value + "]]";
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			BasicHolder that = (BasicHolder) o;

			return value == that.value;
		}

		@Override
		public int hashCode() {
			return (int) ( value ^ ( value >>> 32 ) );
		}
	}

	public static class BigIntegerHolder implements IntegralDataTypeHolder {
		private BigInteger value;

		public IntegralDataTypeHolder initialize(long value) {
			this.value = BigInteger.valueOf( value );
			return this;
		}

		public IntegralDataTypeHolder initialize(ResultSet resultSet, long defaultValue) throws SQLException {
			final BigDecimal rsValue = resultSet.getBigDecimal( 1 );
			if ( resultSet.wasNull() ) {
				return initialize( defaultValue );
			}
			this.value = rsValue.setScale( 0, BigDecimal.ROUND_UNNECESSARY ).toBigInteger();
			return this;
		}

		public void bind(PreparedStatement preparedStatement, int position) throws SQLException {
			preparedStatement.setBigDecimal( position, new BigDecimal( value ) );
		}

		public IntegralDataTypeHolder increment() {
			checkInitialized();
			value = value.add( BigInteger.ONE );
			return this;
		}

		private void checkInitialized() {
			if ( value == null ) {
				throw new IdentifierGenerationException( "integral holder was not initialized" );
			}
		}

		public IntegralDataTypeHolder add(long increment) {
			checkInitialized();
			value = value.add( BigInteger.valueOf( increment ) );
			return this;
		}

		public IntegralDataTypeHolder decrement() {
			checkInitialized();
			value = value.subtract( BigInteger.ONE );
			return this;
		}

		public IntegralDataTypeHolder subtract(long subtrahend) {
			checkInitialized();
			value = value.subtract( BigInteger.valueOf( subtrahend ) );
			return this;
		}

		public IntegralDataTypeHolder multiplyBy(IntegralDataTypeHolder factor) {
			checkInitialized();
			value = value.multiply( extractBigInteger( factor ) );
			return this;
		}

		public IntegralDataTypeHolder multiplyBy(long factor) {
			checkInitialized();
			value = value.multiply( BigInteger.valueOf( factor ) );
			return this;
		}

		public boolean eq(IntegralDataTypeHolder other) {
			checkInitialized();
			return value.compareTo( extractBigInteger( other ) ) == 0;
		}

		public boolean eq(long value) {
			checkInitialized();
			return this.value.compareTo( BigInteger.valueOf( value ) ) == 0;
		}

		public boolean lt(IntegralDataTypeHolder other) {
			checkInitialized();
			return value.compareTo( extractBigInteger( other ) ) < 0;
		}

		public boolean lt(long value) {
			checkInitialized();
			return this.value.compareTo( BigInteger.valueOf( value ) ) < 0;
		}

		public boolean gt(IntegralDataTypeHolder other) {
			checkInitialized();
			return value.compareTo( extractBigInteger( other ) ) > 0;
		}

		public boolean gt(long value) {
			checkInitialized();
			return this.value.compareTo( BigInteger.valueOf( value ) ) > 0;
		}

		public IntegralDataTypeHolder copy() {
			BigIntegerHolder copy = new BigIntegerHolder();
			copy.value = value;
			return copy;
		}

		public Number makeValue() {
			checkInitialized();
			return value;
		}

		public Number makeValueThenIncrement() {
			final Number result = makeValue();
			value = value.add( BigInteger.ONE );
			return result;
		}

		public Number makeValueThenAdd(long addend) {
			final Number result = makeValue();
			value = value.add( BigInteger.valueOf( addend ) );
			return result;
		}

		@Override
		public String toString() {
			return "BigIntegerHolder[" + value + "]";
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			BigIntegerHolder that = (BigIntegerHolder) o;

			return this.value == null
					? that.value == null
					: value.equals( that.value );
		}

		@Override
		public int hashCode() {
			return value != null ? value.hashCode() : 0;
		}
	}

	public static class BigDecimalHolder implements IntegralDataTypeHolder {
		private BigDecimal value;

		public IntegralDataTypeHolder initialize(long value) {
			this.value = BigDecimal.valueOf( value );
			return this;
		}

		public IntegralDataTypeHolder initialize(ResultSet resultSet, long defaultValue) throws SQLException {
			final BigDecimal rsValue = resultSet.getBigDecimal( 1 );
			if ( resultSet.wasNull() ) {
				return initialize( defaultValue );
			}
			this.value = rsValue.setScale( 0, BigDecimal.ROUND_UNNECESSARY );
			return this;
		}

		public void bind(PreparedStatement preparedStatement, int position) throws SQLException {
			preparedStatement.setBigDecimal( position, value );
		}

		public IntegralDataTypeHolder increment() {
			checkInitialized();
			value = value.add( BigDecimal.ONE );
			return this;
		}

		private void checkInitialized() {
			if ( value == null ) {
				throw new IdentifierGenerationException( "integral holder was not initialized" );
			}
		}

		public IntegralDataTypeHolder add(long increment) {
			checkInitialized();
			value = value.add( BigDecimal.valueOf( increment ) );
			return this;
		}

		public IntegralDataTypeHolder decrement() {
			checkInitialized();
			value = value.subtract( BigDecimal.ONE );
			return this;
		}

		public IntegralDataTypeHolder subtract(long subtrahend) {
			checkInitialized();
			value = value.subtract( BigDecimal.valueOf( subtrahend ) );
			return this;
		}

		public IntegralDataTypeHolder multiplyBy(IntegralDataTypeHolder factor) {
			checkInitialized();
			value = value.multiply( extractBigDecimal( factor ) );
			return this;
		}

		public IntegralDataTypeHolder multiplyBy(long factor) {
			checkInitialized();
			value = value.multiply( BigDecimal.valueOf( factor ) );
			return this;
		}

		public boolean eq(IntegralDataTypeHolder other) {
			checkInitialized();
			return value.compareTo( extractBigDecimal( other ) ) == 0;
		}

		public boolean eq(long value) {
			checkInitialized();
			return this.value.compareTo( BigDecimal.valueOf( value ) ) == 0;
		}

		public boolean lt(IntegralDataTypeHolder other) {
			checkInitialized();
			return value.compareTo( extractBigDecimal( other ) ) < 0;
		}

		public boolean lt(long value) {
			checkInitialized();
			return this.value.compareTo( BigDecimal.valueOf( value ) ) < 0;
		}

		public boolean gt(IntegralDataTypeHolder other) {
			checkInitialized();
			return value.compareTo( extractBigDecimal( other ) ) > 0;
		}

		public boolean gt(long value) {
			checkInitialized();
			return this.value.compareTo( BigDecimal.valueOf( value ) ) > 0;
		}

		public IntegralDataTypeHolder copy() {
			BigDecimalHolder copy = new BigDecimalHolder();
			copy.value = value;
			return copy;
		}

		public Number makeValue() {
			checkInitialized();
			return value;
		}

		public Number makeValueThenIncrement() {
			final Number result = makeValue();
			value = value.add( BigDecimal.ONE );
			return result;
		}

		public Number makeValueThenAdd(long addend) {
			final Number result = makeValue();
			value = value.add( BigDecimal.valueOf( addend ) );
			return result;
		}

		@Override
		public String toString() {
			return "BigDecimalHolder[" + value + "]";
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			BigDecimalHolder that = (BigDecimalHolder) o;

			return this.value == null
					? that.value == null
					: this.value.equals( that.value );
		}

		@Override
		public int hashCode() {
			return value != null ? value.hashCode() : 0;
		}
	}

	/**
	 * Disallow instantiation of IdentifierGeneratorHelper.
	 */
	private IdentifierGeneratorHelper() {
	}
}
