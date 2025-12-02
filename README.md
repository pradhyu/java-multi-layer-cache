Multi-layer cache project

This repository contains two Maven modules:

- `cache-lib`: Library implementing a configurable multi-layer cache (Ehcache layers + file-backed loader) and Micrometer metrics.
- `cache-app`: Spring Boot demo application that wires the library, exposes REST endpoints, and provides a Prometheus metrics endpoint via Actuator.

Quick start

To build:

```bash
mvn clean package
```

To run the demo app:

```bash
mvn -pl cache-app spring-boot:run
```

Configuration

Edit `cache-app/src/main/resources/application.yml` to configure cache layers and file loader paths.

Next steps

I will add library source code, Spring Boot wiring, sample CSVs, and tests.
# -java-multi-layer-cache
