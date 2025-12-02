package com.example.cache.app;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * REST Controller for multi-layer cache operations.
 * Exposes cache operations through HTTP endpoints.
 */
@RestController
@RequestMapping("/api/cache")
public class CacheController {
    private final CacheService cacheService;

    public CacheController(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    /**
     * Get a value from the cache.
     * @param key the cache key
     * @return the cached value, or 404 if not found
     */
    @GetMapping("/{key}")
    public CacheValueResponse get(@PathVariable String key) {
        Optional<List<String>> value = cacheService.get(key);
        return value
            .map(v -> new CacheValueResponse(key, v, true))
            .orElseGet(() -> new CacheValueResponse(key, null, false));
    }

    /**
     * Put a value into the cache.
     * @param key the cache key
     * @param request the cache value
     */
    @PostMapping("/{key}")
    public CacheValueResponse put(@PathVariable String key, @RequestBody CachePutRequest request) {
        cacheService.put(key, request.value());
        return new CacheValueResponse(key, request.value(), true);
    }

    /**
     * Evict (remove) a value from the cache.
     * @param key the cache key
     */
    @DeleteMapping("/{key}")
    public CacheValueResponse evict(@PathVariable String key) {
        cacheService.evict(key);
        return new CacheValueResponse(key, null, true);
    }

    /**
     * Clear all cache entries.
     */
    @DeleteMapping
    public CacheClearResponse clear() {
        cacheService.clear();
        return new CacheClearResponse("Cache cleared successfully");
    }

    // Response DTOs
    public record CacheValueResponse(String key, List<String> value, boolean success) {}
    public record CachePutRequest(List<String> value) {}
    public record CacheClearResponse(String message) {}
}
