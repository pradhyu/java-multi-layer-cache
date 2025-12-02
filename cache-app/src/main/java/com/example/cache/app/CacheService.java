package com.example.cache.app;

import com.example.cache.lib.MultiLayerCache;

import java.util.List;
import java.util.Optional;

/**
 * Service layer for cache operations.
 * Provides high-level cache operations for the application.
 */
public class CacheService {
    private final MultiLayerCache<String, List<String>> cache;

    public CacheService(MultiLayerCache<String, List<String>> cache) {
        this.cache = cache;
    }

    /**
     * Get a value from the cache by key.
     * @param key the cache key
     * @return optional containing the cached value, or empty if not found
     */
    public Optional<List<String>> get(String key) {
        return cache.get(key);
    }

    /**
     * Put a value into the cache.
     * @param key the cache key
     * @param value the value to cache
     */
    public void put(String key, List<String> value) {
        cache.put(key, value);
    }

    /**
     * Remove a value from the cache.
     * @param key the cache key
     */
    public void evict(String key) {
        cache.evict(key);
    }

    /**
     * Clear all cache entries.
     */
    public void clear() {
        cache.clear();
    }
}
