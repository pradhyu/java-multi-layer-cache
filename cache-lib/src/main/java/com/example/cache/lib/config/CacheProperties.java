package com.example.cache.lib.config;

import java.util.ArrayList;
import java.util.List;

public class CacheProperties {
    private List<CacheConfiguration> layers = new ArrayList<>();

    public List<CacheConfiguration> getLayers() { return layers; }
    public void setLayers(List<CacheConfiguration> layers) { this.layers = layers; }

    public static class FileLoaderConfig {
        private List<String> paths = new ArrayList<>();
        private String delimiter = ",";
        private boolean header = false;
        private String charset = "UTF-8";

        public List<String> getPaths() { return paths; }
        public void setPaths(List<String> paths) { this.paths = paths; }
        public String getDelimiter() { return delimiter; }
        public void setDelimiter(String delimiter) { this.delimiter = delimiter; }
        public boolean isHeader() { return header; }
        public void setHeader(boolean header) { this.header = header; }
        public String getCharset() { return charset; }
        public void setCharset(String charset) { this.charset = charset; }
    }

    private FileLoaderConfig fileLoader = new FileLoaderConfig();

    public FileLoaderConfig getFileLoader() { return fileLoader; }
    public void setFileLoader(FileLoaderConfig fileLoader) { this.fileLoader = fileLoader; }
}
