# Multi-Layer Cache Application

A sophisticated multi-tier caching solution built with Java 21, Spring Boot 3.1.6, and Micrometer metrics integration.

## Architecture

The application implements a three-layer cache hierarchy:

### Cache Layers

1. **L1 Cache (In-Memory)** - Fast, short TTL (5 minutes)
   - Ultra-fast access for frequently used data
   - Automatic expiration after 5 minutes
   - Limited to actively used items

2. **L2 Cache (In-Memory)** - Medium speed, longer TTL (1 hour)
   - Intermediate cache for less frequently accessed data
   - Longer retention period of 1 hour
   - Fallback for L1 evictions

3. **L3 Cache (File-Backed)** - Persistent storage
   - CSV file-based persistent storage
   - Automatic loading from file system
   - Survives application restarts

## Features

- ✅ **Multi-tier Caching**: Automatic cache promotion across layers
- ✅ **Single-flight Loading**: Prevents thundering herd problem
- ✅ **Metrics Integration**: Micrometer-based metrics collection
- ✅ **Health Checks**: Spring Boot Actuator endpoints
- ✅ **REST API**: Full CRUD operations via HTTP
- ✅ **Java 21 LTS**: Latest long-term support Java version
- ✅ **Comprehensive Logging**: Debug-level logging for cache operations

## Building

### Prerequisites
- Java 21 (LTS)
- Maven 3.6+

### Build Commands

```bash
# Build the entire project
./mvnw clean install

# Build specific module
./mvnw clean install -pl cache-lib
./mvnw clean install -pl cache-app

# Run tests
./mvnw test
```

## Running the Application

### Method 1: Using Spring Boot Maven Plugin
```bash
./mvnw spring-boot:run -pl cache-app
```

### Method 2: Using JAR file
```bash
java -jar cache-app/target/cache-app-0.0.1-SNAPSHOT.jar
```

The application will start on `http://localhost:8080`

## API Endpoints

### Get cached value
```bash
GET /api/cache/{key}

# Example
curl http://localhost:8080/api/cache/user:1

# Response
{
  "key": "user:1",
  "value": ["John", "Doe", "Active"],
  "success": true
}
```

### Add/Update cache entry
```bash
POST /api/cache/{key}
Content-Type: application/json

{
  "value": ["data1", "data2", "data3"]
}

# Example
curl -X POST http://localhost:8080/api/cache/custom:key \
  -H "Content-Type: application/json" \
  -d '{"value":["custom","data","test"]}'
```

### Delete cache entry
```bash
DELETE /api/cache/{key}

# Example
curl -X DELETE http://localhost:8080/api/cache/user:1
```

### Clear all cache entries
```bash
DELETE /api/cache

# Example
curl -X DELETE http://localhost:8080/api/cache
```

## Monitoring & Metrics

### Health Check
```bash
curl http://localhost:8080/actuator/health
```

### Available Metrics
```bash
curl http://localhost:8080/actuator/metrics
```

### Specific Metrics
```bash
# Cache hits
curl http://localhost:8080/actuator/metrics/cache.hit

# Cache misses
curl http://localhost:8080/actuator/metrics/cache.miss

# Cache puts
curl http://localhost:8080/actuator/metrics/cache.put

# Cache evictions
curl http://localhost:8080/actuator/metrics/cache.evict

# File read duration
curl http://localhost:8080/actuator/metrics/file.read.duration
```

### Prometheus Metrics
```bash
curl http://localhost:8080/actuator/prometheus
```

## Sample Data

The application initializes with sample CSV data in `/tmp/cache-data/data.csv`:

```csv
key,value1,value2,value3
user:1,John,Doe,Active
user:2,Jane,Smith,Active
product:1,Laptop,Electronics,Available
product:2,Phone,Electronics,Available
```

## Configuration

Edit `cache-app/src/main/resources/application.properties` to customize:

- `server.port` - Application port (default: 8080)
- Cache layer TTLs in `CacheConfiguration.java`
- Logging levels and patterns
- Actuator endpoints exposure

## Module Structure

```
java-multi-layer-cache/
├── cache-lib/                          # Core cache library
│   └── src/main/java/com/example/cache/lib/
│       ├── CacheLayer.java            # Cache layer interface
│       ├── CacheLoader.java           # Cache loader interface
│       ├── MultiLayerCache.java       # Multi-layer implementation
│       ├── config/                    # Configuration classes
│       ├── impl/                      # Layer implementations
│       ├── loader/                    # Loader implementations
│       └── metrics/                   # Metrics interface
│
└── cache-app/                          # Spring Boot application
    └── src/main/java/com/example/cache/app/
        ├── CacheApplication.java      # Spring Boot entry point
        ├── CacheConfiguration.java    # Spring configuration
        ├── CacheController.java       # REST API controller
        ├── CacheService.java          # Business logic service
        └── CacheMetricsImpl.java       # Micrometer metrics
```

## Java 21 Upgrade

This project has been successfully upgraded to Java 21 LTS using the OpenRewrite migration tool.

**Key Changes:**
- Updated `java.version` property from 17 to 21 in `pom.xml`
- Applied OpenRewrite recipes for Java 21 compatibility
- All code compiles and tests pass successfully
- No deprecated APIs used

## Testing

### Manual Testing
```bash
# Start the application
java -jar cache-app/target/cache-app-0.0.1-SNAPSHOT.jar

# In another terminal, run tests
./test.sh  # Included test script with various scenarios
```

### Example Test Scenario
1. Request a cached value (cache miss, loads from file)
2. Request the same value again (cache hit from L1/L2)
3. Wait for TTL expiration and request again
4. Add a new custom value via POST
5. Monitor metrics for cache operations

## Performance Characteristics

- **L1 Cache Hit**: < 1ms (in-memory lookup)
- **L2 Cache Hit**: < 5ms (in-memory with TTL check)
- **L3 Cache Hit**: 10-100ms (file I/O)
- **Cache Miss**: 50-200ms (file parsing and cache population)

## Technologies

- **Java**: 21 (LTS)
- **Spring Boot**: 3.1.6
- **Spring Framework**: 6.0.14
- **Micrometer**: 1.11.6 (metrics)
- **Maven**: Build automation
- **Apache Commons CSV**: CSV file parsing

## License

This project is provided as-is for educational purposes.

## Support

For issues or questions:
1. Check the application logs: `logging.level.com.example.cache=DEBUG`
2. Review the REST API endpoints documentation above
3. Verify Java 21 installation: `java -version`
4. Ensure port 8080 is available

---

**Latest Version**: 0.0.1-SNAPSHOT  
**Java Version**: 21 (LTS)  
**Spring Boot**: 3.1.6  
**Build Status**: ✅ SUCCESS
