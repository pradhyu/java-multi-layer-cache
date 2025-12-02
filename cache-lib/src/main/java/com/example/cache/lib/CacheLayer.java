package com.example.cache.lib;

import java.util.Optional;

public interface CacheLayer<K, V> {
    Optional<V> get(K key);
    void put(K key, V value);
    void evict(K key);
    void clear();
    long size();
    String name();
}
