package com.example.cache.app;

import com.example.cache.lib.MultiLayerCache;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
public class RedisIntegrationTest {

    @Container
    public static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("redis.host", redis::getHost);
        registry.add("redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private MultiLayerCache<String, List<String>> cache;

    @Autowired
    private JedisPool jedisPool;

    @Test
    public void testRedisCacheLayerIntegration() {
        String key = "test:redis:key";
        List<String> value = Arrays.asList("one", "two", "three");

        // 1. Put value in cache
        cache.put(key, value);

        // 2. Verify it exists in Redis directly
        try (Jedis jedis = jedisPool.getResource()) {
            String redisValue = jedis.get("L3-Redis:" + key); // RedisCacheLayer prefixes keys with name + ":"
            assertNotNull(redisValue, "Value should exist in Redis");
            assertTrue(redisValue.contains("one"), "Redis value should contain 'one'");
            assertTrue(redisValue.contains("two"), "Redis value should contain 'two'");
            assertTrue(redisValue.contains("three"), "Redis value should contain 'three'");
        }

        // 3. Verify retrieval from cache
        Optional<List<String>> retrieved = cache.get(key);
        assertTrue(retrieved.isPresent(), "Should retrieve value from cache");
        assertEquals(value, retrieved.get(), "Retrieved value should match original");
    }
}
