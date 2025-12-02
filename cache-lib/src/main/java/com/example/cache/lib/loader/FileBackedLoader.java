package com.example.cache.lib.loader;

import com.example.cache.lib.CacheLoader;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class FileBackedLoader implements CacheLoader<String, List<String>> {
    private final List<Path> paths;
    private final char delimiter;
    private final boolean header;
    private final Charset charset;

    public FileBackedLoader(List<Path> paths, char delimiter, boolean header, Charset charset) {
        this.paths = new ArrayList<>(paths);
        this.delimiter = delimiter;
        this.header = header;
        this.charset = charset;
    }

    @Override
    public List<String> load(String key) throws IOException {
        Map<String, List<String>> all = loadAll(Collections.singletonList(key));
        return all.getOrDefault(key, Collections.emptyList());
    }

    @Override
    public Map<String, List<String>> loadAll(Collection<String> keys) throws IOException {
        Set<String> keySet = new HashSet<>(keys);
        Map<String, List<String>> result = new HashMap<>();
        for (Path p : paths) {
            if (!Files.exists(p)) continue;
            try (Reader r = Files.newBufferedReader(p, charset)) {
                CSVFormat fmt = CSVFormat.DEFAULT.withDelimiter(delimiter);
                if (header) fmt = fmt.withFirstRecordAsHeader();
                CSVParser parser = new CSVParser(r, fmt);
                for (CSVRecord rec : parser) {
                    if (rec.size() == 0) continue;
                    String k = rec.get(0).trim();
                    if (!keySet.contains(k)) continue;
                    List<String> values = new ArrayList<>();
                    for (int i = 1; i < rec.size(); i++) {
                        String v = rec.get(i).trim();
                        if (!v.isEmpty()) values.add(v);
                    }
                    result.computeIfAbsent(k, kk -> new ArrayList<>()).addAll(values);
                }
            }
        }
        return result;
    }
}
