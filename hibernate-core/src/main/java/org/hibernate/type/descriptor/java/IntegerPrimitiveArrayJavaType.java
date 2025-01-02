/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.SharedSessionContract;
import org.hibernate.engine.jdbc.BinaryStream;
import org.hibernate.engine.jdbc.internal.ArrayBackedBinaryStream;
import org.hibernate.internal.build.AllowReflection;
import org.hibernate.internal.util.SerializationHelper;
import org.hibernate.type.descriptor.WrapperOptions;

/**
 * Descriptor for {@code int[]} handling.
 *
 * @author Christian Beikov
 */
@AllowReflection // Needed for arbitrary array wrapping/unwrapping
public class IntegerPrimitiveArrayJavaType extends AbstractArrayJavaType<int[], Integer> {

	public static final IntegerPrimitiveArrayJavaType INSTANCE = new IntegerPrimitiveArrayJavaType();

	private IntegerPrimitiveArrayJavaType() {
		this( IntegerJavaType.INSTANCE );
	}

	protected IntegerPrimitiveArrayJavaType(JavaType<Integer> baseDescriptor) {
		super( int[].class, baseDescriptor, new ArrayMutabilityPlan() );
	}

	@Override
	public String extractLoggableRepresentation(int[] value) {
		return value == null ? super.extractLoggableRepresentation( null ) : Arrays.toString( value );
	}

	@Override
	public boolean areEqual(int[] one, int[] another) {
		return Arrays.equals( one, another );
	}

	@Override
	public int extractHashCode(int[] value) {
		return Arrays.hashCode( value );
	}

	@Override
	public String toString(int[] value) {
		if ( value == null ) {
			return null;
		}
		final StringBuilder sb = new StringBuilder();
		sb.append( '{' );
		sb.append( value[0] );
		for ( int i = 1; i < value.length; i++ ) {
			sb.append( value[i] );
			sb.append( ',' );
		}
		sb.append( '}' );
		return sb.toString();
	}

	@Override
	public int[] fromString(CharSequence charSequence) {
		if ( charSequence == null ) {
			return null;
		}
		final List<Integer> list = new ArrayList<>();
		final char lastChar = charSequence.charAt( charSequence.length() - 1 );
		final char firstChar = charSequence.charAt( 0 );
		if ( firstChar != '{' || lastChar != '}' ) {
			throw new IllegalArgumentException( "Cannot parse given string into array of strings. First and last character must be { and }" );
		}
		final int len = charSequence.length();
		int elementStart = 1;
		for ( int i = elementStart; i < len; i ++ ) {
			final char c = charSequence.charAt( i );
			if ( c == ',' ) {
				list.add( Integer.parseInt( charSequence, elementStart, i, 10 ) );
				elementStart = i + 1;
			}
		}
		final int[] result = new int[list.size()];
		for ( int i = 0; i < result.length; i ++ ) {
			result[ i ] = list.get( i );
		}
		return result;
	}

	@Override
	public <X> X unwrap(int[] value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}

		if ( type.isInstance( value ) ) {
			return (X) value;
		}
		else if ( Object[].class.isAssignableFrom( type ) ) {
			final Class<?> preferredJavaTypeClass = type.getComponentType();
			final Object[] unwrapped = (Object[]) Array.newInstance( preferredJavaTypeClass, value.length );
			for ( int i = 0; i < value.length; i++ ) {
				unwrapped[i] = getElementJavaType().unwrap( value[i], preferredJavaTypeClass, options );
			}
			return (X) unwrapped;
		}
		else if ( type == byte[].class ) {
			// byte[] can only be requested if the value should be serialized
			return (X) SerializationHelper.serialize( value );
		}
		else if ( type == BinaryStream.class ) {
			// BinaryStream can only be requested if the value should be serialized
			//noinspection unchecked
			return (X) new ArrayBackedBinaryStream( SerializationHelper.serialize( value ) );
		}
		else if ( type.isArray() ) {
			final Class<?> preferredJavaTypeClass = type.getComponentType();
			final Object unwrapped = Array.newInstance( preferredJavaTypeClass, value.length );
			for ( int i = 0; i < value.length; i++ ) {
				Array.set( unwrapped, i, getElementJavaType().unwrap( value[i], preferredJavaTypeClass, options ) );
			}
			return (X) unwrapped;
		}

		throw unknownUnwrap( type );
	}

	@Override
	public <X> int[] wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}

		if ( value instanceof java.sql.Array array ) {
			try {
				//noinspection unchecked
				value = (X) array.getArray();
			}
			catch ( SQLException ex ) {
				// This basically shouldn't happen unless you've lost connection to the database.
				throw new HibernateException( ex );
			}
		}

		if ( value instanceof int[] ints ) {
			return ints;
		}
		else if ( value instanceof byte[] bytes ) {
			// When the value is a byte[], this is a deserialization request
			return (int[]) SerializationHelper.deserialize( bytes );
		}
		else if ( value instanceof BinaryStream binaryStream ) {
			// When the value is a BinaryStream, this is a deserialization request
			return (int[]) SerializationHelper.deserialize( binaryStream.getBytes() );
		}
		else if ( value.getClass().isArray() ) {
			final int[] wrapped = new int[Array.getLength( value )];
			for ( int i = 0; i < wrapped.length; i++ ) {
				wrapped[i] = getElementJavaType().wrap( Array.get( value, i ), options );
			}
			return wrapped;
		}
		else if ( value instanceof Integer integer ) {
			// Support binding a single element as parameter value
			return new int[]{ integer };
		}
		else if ( value instanceof Collection<?> collection ) {
			final int[] wrapped = new int[collection.size()];
			int i = 0;
			for ( Object e : collection ) {
				wrapped[i++] = getElementJavaType().wrap( e, options );
			}
			return wrapped;
		}

		throw unknownWrap( value.getClass() );
	}

	private static class ArrayMutabilityPlan implements MutabilityPlan<int[]> {

		@Override
		public boolean isMutable() {
			return true;
		}

		@Override
		public int[] deepCopy(int[] value) {
			return value == null ? null : value.clone();
		}

		@Override
		public Serializable disassemble(int[] value, SharedSessionContract session) {
			return deepCopy( value );
		}

		@Override
		public int[] assemble(Serializable cached, SharedSessionContract session) {
			return deepCopy( (int[]) cached );
		}

	}
}
