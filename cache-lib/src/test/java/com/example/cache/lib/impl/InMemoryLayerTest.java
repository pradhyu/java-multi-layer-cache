package com.example.cache.lib.impl;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryLayerTest {

    @Test
    void putGetEvictAndClear() {
        InMemoryLayer<String, String> layer = new InMemoryLayer<>("mem", Duration.ofSeconds(60));
        assertEquals(0, layer.size());

        layer.put("k", "v");
        assertEquals(1, layer.size());

        Optional<String> v = layer.get("k");
        assertTrue(v.isPresent());
        assertEquals("v", v.get());

        layer.evict("k");
        assertFalse(layer.get("k").isPresent());
        assertEquals(0, layer.size());

        layer.put("a", "1");
        layer.put("b", "2");
        layer.clear();
        assertEquals(0, layer.size());
    }

    @Test
    void ttlExpiryWorks() throws InterruptedException {
        InMemoryLayer<String, String> layer = new InMemoryLayer<>("mem-t", Duration.ofMillis(50));
        layer.put("x", "y");
        assertTrue(layer.get("x").isPresent());
        Thread.sleep(120);
        assertFalse(layer.get("x").isPresent());
    }
}
