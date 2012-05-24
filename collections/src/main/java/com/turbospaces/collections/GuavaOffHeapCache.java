package com.turbospaces.collections;

import java.nio.ByteBuffer;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;

import com.esotericsoftware.kryo.ObjectBuffer;
import com.google.common.base.Preconditions;
import com.google.common.cache.AbstractCache;
import com.google.common.cache.CacheStats;
import com.turbospaces.core.JVMUtil;
import com.turbospaces.model.ExplicitCacheEntry;
import com.turbospaces.offmemory.ByteArrayPointer;
import com.turbospaces.pool.ObjectPool;
import com.turbospaces.serialization.DecoratedKryo;

/**
 * This is guava's cache implementation build on top of {@link OffHeapHashSet} with off-heap cache with absolutely same
 * functionality and semantics(and behavior), except this map scale out of java heap space and dramatically reduce the
 * memory needed in java heap for storing pointer(basically for each key-value pair we need only long's 8 bytes heap
 * memory space).</p>
 * 
 * <b> NOTE: </b> - it's not recommended to you this class directly and you would prefer
 * {@link GuavaOffHeapCacheBuilder} instead.
 * 
 * @since 0.1
 * 
 * @param <K>
 *            key type
 * @param <V>
 *            value type
 */
final class GuavaOffHeapCache<K, V> extends AbstractCache<K, V> implements DisposableBean {
    private final Logger logger = LoggerFactory.getLogger( getClass() );
    private final OffHeapHashSet offHeapHashSet;
    private final ObjectPool<ObjectBuffer> objectsPool;
    private final DecoratedKryo kryo;
    private final int ttlAfterWrite;
    private final SimpleStatsCounter statsCounter;

    /**
     * create new guava's cache over off-heap set delegate and associated kryo serializer. also time-2-live must be
     * explicitly passed.
     * 
     * @param offHeapHashSet
     *            off-heap cache collection
     * @param kryo
     *            serialization provider
     * @param ttlAfterWrite
     *            time-to-live after write
     * @param statsCounter
     *            statistics counter
     */
    public GuavaOffHeapCache(final OffHeapHashSet offHeapHashSet,
                             final DecoratedKryo kryo,
                             final int ttlAfterWrite,
                             final SimpleStatsCounter statsCounter) {
        this.offHeapHashSet = offHeapHashSet;
        this.kryo = kryo;
        this.ttlAfterWrite = ttlAfterWrite;
        this.objectsPool = JVMUtil.newObjectBufferPool();
        this.statsCounter = statsCounter;
    }

    @Override
    public V getIfPresent(final Object key) {
        return getIfPresent( key, true );
    }

    @Override
    public V get(final K key,
                 final Callable<? extends V> valueLoader)
                                                         throws ExecutionException {
        V value = getIfPresent( key, true );
        if ( value == null )
            synchronized ( valueLoader ) {
                long nano = 0;
                try {
                    // re-check under lock first and do not increment cache misses as this is internal barrier
                    value = getIfPresent( key, false );
                    if ( value == null ) {
                        if ( statsCounter != null )
                            nano = System.nanoTime();
                        value = valueLoader.call();
                        if ( nano != 0 )
                            statsCounter.recordLoadSuccess( System.nanoTime() - nano );
                        // and finally add loaded value
                        put( key, Preconditions.checkNotNull( value, "Cache loader didn't load any value" ) );
                    }
                }
                catch ( Exception e ) {
                    logger.error( e.getMessage(), e );
                    // record exception
                    if ( nano != 0 )
                        statsCounter.recordLoadException( System.nanoTime() - nano );
                    throw new ExecutionException( e );
                }
            }
        return value;
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void put(final K key,
                    final V value) {
        ExplicitCacheEntry<K, V> e = new ExplicitCacheEntry( Preconditions.checkNotNull( key ), Preconditions.checkNotNull( value ) );
        ObjectBuffer objectBuffer = objectsPool.borrowObject();
        objectBuffer.setKryo( kryo );
        try {
            ByteArrayPointer pointer = new ByteArrayPointer( objectBuffer.writeObjectData( e ), e, ttlAfterWrite );
            offHeapHashSet.put( key, pointer );
        }
        finally {
            objectsPool.returnObject( objectBuffer );
        }
    }

    @Override
    public void invalidate(final Object key) {
        offHeapHashSet.remove( key );
    }

    @Override
    public long size() {
        return offHeapHashSet.size();
    }

    @Override
    public CacheStats stats() {
        Preconditions.checkState( statsCounter != null, "statistics disabled for this class" );
        return statsCounter.snapshot();
    }

    @Override
    public void cleanUp() {
        offHeapHashSet.cleanUp();
    }

    @Override
    public void destroy() {
        offHeapHashSet.destroy();
    }

    @SuppressWarnings("unchecked")
    private V getIfPresent(final Object key,
                           final boolean record) {
        ByteBuffer dataBuffer = offHeapHashSet.getAsSerializedData( key );
        V result = null;

        if ( dataBuffer != null ) {
            ObjectBuffer objectBuffer = objectsPool.borrowObject();
            objectBuffer.setKryo( kryo );

            try {
                ExplicitCacheEntry<K, V> explicitCacheEntry = objectBuffer.readObjectData( dataBuffer.array(), ExplicitCacheEntry.class );
                result = explicitCacheEntry.getBean();
                if ( record && statsCounter != null )
                    statsCounter.recordHits( 1 );
            }
            finally {
                objectsPool.returnObject( objectBuffer );
            }
        }
        else if ( record && statsCounter != null )
            statsCounter.recordMisses( 1 );
        return result;
    }
}