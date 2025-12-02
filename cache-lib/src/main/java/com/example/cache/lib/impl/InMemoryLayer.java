package com.example.cache.lib.impl;

import com.example.cache.lib.CacheLayer;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryLayer<K, V> implements CacheLayer<K, V> {
    private final String name;
    private final Duration ttl;
    private final ConcurrentHashMap<K, Entry<V>> map = new ConcurrentHashMap<>();

    public InMemoryLayer(String name, Duration ttl) {
        this.name = name;
        this.ttl = ttl == null ? Duration.ofSeconds(0) : ttl;
    }

    @Override
    public Optional<V> get(K key) {
        Entry<V> e = map.get(key);
        if (e == null) return Optional.empty();
        if (e.expiry != 0 && Instant.now().toEpochMilli() > e.expiry) {
            map.remove(key);
            return Optional.empty();
        }
        return Optional.ofNullable(e.value);
    }

    @Override
    public void put(K key, V value) {
        long expiry = 0;
        if (!ttl.isZero() && !ttl.isNegative()) {
            expiry = Instant.now().plusMillis(ttl.toMillis()).toEpochMilli();
        }
        map.put(key, new Entry<>(value, expiry));
    }

    @Override
    public void evict(K key) { map.remove(key); }

    @Override
    public void clear() { map.clear(); }

    @Override
    public long size() { return map.size(); }

    @Override
    public String name() { return name; }

    private static class Entry<V> {
        final V value;
        final long expiry; // epoch millis, 0 means no expiry
        Entry(V value, long expiry) { this.value = value; this.expiry = expiry; }
    }
}
