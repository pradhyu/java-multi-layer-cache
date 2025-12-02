package com.example.cache.app;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CacheControllerTest {

    static class StubService extends CacheService {
        StubService() { super(null); }
        @Override public Optional<List<String>> get(String key) { return Optional.of(List.of("v")); }
        @Override public void put(String key, List<String> value) { /* noop */ }
        @Override public void evict(String key) { /* noop */ }
        @Override public void clear() { /* noop */ }
    }

    @Test
    void controllerGetPutEvictClear() {
        CacheController controller = new CacheController(new StubService());

        var resp = controller.get("k");
        assertTrue(resp.success());
        assertEquals("k", resp.key());

        var putReq = new CacheController.CachePutRequest(List.of("a"));
        var putResp = controller.put("k", putReq);
        assertTrue(putResp.success());

        var delResp = controller.evict("k");
        assertTrue(delResp.success());

        var clearResp = controller.clear();
        assertNotNull(clearResp.message());
    }
}
