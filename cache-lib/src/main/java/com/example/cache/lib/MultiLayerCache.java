package com.example.cache.lib;

import com.example.cache.lib.metrics.CacheMetrics;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

public class MultiLayerCache<K, V> {
    private final List<CacheLayer<K, V>> layers;
    private final CacheLoader<K, V> loader;
    private final CacheMetrics metrics;
    private final ConcurrentHashMap<K, CompletableFuture<V>> inFlight = new ConcurrentHashMap<>();

    public MultiLayerCache(List<CacheLayer<K, V>> layers, CacheLoader<K, V> loader, CacheMetrics metrics) {
        this.layers = new ArrayList<>(layers);
        this.loader = loader;
        this.metrics = metrics;
    }

    public Optional<V> get(K key) {
        // try layers in order
        for (int i = 0; i < layers.size(); i++) {
            CacheLayer<K, V> layer = layers.get(i);
            Optional<V> v = layer.get(key);
            if (v.isPresent()) {
                metrics.recordHit(layer.name());
                // populate higher-priority layers (0..i-1)
                for (int j = 0; j < i; j++) {
                    layers.get(j).put(key, v.get());
                    metrics.recordPut(layers.get(j).name());
                }
                return v;
            } else {
                metrics.recordMiss(layer.name());
            }
        }

        // not found in caches -> single-flight load
        try {
            V loaded = loadSingleFlight(key);
            if (loaded != null) {
                for (CacheLayer<K, V> layer : layers) {
                    layer.put(key, loaded);
                    metrics.recordPut(layer.name());
                }
                return Optional.of(loaded);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return Optional.empty();
    }

    private V loadSingleFlight(K key) throws ExecutionException, InterruptedException {
        CompletableFuture<V> f = inFlight.computeIfAbsent(key, k -> CompletableFuture.supplyAsync(() -> {
            try {
                long start = System.nanoTime();
                V loaded = loader.load(k);
                metrics.recordFileReadDuration(k == null ? "unknown" : k.toString(), System.nanoTime() - start);
                metrics.recordFileRead(k == null ? "unknown" : k.toString());
                return loaded;
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }));

        try {
            return f.get();
        } finally {
            inFlight.remove(key);
        }
    }

    public void put(K key, V value) {
        for (CacheLayer<K, V> layer : layers) {
            layer.put(key, value);
            metrics.recordPut(layer.name());
        }
    }

    public void evict(K key) {
        for (CacheLayer<K, V> layer : layers) {
            layer.evict(key);
            metrics.recordEvict(layer.name());
        }
    }

    public void clear() {
        for (CacheLayer<K, V> layer : layers) layer.clear();
    }
}
