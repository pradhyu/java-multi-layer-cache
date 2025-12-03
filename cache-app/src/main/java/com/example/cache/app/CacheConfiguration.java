package com.example.cache.app;

import com.example.cache.lib.CacheLayer;
import com.example.cache.lib.CacheLoader;
import com.example.cache.lib.MultiLayerCache;
import com.example.cache.lib.impl.InMemoryLayer;
import com.example.cache.lib.impl.JCacheEhCacheLayer;
import com.example.cache.lib.loader.FileBackedLoader;
import com.example.cache.lib.metrics.CacheMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Configuration for the multi-layer cache application.
 * Sets up the cache layers, loader, and metrics.
 * 
 * Layer hierarchy:
 * L1: In-Memory (5m TTL) - Ultra-fast access for hot data
 * L2: EhCache with JCache wrapper - Disk-backed cache with automatic Micrometer
 * metrics
 * L3: File-Backed (CSV) - Persistent file storage, slowest layer
 */
@Configuration
public class CacheConfiguration {

    @Bean
    public MultiLayerCache<String, List<String>> multiLayerCache(
            CacheMetrics metrics,
            MeterRegistry meterRegistry) throws Exception {
        // Layer 0: L1 cache (in-memory, short TTL, fast)
        CacheLayer<String, List<String>> l1 = new InMemoryLayer<>("L1-Memory", Duration.ofMinutes(5));

        // Layer 1: L2 cache (EhCache with JCache wrapper, automatic Micrometer metrics)
        @SuppressWarnings("unchecked")
        CacheLayer<String, List<String>> l2 = new JCacheEhCacheLayer<>(
                "L2-EhCache",
                50, // 50 MB heap
                String.class,
                (Class<List<String>>) (Class<?>) List.class,
                meterRegistry // Automatic metrics registration!
        );

        // Layer 2: L3 cache (file-backed, persistent, CSV format)
        Path cacheDir = Path.of(System.getProperty("java.io.tmpdir"), "cache-data");
        Files.createDirectories(cacheDir);

        // Create a sample CSV file for demonstration
        Path sampleFile = cacheDir.resolve("data.csv");
        if (!Files.exists(sampleFile)) {
            Files.writeString(sampleFile,
                    """
                            key,value1,value2,value3
                            user:1,John,Doe,Active
                            user:2,Jane,Smith,Active
                            product:1,Laptop,Electronics,Available
                            product:2,Phone,Electronics,Available
                            """,
                    StandardCharsets.UTF_8);
        }

        CacheLoader<String, List<String>> fileLoader = new FileBackedLoader(
                Collections.singletonList(sampleFile),
                ',',
                true,
                StandardCharsets.UTF_8);

        List<CacheLayer<String, List<String>>> layers = Arrays.asList(l1, l2);

        return new MultiLayerCache<>(layers, fileLoader, metrics);
    }

    @Bean
    CacheService cacheService(MultiLayerCache<String, List<String>> multiLayerCache) {
        return new CacheService(multiLayerCache);
    }
}
