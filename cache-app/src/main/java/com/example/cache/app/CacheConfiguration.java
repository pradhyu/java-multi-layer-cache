package com.example.cache.app;

import com.example.cache.lib.CacheLayer;
import com.example.cache.lib.CacheLoader;
import com.example.cache.lib.MultiLayerCache;
import com.example.cache.lib.impl.InMemoryLayer;
import com.example.cache.lib.impl.JCacheEhCacheLayer;
import com.example.cache.lib.impl.RedisCacheLayer;
import com.example.cache.lib.loader.FileBackedLoader;
import com.example.cache.lib.metrics.CacheMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

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

        @org.springframework.beans.factory.annotation.Value("${redis.host:localhost}")
        private String redisHost;

        @org.springframework.beans.factory.annotation.Value("${redis.port:6379}")
        private int redisPort;

        @Bean(destroyMethod = "close")
        public JedisPool jedisPool() {
                JedisPoolConfig poolConfig = new JedisPoolConfig();
                poolConfig.setMaxTotal(128);
                return new JedisPool(poolConfig, redisHost, redisPort);
        }

        @Bean
        public javax.cache.Cache<String, List<String>> ehCache() {
                // Get EhCache's JCache provider
                javax.cache.spi.CachingProvider cachingProvider = javax.cache.Caching.getCachingProvider(
                                "org.ehcache.jsr107.EhcacheCachingProvider");

                // Create cache manager
                javax.cache.CacheManager cacheManager = cachingProvider.getCacheManager();

                // Build EhCache configuration (heap-only for simplicity)
                org.ehcache.config.CacheConfiguration<String, List> ehcacheConfig = org.ehcache.config.builders.CacheConfigurationBuilder
                                .newCacheConfigurationBuilder(
                                                String.class, List.class,
                                                org.ehcache.config.builders.ResourcePoolsBuilder
                                                                .newResourcePoolsBuilder()
                                                                .heap(50, org.ehcache.config.units.MemoryUnit.MB))
                                .build();

                // Wrap EhCache config in JCache config
                javax.cache.configuration.Configuration<String, List> jcacheConfig = org.ehcache.jsr107.Eh107Configuration
                                .fromEhcacheCacheConfiguration(ehcacheConfig);

                // Create cache
                // Note: We cast to List<String> because JCache/EhCache generics are strict but
                // runtime is type-erased
                @SuppressWarnings("unchecked")
                javax.cache.Cache<String, List<String>> cache = (javax.cache.Cache<String, List<String>>) (javax.cache.Cache<?, ?>) cacheManager
                                .createCache("L2-EhCache", jcacheConfig);

                return cache;
        }

        @Bean
        public MultiLayerCache<String, List<String>> multiLayerCache(
                        CacheMetrics metrics,
                        MeterRegistry meterRegistry,
                        JedisPool jedisPool,
                        javax.cache.Cache<String, List<String>> ehCache) throws Exception {

                // Layer 0: L1 cache (in-memory, short TTL, fast)
                CacheLayer<String, List<String>> l1 = new InMemoryLayer<>("L1-Memory", Duration.ofMinutes(5));

                // Layer 1: L2 cache (EhCache with JCache wrapper)
                // Now we pass the pre-configured cache instance
                CacheLayer<String, List<String>> l2 = new JCacheEhCacheLayer<>(
                                "L2-EhCache",
                                ehCache,
                                meterRegistry);

                // Layer 2: L3 cache (Redis Network Cache)
                // Now we pass the pre-configured JedisPool
                CacheLayer<String, List<String>> l3 = new RedisCacheLayer<>(
                                "L3-Redis",
                                jedisPool,
                                (Class<List<String>>) (Class<?>) List.class,
                                Duration.ofMinutes(30),
                                meterRegistry);

                // Layer 3: File-backed loader (persistent, CSV format)
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

                List<CacheLayer<String, List<String>>> layers = Arrays.asList(l1, l2, l3);

                return new MultiLayerCache<>(layers, fileLoader, metrics);
        }

        @Bean
        CacheService cacheService(MultiLayerCache<String, List<String>> multiLayerCache) {
                return new CacheService(multiLayerCache);
        }
}
