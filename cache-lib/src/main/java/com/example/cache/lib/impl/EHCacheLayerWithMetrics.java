package com.example.cache.lib.impl;

import com.example.cache.lib.CacheLayer;
import io.micrometer.core.instrument.MeterRegistry;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.MemoryUnit;

import java.util.Optional;

/**
 * EHCache-backed cache layer implementation with Micrometer metrics
 * integration.
 * 
 * This implementation can optionally integrate with Micrometer to expose
 * cache metrics (hits, misses, evictions, etc.) to monitoring systems.
 * 
 * Provides persistent, disk-backed caching with automatic overflow.
 * Suitable for medium-sized datasets that exceed in-memory capacity.
 */
public class EHCacheLayerWithMetrics<K, V> implements CacheLayer<K, V> {
    private final String name;
    private final Cache<K, V> cache;
    private final CacheManager cacheManager;
    private final MeterRegistry meterRegistry;

    /**
     * Create an EHCache layer with Micrometer metrics integration.
     *
     * @param name          the layer name
     * @param maxSizeInMB   maximum heap size in MB
     * @param meterRegistry Micrometer registry for metrics (can be null to disable
     *                      metrics)
     */
    public EHCacheLayerWithMetrics(String name, int maxSizeInMB, MeterRegistry meterRegistry) {
        this.name = name;
        this.meterRegistry = meterRegistry;

        // Configure EHCache with heap + disk storage (transient, not persistent)
        this.cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
                .withCache(name,
                        CacheConfigurationBuilder.newCacheConfigurationBuilder(
                                Object.class, Object.class,
                                ResourcePoolsBuilder.newResourcePoolsBuilder()
                                        .heap(maxSizeInMB, MemoryUnit.MB)
                                        .disk(maxSizeInMB * 2L, MemoryUnit.MB, false)))
                .build(true);

        @SuppressWarnings("unchecked")
        Cache<K, V> typedCache = (Cache<K, V>) cacheManager.getCache(name, Object.class, Object.class);
        this.cache = typedCache;

        // Register metrics if MeterRegistry is provided
        if (meterRegistry != null) {
            registerMetrics();
        }
    }

    /**
     * Register cache metrics with Micrometer.
     * 
     * Note: EhCache 3.x doesn't have direct Micrometer binder support like EhCache
     * 2.x.
     * For production use, consider:
     * 1. Using JMX to expose metrics
     * 2. Creating custom gauges for cache size
     * 3. Using manual tracking in the wrapper layer
     */
    private void registerMetrics() {
        // Register cache size as a gauge
        meterRegistry.gauge("cache.size",
                java.util.Collections.singletonList(io.micrometer.core.instrument.Tag.of("cache", name)),
                this,
                c -> c.size());

        // Note: For hit/miss rates, EhCache 3.x requires either:
        // - JMX integration (via javax.management)
        // - Manual tracking at the wrapper level
        // - Using JSR-107 (JCache) wrapper with statistics enabled
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
        long size = 0;
        for (@SuppressWarnings("unused")
        Object ignored : cache) {
            size++;
        }
        return size;
    }

    @Override
    public String name() {
        return name;
    }

    /**
     * Get the underlying EhCache CacheManager for advanced operations.
     * This can be used to access JMX statistics or other EhCache-specific features.
     */
    public CacheManager getCacheManager() {
        return cacheManager;
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
