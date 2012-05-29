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

import java.nio.ByteBuffer;

import com.turbospaces.model.ExplicitCacheEntry;

/**
 * kryo serializer for {@link ExplicitCacheEntry}
 * 
 * @since 0.1
 * @see PropertiesSerializer
 */
public final class ExplicitCacheEntrySerializer extends MatchingSerializer<ExplicitCacheEntry<?, ?>> {

    /**
     * create new explicit serializer suitable to work with {@link ExplicitCacheEntry} beans.
     * 
     * @param kryo
     *            serialization provider
     */
    public ExplicitCacheEntrySerializer(final DecoratedKryo kryo) {
        super( kryo, new CachedSerializationProperty[] //
                { new CachedSerializationProperty( Object.class ), // key
                        new CachedSerializationProperty( Integer.class ), // version
                        new CachedSerializationProperty( Object.class ), // routing
                        new CachedSerializationProperty( Object.class ) // object
                } );
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public ExplicitCacheEntry<?, ?> read(final ByteBuffer buffer) {
        Object id = readPropertyValue( cachedProperties[0], buffer ); // key
        Integer version = (Integer) readPropertyValue( cachedProperties[1], buffer ); // version
        Object routing = readPropertyValue( cachedProperties[2], buffer ); // routing
        Object bean = readPropertyValue( cachedProperties[3], buffer ); // object

        return new ExplicitCacheEntry( id, bean ).withRouting( routing ).withVersion( version );
    }

    @Override
    public void write(final ByteBuffer buffer,
                      final ExplicitCacheEntry<?, ?> object) {
        Object id = object.getKey();
        Integer version = object.getVersion();
        Object routing = object.getRouting();
        Object bean = object.getBean();

        writePropertyValue( cachedProperties[0], id, buffer ); // key
        writePropertyValue( cachedProperties[1], version, buffer ); // version
        writePropertyValue( cachedProperties[2], routing, buffer ); // routing
        writePropertyValue( cachedProperties[3], bean, buffer ); // obj
    }

    @Override
    public Object readID(final ByteBuffer buffer) {
        buffer.clear();
        Object id = readPropertyValue( cachedProperties[0], buffer ); // key
        buffer.clear();
        return id;
    }

    @Override
    public Class<?> getType() {
        return ExplicitCacheEntry.class;
    }
}
