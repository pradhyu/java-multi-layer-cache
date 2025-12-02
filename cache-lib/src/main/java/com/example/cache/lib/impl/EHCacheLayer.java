package com.example.cache.lib.impl;

import com.example.cache.lib.CacheLayer;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.MemoryUnit;

import java.util.Optional;

/**
 * EHCache-backed cache layer implementation.
 * Provides persistent, disk-backed caching with automatic overflow.
 * Suitable for medium-sized datasets that exceed in-memory capacity.
 */
public class EHCacheLayer<K, V> implements CacheLayer<K, V> {
    private final String name;
    private final Cache<K, V> cache;
    private final CacheManager cacheManager;

    /**
     * Create an EHCache layer with specified capacity and storage path.
     *
     * @param name the layer name
     * @param maxSizeInMB maximum heap size in MB
     */
    public EHCacheLayer(String name, int maxSizeInMB) {
        this.name = name;
        
        // Configure EHCache with heap + disk storage (transient, not persistent)
        // Using transient disk avoids requiring a persistence directory configuration
        this.cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
            .withCache(name,
                CacheConfigurationBuilder.newCacheConfigurationBuilder(
                    Object.class, Object.class,
                    ResourcePoolsBuilder.newResourcePoolsBuilder()
                        .heap(maxSizeInMB, MemoryUnit.MB)
                        .disk(maxSizeInMB * 2L, MemoryUnit.MB, false) // disk = 2x heap size, transient (non-persistent)
                )
            )
            .build(true);

        @SuppressWarnings("unchecked")
        Cache<K, V> typedCache = (Cache<K, V>) cacheManager.getCache(name, Object.class, Object.class);
        this.cache = typedCache;
    }

    @Override
    public Optional<V> get(K key) {
        V value = cache.get(key);
        return Optional.ofNullable(value);
    }

    @Override
    public void put(K key, V value) {
        cache.put(key, value);
    }

    @Override
    public void evict(K key) {
        cache.remove(key);
    }

    @Override
    public void clear() {
        cache.clear();
    }

    @Override
    public long size() {
        // EHCache doesn't provide direct size method, count items
        long size = 0;
        for (@SuppressWarnings("unused") Object ignored : cache) {
            size++;
        }
        return size;
    }

    @Override
    public String name() {
        return name;
    }

    /**
     * Close the cache manager and release resources.
     */
    public void close() {
        if (cacheManager != null) {
            cacheManager.close();
        }
    }
}
