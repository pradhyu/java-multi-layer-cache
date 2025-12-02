package com.example.cache.app;

import com.example.cache.lib.MultiLayerCache;
import com.example.cache.lib.impl.InMemoryLayer;
import com.example.cache.lib.metrics.CacheMetrics;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CacheServiceTest {

    static class NoopMetrics implements CacheMetrics {
        @Override public void recordHit(String layerName) {}
        @Override public void recordMiss(String layerName) {}
        @Override public void recordPut(String layerName) {}
        @Override public void recordEvict(String layerName) {}
        @Override public void recordFileRead(String key) {}
        @Override public void recordFileReadDuration(String key, long durationNanos) {}
    }

    @Test
    void servicePutGetEvictClear() {
        InMemoryLayer<String, List<String>> top = new InMemoryLayer<>("top", Duration.ofSeconds(60));
        InMemoryLayer<String, List<String>> bottom = new InMemoryLayer<>("bottom", Duration.ofSeconds(60));

        MultiLayerCache<String, List<String>> mlc = new MultiLayerCache<>(List.of(top, bottom), new com.example.cache.lib.CacheLoader<>() {
            @Override public List<String> load(String key) { return null; }
            @Override public java.util.Map<String, List<String>> loadAll(java.util.Collection<String> keys) { return java.util.Collections.emptyMap(); }
        }, new NoopMetrics());

        CacheService svc = new CacheService(mlc);

        svc.put("k", List.of("a","b"));
        assertTrue(svc.get("k").isPresent());

        svc.evict("k");
        assertFalse(svc.get("k").isPresent());

        svc.put("x", List.of("z"));
        svc.clear();
        assertFalse(svc.get("x").isPresent());
    }
}
