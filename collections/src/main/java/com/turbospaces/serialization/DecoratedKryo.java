/**
 * Copyright (C) 2011-2012 Andrey Borisov <aandrey.borisov@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.turbospaces.serialization;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentMap;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.SerializationException;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.serialize.BigDecimalSerializer;
import com.esotericsoftware.kryo.serialize.BigIntegerSerializer;
import com.esotericsoftware.kryo.serialize.CollectionSerializer;
import com.esotericsoftware.kryo.serialize.DateSerializer;
import com.esotericsoftware.kryo.serialize.EnumSerializer;
import com.esotericsoftware.kryo.serialize.MapSerializer;
import com.esotericsoftware.kryo.serialize.SimpleSerializer;
import com.google.common.collect.Maps;
import com.turbospaces.model.CacheStoreEntryWrapper;
import com.turbospaces.model.ExplicitCacheEntry;

/**
 * This is decorated version of {@link Kryo} - adds some more user predictable behavior (like registering JDK
 * collection, arrays, enums over user provided classes) - we consider this sugar enhancements as reasonable defaults.
 * 
 * @since 0.1
 */
@SuppressWarnings({ "rawtypes" })
public class DecoratedKryo extends Kryo {
    private ConcurrentMap<Class, RegisteredClass> serializers;

    /**
     * default constructor that performs decoration.
     */
    public DecoratedKryo() {
        register( Date.class, new DateSerializer() );
        register( BigDecimal.class, new BigDecimalSerializer() );
        register( BigInteger.class, new BigIntegerSerializer() );

        register( Map.class, new MapSerializer( this ) );
        register( List.class, new CollectionSerializer( this ) );
        register( Set.class, new CollectionSerializer( this ) );
        register( HashMap.class, new MapSerializer( this ) );

        register( TreeMap.class, new MapSerializer( this ) );
        register( LinkedHashMap.class, new MapSerializer( this ) );
        register( Hashtable.class, new MapSerializer( this ) );
        register( Properties.class, new MapSerializer( this ) );
        register( ArrayList.class, new CollectionSerializer( this ) );
        register( LinkedList.class, new CollectionSerializer( this ) );
        register( HashSet.class, new CollectionSerializer( this ) );
        register( TreeSet.class, new CollectionSerializer( this ) );
        register( LinkedHashSet.class, new CollectionSerializer( this ) );

        register( boolean[].class, new SingleDimensionArraySerializer( boolean[].class, this ) );
        register( byte[].class, new SingleDimensionArraySerializer( byte[].class, this ) );
        register( short[].class, new SingleDimensionArraySerializer( short[].class, this ) );
        register( char[].class, new SingleDimensionArraySerializer( char[].class, this ) );
        register( int[].class, new SingleDimensionArraySerializer( int[].class, this ) );
        register( float[].class, new SingleDimensionArraySerializer( float[].class, this ) );
        register( double[].class, new SingleDimensionArraySerializer( double[].class, this ) );
        register( long[].class, new SingleDimensionArraySerializer( long[].class, this ) );
        register( byte[][].class, new NetworkCommunicationSerializer() );

        register( Boolean[].class, new SingleDimensionArraySerializer( Boolean[].class, this ) );
        register( Byte[].class, new SingleDimensionArraySerializer( Byte[].class, this ) );
        register( Short[].class, new SingleDimensionArraySerializer( Short[].class, this ) );
        register( Character[].class, new SingleDimensionArraySerializer( Character[].class, this ) );
        register( Integer[].class, new SingleDimensionArraySerializer( Integer[].class, this ) );
        register( Float[].class, new SingleDimensionArraySerializer( Float[].class, this ) );
        register( Double[].class, new SingleDimensionArraySerializer( Double[].class, this ) );
        register( Long[].class, new SingleDimensionArraySerializer( Long[].class, this ) );
        register( String[].class, new SingleDimensionArraySerializer( String[].class, this ) );

        register( CacheStoreEntryWrapper.class, new SimpleSerializer<CacheStoreEntryWrapper>() {
            @Override
            public CacheStoreEntryWrapper read(final ByteBuffer buffer) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void write(final ByteBuffer buffer,
                              final CacheStoreEntryWrapper entry) {
                Serializer serializer = getRegisteredClass( entry.getPersistentEntity().getOriginalPersistentEntity().getType() ).getSerializer();
                ( (PropertiesSerializer) serializer ).write( buffer, entry );
            }
        } );
        register( ExplicitCacheEntry.class, new ExplicitCacheEntrySerializer( this ) );
        register( RoundingMode.class, new EnumSerializer( RoundingMode.class ) );
    }

    @Override
    public RegisteredClass register(final Class type,
                                    final Serializer serializer) {
        RegisteredClass regClass = super.register( type, serializer );
        if ( serializers == null )
            serializers = Maps.newConcurrentMap();
        serializers.put( type, regClass );
        return regClass;
    }

    /**
     * check whether given class has been registered.
     * 
     * @param type
     *            user type
     * @return true if class has been registered
     */
    public boolean isTypeRegistered(final Class<?> type) {
        return serializers.containsKey( type );
    }

    /**
     * de-serialize the entitie's state from byte array to POJO state without changing position/limit/mark or any other
     * attributes of byte buffer.
     * 
     * @param source
     *            byte array representation of entity (byte buffer)
     * @param clazz
     *            persistent class
     * @return POJO state
     * @throws SerializationException
     *             IO exception if entity can't be de-serialized (for example due to file corruption or any other IO
     *             problems)
     */
    public SerializationEntry deserialize(final ByteBuffer source,
                                          final Class<?> clazz) {
        source.clear();
        SerializationEntry entry = ( (PropertiesSerializer) getSerializer( clazz ) ).readToSerializedEntry( source );
        source.clear();
        return entry;
    }

    /**
     * perform find-by-example operation over serialized object's byte array storage and given template class without
     * changing byte buffer's position/limit or any other attributes.
     * 
     * @param source
     *            byte array representation (byte buffer array)
     * @param cacheEntryTemplate
     *            find-by-example template
     * 
     * @return true if de-serialized state of the entity matched by template
     * @throws SerializationException
     *             if for some reason source's state can't be matched with template
     */
    public boolean matchByTemplate(final ByteBuffer source,
                                   final CacheStoreEntryWrapper cacheEntryTemplate)
                                                                                   throws SerializationException {
        source.clear();
        Serializer serializer = getSerializer( cacheEntryTemplate.getPersistentEntity().getOriginalPersistentEntity().getType() );
        boolean match = ( (PropertiesSerializer) serializer ).matches( source, cacheEntryTemplate );
        source.clear();
        return match;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append( "serializers) = " ).append( "\n" );
        for ( Entry<Class, RegisteredClass> entry : serializers.entrySet() ) {
            Class<?> key = entry.getKey();
            builder.append( "\t" );
            builder.append( key.getName() + " -> " + entry.getValue().getSerializer() );
            builder.append( "\n" );
        }
        return "Kryo(" + builder.toString();
    }

    static void writePropertyValue(final DecoratedKryo kryo,
                                   final CachedSerializationProperty cachedProperty,
                                   final Object value,
                                   final ByteBuffer buffer) {
        Serializer serializer = cachedProperty.getSerializer();

        if ( cachedProperty.isFinal() ) {
            if ( serializer == null )
                cachedProperty.setSerializer( kryo.getRegisteredClass( cachedProperty.getPropertyType() ).getSerializer() );
            cachedProperty.write( buffer, value );
        }
        else {
            if ( value == null ) {
                kryo.writeClass( buffer, null );
                return;
            }
            RegisteredClass registeredClass = kryo.writeClass( buffer, value.getClass() );

            if ( serializer == null )
                serializer = registeredClass.getSerializer();
            serializer.writeObjectData( buffer, value );
        }
    }

    @SuppressWarnings("unchecked")
    static Object readPropertyValue(final DecoratedKryo kryo,
                                    final CachedSerializationProperty cachedProperty,
                                    final ByteBuffer buffer) {
        Object value = null;
        Serializer serializer = cachedProperty.getSerializer();
        if ( cachedProperty.isFinal() ) {
            if ( serializer == null )
                cachedProperty.setSerializer( kryo.getRegisteredClass( cachedProperty.getPropertyType() ).getSerializer() );
            value = cachedProperty.read( buffer, cachedProperty.getPropertyType() );
        }
        else {
            RegisteredClass registeredClass = kryo.readClass( buffer );
            if ( registeredClass != null ) {
                if ( serializer == null )
                    serializer = registeredClass.getSerializer();
                value = serializer.readObjectData( buffer, registeredClass.getType() );
            }
        }
        return value;
    }
}
