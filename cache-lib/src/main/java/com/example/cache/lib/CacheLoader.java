package com.example.cache.lib;

import java.util.Collection;
import java.util.Map;

public interface CacheLoader<K, V> {
    V load(K key) throws Exception;
    Map<K, V> loadAll(Collection<K> keys) throws Exception;
}
