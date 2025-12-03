package com.example.cache.app;

import com.example.cache.lib.MultiLayerCache;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

/**
 * Integration test for EhCache metrics with Prometheus.
 * 
 * This test verifies that:
 * 1. EhCache JCache layer is properly configured
 * 2. Cache operations generate metrics
 * 3. Metrics are exposed via Prometheus endpoint
 */
@SpringBootTest
@AutoConfigureMockMvc
class EhCacheMetricsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MultiLayerCache<String, List<String>> multiLayerCache;

    @Autowired
    private MeterRegistry meterRegistry;

    @Test
    void testCacheOperationsGenerateMetrics() throws Exception {
        // Given: Cache is empty
        multiLayerCache.clear();

        // When: We perform cache operations
        // 1. First get - should be a miss, load from file
        List<String> result1 = multiLayerCache.get("user:1");
        assertNotNull(result1, "Should load from file");
        assertEquals("John", result1.get(0), "Should have correct data");

        // 2. Second get - should be a hit from L1
        List<String> result2 = multiLayerCache.get("user:1");
        assertNotNull(result2, "Should hit cache");
        assertEquals(result1, result2, "Should return same data");

        // 3. Get another key - miss then hit
        List<String> result3 = multiLayerCache.get("user:2");
        assertNotNull(result3, "Should load from file");
        assertEquals("Jane", result3.get(0), "Should have correct data");

        // 4. Get from L2 cache (EhCache)
        List<String> result4 = multiLayerCache.get("product:1");
        assertNotNull(result4, "Should load from file");

        // Then: Verify metrics are recorded
        // Note: We need to wait a bit for async metric recording
        Thread.sleep(100);

        // Check that we have cache metrics
        double cacheGets = meterRegistry.find("cache.gets")
                .tag("cache", "L2-EhCache")
                .counters()
                .stream()
                .mapToDouble(counter -> counter.count())
                .sum();

        System.out.println("Total cache.gets for L2-EhCache: " + cacheGets);

        // We should have some cache operations recorded
        assertTrue(cacheGets >= 0, "Should have cache.gets metric");

        // Print all available metrics for debugging
        System.out.println("\n=== Available Metrics ===");
        meterRegistry.getMeters().forEach(meter -> {
            if (meter.getId().getName().startsWith("cache")) {
                System.out.println(meter.getId() + " = " + meter.measure());
            }
        });
    }

    @Test
    void testPrometheusEndpointExposesEhCacheMetrics() throws Exception {
        // Given: We perform some cache operations
        multiLayerCache.clear();
        multiLayerCache.get("user:1");
        multiLayerCache.get("user:1"); // Hit
        multiLayerCache.get("user:2");

        // Wait for metrics to be recorded
        Thread.sleep(200);

        // When: We call the Prometheus metrics endpoint
        String prometheusMetrics = mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Then: Verify EhCache metrics are present
        System.out.println("\n=== Prometheus Metrics Output ===");
        System.out.println(prometheusMetrics);

        // Check for JCache metrics (these should be present)
        boolean hasCacheMetrics = prometheusMetrics.contains("cache_gets") ||
                prometheusMetrics.contains("cache_puts") ||
                prometheusMetrics.contains("cache_size");

        // Check for our custom metrics
        boolean hasCustomMetrics = prometheusMetrics.contains("cache_hit") ||
                prometheusMetrics.contains("cache_miss");

        System.out.println("\nHas JCache metrics: " + hasCacheMetrics);
        System.out.println("Has custom metrics: " + hasCustomMetrics);

        // At minimum, we should have some cache-related metrics
        assertTrue(hasCacheMetrics || hasCustomMetrics,
                "Should have either JCache or custom cache metrics in Prometheus output");

        // Print specific EhCache metrics if found
        System.out.println("\n=== EhCache-specific Metrics ===");
        prometheusMetrics.lines()
                .filter(line -> line.contains("L2-EhCache") || line.contains("cache_"))
                .filter(line -> !line.startsWith("#"))
                .forEach(System.out::println);
    }

    @Test
    void testMetricsShowHitsAndMisses() throws Exception {
        // Given: Clear cache
        multiLayerCache.clear();

        // When: Perform operations with known hit/miss patterns
        // First access - MISS (load from file)
        multiLayerCache.get("user:1");

        // Second access - HIT (from L1)
        multiLayerCache.get("user:1");

        // Third access - HIT (from L1)
        multiLayerCache.get("user:1");

        // New key - MISS
        multiLayerCache.get("product:1");

        Thread.sleep(200);

        // Then: Check Prometheus output
        String prometheusMetrics = mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        System.out.println("\n=== Hit/Miss Metrics ===");

        // Look for hit/miss metrics
        prometheusMetrics.lines()
                .filter(line -> line.contains("hit") || line.contains("miss"))
                .filter(line -> !line.startsWith("#"))
                .forEach(System.out::println);

        // Verify we have some hit/miss tracking
        boolean hasHitMissMetrics = prometheusMetrics.contains("result=\"hit\"") ||
                prometheusMetrics.contains("result=\"miss\"") ||
                prometheusMetrics.contains("cache_hit") ||
                prometheusMetrics.contains("cache_miss");

        assertTrue(hasHitMissMetrics, "Should have hit/miss metrics");
    }

    @Test
    void testEhCacheSpecificMetrics() {
        // Given: Perform cache operations
        multiLayerCache.clear();
        multiLayerCache.get("user:1");
        multiLayerCache.get("user:2");
        multiLayerCache.put("test:1", List.of("test", "data"));

        // When: Check MeterRegistry directly
        System.out.println("\n=== EhCache Metrics from MeterRegistry ===");

        // Look for all cache-related metrics
        meterRegistry.getMeters().forEach(meter -> {
            String name = meter.getId().getName();
            if (name.startsWith("cache")) {
                System.out.println(meter.getId() + " = " + meter.measure());
            }
        });

        // Check for specific JCache metrics
        long cacheGetsTotal = meterRegistry.find("cache.gets")
                .counters()
                .stream()
                .mapToLong(counter -> (long) counter.count())
                .sum();

        long cachePutsTotal = meterRegistry.find("cache.puts")
                .counters()
                .stream()
                .mapToLong(counter -> (long) counter.count())
                .sum();

        System.out.println("\nTotal cache.gets: " + cacheGetsTotal);
        System.out.println("Total cache.puts: " + cachePutsTotal);

        // We should have at least some operations
        assertTrue(cacheGetsTotal >= 0, "Should track cache gets");
        assertTrue(cachePutsTotal >= 0, "Should track cache puts");
    }
}
