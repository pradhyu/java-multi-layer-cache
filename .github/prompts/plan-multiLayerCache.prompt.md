Plan: Multi-layer cache project tasks

TL;DR: Scaffold a Maven multi-module project with `cache-lib` (multi-layer Ehcache + CSV loader + metrics) and `cache-app` (Spring Boot demo exposing Prometheus/Actuator). Implement interfaces, Ehcache and file-backed layers, metrics via Micrometer, and comprehensive unit/integration tests to validate hits, puts, TTLs, and file loads.

Steps
1. Scaffold parent `pom.xml` and modules `cache-lib` and `cache-app`.
2. Implement core interfaces: `CacheLayer<K,V>` and `CacheLoader<K,V>` in `cache-lib`.
3. Add implementations: `EhcacheLayer`, `InMemoryLayer`, `MultiLayerCache`, `CacheLayerFactory`.
4. Implement `FileBackedLoader` returning `Map<String,List<String>>` from CSV paths.
5. Add `CacheMetrics` using `MeterRegistry` (counters/gauges/timers per `layer`/`file`).
6. Create `cache-app` Spring Boot demo, `application.yml`, Actuator Prometheus endpoint, and sample CSVs.
7. Write tests: `FileBackedLoaderTest`, `EhcacheLayerTest`, `MultiLayerCacheIntegrationTest`, and Prometheus metrics integration tests.

Further Considerations
1. Confirm `groupId`/base package (default: `com.example.cache`) and Maven choice.
2. Decide single-flight behavior for concurrent misses (recommended: dedupe loads).
3. Confirm CSV schema (key column index, header-present) and large-file strategy (in-memory vs streaming).

Deliverables
- Parent pom and two module poms
- `cache-lib` implementation: interfaces, Ehcache integration, multi-layer chaining logic, metrics
- `cache-app` Spring Boot demo: configuration, auto-configuration, controller(s), sample CSVs
- Unit and integration tests verifying behavior and metrics
- README with run/test instructions

Next steps
- Start scaffolding the project files and implement library classes and tests. (Ask for permission before committing/creating repo files.)

Notes
- Use Spring Boot starter parent (3.1.x+), Ehcache 3.x, Micrometer + Prometheus registry.
- Metrics: counters for hits/misses/puts/evicts per layer; gauge for cache size; counters/timers for file reads and load durations.
- CSV loader returns Map<String, List<String>>; key = first column, remaining columns appended as values across rows.
