package com.example.cache.app;

import com.example.cache.lib.CacheLayer;
import com.example.cache.lib.CacheLoader;
import com.example.cache.lib.MultiLayerCache;
import com.example.cache.lib.impl.InMemoryLayer;
import com.example.cache.lib.loader.FileBackedLoader;
import com.example.cache.lib.metrics.CacheMetrics;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Configuration for the multi-layer cache application.
 * Sets up the cache layers, loader, and metrics.
 */
@Configuration
public class CacheConfiguration {

    @Bean
    public MultiLayerCache<String, List<String>> multiLayerCache(CacheMetrics metrics) throws Exception {
        // Create cache layers with different TTLs
        // Layer 0: L1 cache (in-memory, short TTL, fast)
        CacheLayer<String, List<String>> l1 = new InMemoryLayer<>("L1-Memory", Duration.ofMinutes(5));
        
        // Layer 1: L2 cache (in-memory, longer TTL)
        CacheLayer<String, List<String>> l2 = new InMemoryLayer<>("L2-Memory", Duration.ofHours(1));
        
        // Layer 2: L3 cache (file-backed, persistent, CSV format)
        Path cacheDir = Paths.get(System.getProperty("java.io.tmpdir"), "cache-data");
        Files.createDirectories(cacheDir);
        
        // Create a sample CSV file for demonstration
        Path sampleFile = cacheDir.resolve("data.csv");
        if (!Files.exists(sampleFile)) {
            Files.writeString(sampleFile, 
                "key,value1,value2,value3\n" +
                "user:1,John,Doe,Active\n" +
                "user:2,Jane,Smith,Active\n" +
                "product:1,Laptop,Electronics,Available\n" +
                "product:2,Phone,Electronics,Available\n",
                StandardCharsets.UTF_8);
        }
        
        CacheLoader<String, List<String>> fileLoader = new FileBackedLoader(
            Collections.singletonList(sampleFile), 
            ',', 
            true, 
            StandardCharsets.UTF_8
        );
        
        List<CacheLayer<String, List<String>>> layers = Arrays.asList(l1, l2);
        
        return new MultiLayerCache<>(layers, fileLoader, metrics);
    }

    @Bean
    public CacheService cacheService(MultiLayerCache<String, List<String>> multiLayerCache) {
        return new CacheService(multiLayerCache);
    }
}
