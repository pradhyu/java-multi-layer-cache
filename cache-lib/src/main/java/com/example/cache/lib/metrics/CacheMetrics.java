package com.example.cache.lib.metrics;

/**
 * Metrics interface for tracking cache operations and performance.
 */
public interface CacheMetrics {
    /**
     * Record a cache hit for the given layer.
     */
    void recordHit(String layerName);

    /**
     * Record a cache miss for the given layer.
     */
    void recordMiss(String layerName);

    /**
     * Record a put operation for the given layer.
     */
    void recordPut(String layerName);

    /**
     * Record an evict operation for the given layer.
     */
    void recordEvict(String layerName);

    /**
     * Record a file read operation.
     */
    void recordFileRead(String key);

    /**
     * Record the duration of a file read operation in nanoseconds.
     */
    void recordFileReadDuration(String key, long durationNanos);
}
