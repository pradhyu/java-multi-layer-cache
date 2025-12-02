package com.example.cache.app;

import com.example.cache.lib.metrics.CacheMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

/**
 * Implementation of CacheMetrics using Micrometer for production metrics collection.
 */
@Component
public class CacheMetricsImpl implements CacheMetrics {
    private final MeterRegistry meterRegistry;

    public CacheMetricsImpl(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void recordHit(String layerName) {
        meterRegistry.counter("cache.hit", "layer", layerName).increment();
    }

    @Override
    public void recordMiss(String layerName) {
        meterRegistry.counter("cache.miss", "layer", layerName).increment();
    }

    @Override
    public void recordPut(String layerName) {
        meterRegistry.counter("cache.put", "layer", layerName).increment();
    }

    @Override
    public void recordEvict(String layerName) {
        meterRegistry.counter("cache.evict", "layer", layerName).increment();
    }

    @Override
    public void recordFileRead(String key) {
        meterRegistry.counter("file.read", "key", key).increment();
    }

    @Override
    public void recordFileReadDuration(String key, long durationNanos) {
        Timer.builder("file.read.duration")
                .tag("key", key)
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry)
                .record(durationNanos, java.util.concurrent.TimeUnit.NANOSECONDS);
    }
}
