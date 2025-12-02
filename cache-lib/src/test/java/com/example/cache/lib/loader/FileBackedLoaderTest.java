package com.example.cache.lib.loader;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FileBackedLoaderTest {

    @Test
    void loadAndLoadAll() throws IOException {
        Path tmp = Files.createTempFile("test-data", ".csv");
        try {
            String csv = "key1,val1,val2\nkey2,val3\n";
            Files.write(tmp, csv.getBytes(StandardCharsets.UTF_8));

            FileBackedLoader loader = new FileBackedLoader(List.of(tmp), ',', false, StandardCharsets.UTF_8);

            List<String> key1 = loader.load("key1");
            assertEquals(2, key1.size());
            assertTrue(key1.contains("val1"));

            Map<String, List<String>> all = loader.loadAll(List.of("key1", "key2"));
            assertEquals(2, all.size());
            assertEquals(1, all.get("key2").size());
        } finally {
            Files.deleteIfExists(tmp);
        }
    }
}
