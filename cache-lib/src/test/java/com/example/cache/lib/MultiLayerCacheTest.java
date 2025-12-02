package com.example.cache.lib;

import com.example.cache.lib.impl.InMemoryLayer;
import com.example.cache.lib.metrics.CacheMetrics;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class MultiLayerCacheTest {

    static class SimpleMetrics implements CacheMetrics {
        final ConcurrentHashMap<String, Integer> hits = new ConcurrentHashMap<>();
        @Override public void recordHit(String layerName) { hits.merge(layerName, 1, Integer::sum); }
        @Override public void recordMiss(String layerName) { hits.merge(layerName+":miss", 1, Integer::sum); }
        @Override public void recordPut(String layerName) { hits.merge(layerName+":put", 1, Integer::sum); }
        @Override public void recordEvict(String layerName) { hits.merge(layerName+":evict", 1, Integer::sum); }
        @Override public void recordFileRead(String key) { hits.merge("fileRead", 1, Integer::sum); }
        @Override public void recordFileReadDuration(String key, long durationNanos) { /* ignore */ }
    }

    @Test
    void getPopulatesHigherLayersAndUsesLoader() {
        InMemoryLayer<String, String> top = new InMemoryLayer<>("top", Duration.ofSeconds(60));
        InMemoryLayer<String, String> bottom = new InMemoryLayer<>("bottom", Duration.ofSeconds(60));

        CacheLoader<String, String> loader = new CacheLoader<>() {
            @Override
            public String load(String key) {
                return "loaded-" + key;
            }

            @Override
            public Map<String, String> loadAll(java.util.Collection<String> keys) {
                return java.util.Collections.emptyMap();
            }
        };
        SimpleMetrics metrics = new SimpleMetrics();

        MultiLayerCache<String, String> cache = new MultiLayerCache<String, String>(List.of(top, bottom), loader, metrics);

        Optional<String> before = top.get("k");
        assertFalse(before.isPresent());

        Optional<String> got = cache.get("k");
        assertTrue(got.isPresent());
        assertEquals("loaded-k", got.get());

        // now top layer should be populated
        assertTrue(top.get("k").isPresent());
        assertTrue(bottom.get("k").isPresent());

        // metrics recorded
        assertTrue(metrics.hits.containsKey("fileRead") || metrics.hits.containsKey("top:put"));
    }

    @Test
    void putAndEvictClear() {
        InMemoryLayer<String, String> top = new InMemoryLayer<>("top", Duration.ofSeconds(60));
        InMemoryLayer<String, String> bottom = new InMemoryLayer<>("bottom", Duration.ofSeconds(60));
        SimpleMetrics metrics = new SimpleMetrics();
        MultiLayerCache<String, String> cache = new MultiLayerCache<String, String>(List.of(top, bottom), new CacheLoader<>() {
            @Override public String load(String key) { return null; }
            @Override public Map<String, String> loadAll(java.util.Collection<String> keys) { return java.util.Collections.emptyMap(); }
        }, metrics);

        cache.put("p", "v");
        assertTrue(top.get("p").isPresent());
        assertTrue(bottom.get("p").isPresent());

        cache.evict("p");
        assertFalse(top.get("p").isPresent());
        assertFalse(bottom.get("p").isPresent());

        cache.put("a", "1");
        cache.clear();
        assertFalse(top.get("a").isPresent());
        assertFalse(bottom.get("a").isPresent());
    }
}
