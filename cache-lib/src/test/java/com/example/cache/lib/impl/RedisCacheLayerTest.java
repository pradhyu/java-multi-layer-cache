package com.example.cache.lib.impl;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisCacheLayerTest {

    @Mock
    private JedisPool jedisPool;

    @Mock
    private Jedis jedis;

    private RedisCacheLayer<String, List> redisLayer;

    @BeforeEach
    void setUp() {
        lenient().when(jedisPool.getResource()).thenReturn(jedis);

        redisLayer = new RedisCacheLayer<>(
                "L3-Redis",
                jedisPool,
                List.class,
                Duration.ofMinutes(10),
                new SimpleMeterRegistry());
    }

    @Test
    void testPutAndGet() {
        String key = "testKey";
        String jsonValue = "[\"value1\",\"value2\"]";
        List<String> value = List.of("value1", "value2");

        // Mock get behavior
        when(jedis.get(key)).thenReturn(jsonValue);

        // Test Put
        redisLayer.put(key, value);
        verify(jedis).setex(eq(key), anyLong(), contains("value1"));

        // Test Get
        Optional<List> result = redisLayer.get(key);

        assertTrue(result.isPresent());
        assertEquals(value, result.get());
    }

    @Test
    void testMiss() {
        when(jedis.get(anyString())).thenReturn(null);

        Optional<List> result = redisLayer.get("nonExistentKey");
        assertTrue(result.isEmpty());
    }

    @Test
    void testEvict() {
        String key = "evictKey";
        redisLayer.evict(key);
        verify(jedis).del(key);
    }
}
