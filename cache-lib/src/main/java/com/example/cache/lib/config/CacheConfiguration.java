package com.example.cache.lib.config;

import java.time.Duration;

public class CacheConfiguration {
    private String name;
    private String type; // EH_CACHE | IN_MEMORY
    private Duration ttl;
    private Integer maxEntries;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Duration getTtl() { return ttl; }
    public void setTtl(Duration ttl) { this.ttl = ttl; }
    public Integer getMaxEntries() { return maxEntries; }
    public void setMaxEntries(Integer maxEntries) { this.maxEntries = maxEntries; }
}
