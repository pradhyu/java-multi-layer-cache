package com.example.cache.lib.impl;

import com.example.cache.lib.CacheLayer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.cache.JCacheMetrics;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.MemoryUnit;
import org.ehcache.jsr107.EhcacheCachingProvider;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.Configuration;
import javax.cache.spi.CachingProvider;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.Optional;
import java.util.Set;

/**
 * JCache (JSR-107) wrapper for EHCache with full Micrometer metrics support.
 * 
 * This implementation uses the JCache API which provides:
 * - Standardized cache statistics (hits, misses, evictions)
 * - Built-in Micrometer integration via JCacheMetrics
 * - Portable code that works with any JCache-compliant provider
 * 
 * Metrics exposed:
 * - cache.gets (tagged with result=hit/miss)
 * - cache.puts
 * - cache.evictions
 * - cache.removals
 * - cache.hit.ratio
 */
public class JCacheEhCacheLayer<K, V> implements CacheLayer<K, V> {
    private final String name;
    private final Cache<K, V> cache;
    private final CacheManager cacheManager;

    /**
     * Create a JCache-wrapped EHCache layer with Micrometer metrics.
     *
     * @param name          the layer name
     * @param maxSizeInMB   maximum heap size in MB
     * @param keyClass      the key class type
     * @param valueClass    the value class type
     * @param meterRegistry Micrometer registry for metrics (can be null)
     */
    public JCacheEhCacheLayer(String name, int maxSizeInMB,
            Class<K> keyClass, Class<V> valueClass,
            MeterRegistry meterRegistry) {
        this.name = name;

        // Get EhCache's JCache provider
        CachingProvider cachingProvider = Caching.getCachingProvider(
                "org.ehcache.jsr107.EhcacheCachingProvider");

        // Create cache manager
        this.cacheManager = cachingProvider.getCacheManager();

        // Build EhCache configuration
        org.ehcache.config.CacheConfiguration<K, V> ehcacheConfig = CacheConfigurationBuilder
                .newCacheConfigurationBuilder(
                        keyClass, valueClass,
                        ResourcePoolsBuilder.newResourcePoolsBuilder()
                                .heap(maxSizeInMB, MemoryUnit.MB)
                                .disk(maxSizeInMB * 2L, MemoryUnit.MB, false))
                .build();

        // Wrap EhCache config in JCache config
        Configuration<K, V> jcacheConfig = org.ehcache.jsr107.Eh107Configuration
                .fromEhcacheCacheConfiguration(ehcacheConfig);

        // Create cache with statistics enabled
        this.cache = cacheManager.createCache(name, jcacheConfig);

        // Enable statistics
        cache.getCacheManager().enableStatistics(name, true);

        // Register with Micrometer if provided
        if (meterRegistry != null) {
            JCacheMetrics.monitor(meterRegistry, cache, name);
        }
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
        // JCache doesn't provide a standard size() method
        // We need to iterate (expensive) or track separately
        long count = 0;
        for (@SuppressWarnings("unused")
        Cache.Entry<K, V> ignored : cache) {
            count++;
        }
        return count;
    }

    @Override
    public String name() {
        return name;
    }

    /**
     * Get JCache statistics directly.
     * Note: This requires JMX to be enabled and statistics to be enabled on the
     * cache.
     * 
     * @return String representation of cache statistics
     */
    public String getStatisticsInfo() {
        try {
            // Access statistics through JMX if available
            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

            ObjectName objectName = new ObjectName(
                    "javax.cache:type=CacheStatistics,CacheManager=*,Cache=" + name);

            Set<ObjectName> names = mBeanServer.queryNames(objectName, null);

            if (!names.isEmpty()) {
                ObjectName statsName = names.iterator().next();
                long hits = (Long) mBeanServer.getAttribute(statsName, "CacheHits");
                long misses = (Long) mBeanServer.getAttribute(statsName, "CacheMisses");
                float hitPercentage = (Float) mBeanServer.getAttribute(statsName, "CacheHitPercentage");

                return String.format("Cache '%s': Hits=%d, Misses=%d, HitRate=%.2f%%",
                        name, hits, misses, hitPercentage);
            }
        } catch (Exception e) {
            // JMX not available or statistics not enabled
        }
        return String.format("Cache '%s': Statistics not available (JMX may not be enabled)", name);
    }

    /**
     * Close the cache and release resources.
     */
    public void close() {
        if (cache != null && !cache.isClosed()) {
            cache.close();
        }
        if (cacheManager != null && !cacheManager.isClosed()) {
            cacheManager.close();
        }
    }
}
