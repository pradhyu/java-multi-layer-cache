package com.example.cache.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Spring Boot application entry point for the multi-layer cache application.
 * Demonstrates a multi-tier caching strategy with in-memory and file-backed layers.
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.example.cache"})
public class CacheApplication {

    public static void main(String[] args) {
        SpringApplication.run(CacheApplication.class, args);
    }
}
