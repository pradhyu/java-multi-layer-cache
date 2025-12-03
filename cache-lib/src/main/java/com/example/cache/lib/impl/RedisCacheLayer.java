package com.example.cache.lib.impl;

import com.example.cache.lib.CacheLayer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.time.Duration;
import java.util.Optional;

/**
 * Redis-based cache layer implementation.
 * Uses Jedis for connection and Jackson for serialization.
 */
public class RedisCacheLayer<K, V> implements CacheLayer<K, V> {
    private final String name;
    private final JedisPool jedisPool;
    private final ObjectMapper objectMapper;
    private final Class<V> valueType;
    private final int ttlSeconds;
    private final MeterRegistry meterRegistry;

    public RedisCacheLayer(String name, JedisPool jedisPool, Class<V> valueType, Duration ttl,
            MeterRegistry meterRegistry) {
        this.name = name;
        this.jedisPool = jedisPool;
        this.valueType = valueType;
        this.ttlSeconds = (int) ttl.toSeconds();
        this.meterRegistry = meterRegistry;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public Optional<V> get(K key) {
        String keyStr = key.toString();
        try (Jedis jedis = jedisPool.getResource()) {
            String valueStr = jedis.get(keyStr);

            if (valueStr != null) {
                recordMetric("hit");
                try {
                    return Optional.ofNullable(objectMapper.readValue(valueStr, valueType));
                } catch (JsonProcessingException e) {
                    // Log error or handle it
                    e.printStackTrace();
                    return Optional.empty();
                }
            } else {
                recordMetric("miss");
                return Optional.empty();
            }
        } catch (Exception e) {
            // Fail safe - treat connection errors as misses
            e.printStackTrace();
            return Optional.empty();
        }
    }

    @Override
    public void put(K key, V value) {
        String keyStr = key.toString();
        try (Jedis jedis = jedisPool.getResource()) {
            String valueStr = objectMapper.writeValueAsString(value);
            jedis.setex(keyStr, ttlSeconds, valueStr);
            recordMetric("put");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void evict(K key) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(key.toString());
            recordMetric("evict");
        }
    }

    @Override
    public void clear() {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.flushDB();
        } catch (Exception e) {
            // Fail safe
            e.printStackTrace();
        }
    }

    @Override
    public long size() {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.dbSize();
        } catch (Exception e) {
            // Fail safe
            e.printStackTrace();
            return 0;
        }
    }

    @Override
    public String name() {
        return name;
    }

    public void close() {
        jedisPool.close();
    }

    private void recordMetric(String result) {
        if (meterRegistry != null) {
            meterRegistry.counter("cache.redis.ops",
                    "layer", name,
                    "result", result)
                    .increment();
        }
    }
}
