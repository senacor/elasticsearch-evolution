# Elasticsearch-Evolution – Agent Guide

## Project Overview
Flyway-inspired library for migrating Elasticsearch/OpenSearch index mappings. Executes versioned HTTP migration scripts and persists their execution state in an internal index (`es_evolution`).
Supports Java 17/21/25, Spring Boot 3.x/4.x, Elasticsearch 8.x–9.x, and OpenSearch 2.x–3.x.

## Module Structure
```
elasticsearch-evolution-rest-abstraction          # EvolutionRestClient interface (public API)
elasticsearch-evolution-rest-abstraction-es-client     # Impl for Elasticsearch RestClient (Elasticsearch 8.x, Spring-Boot 3.x)
elasticsearch-evolution-rest-abstraction-es-rest5client # Impl for Elasticsearch Rest5Client (Elasticsearch 9.x, Spring-Boot 4.x)
elasticsearch-evolution-rest-abstraction-os-restclient  # Impl for OpenSearch RestClient (OpenSearch 2.x only)
elasticsearch-evolution-rest-abstraction-os-genericclient # Impl for OpenSearch GenericClient (OpenSearch 3.x+)
elasticsearch-evolution-core                      # Migration engine (parse → execute → history)
spring-boot-starter-elasticsearch-evolution      # Spring Boot auto-configuration
tests-elasticsearch/                             # Elasticsearch Integration test modules per Spring Boot version
tests-opensearch-restclient/                     # Opensearch Integration test modules per Spring Boot version with elasticsearch-evolution-rest-abstraction-os-restclient 
tests-opensearch-genericclient/                  # Opensearch Integration test modules per Spring Boot version with elasticsearch-evolution-rest-abstraction-os-genericclient 
```

**Rule:** `api` packages are public API; `internal` packages are implementation details – never depend on `internal` from outside the module.

## Build & Test Commands
```bash
# Standard build (unit tests + integration tests via Testcontainers)
./mvnw install

# CI-style (settings for snapshot repo)
./mvnw --settings .cicd.settings.xml -e -B -V install

# Run Elasticsearch integration test suite against a specific Elasticsearch version
./mvnw --file ./tests-elasticsearch/pom.xml clean verify -Delasticsearch.version=9.3.2

# Run OpenSearch (RestClient) integration tests
./mvnw --file ./tests-opensearch-restclient/pom.xml clean verify -Dopensearch.version=3.5.0

# Run OpenSearch (GenericClient) integration tests
./mvnw --file ./tests-opensearch-genericclient/pom.xml clean verify -Dopensearch.version=3.5.0

# Skip tests (e.g., before running external tests separately)
./mvnw install -DskipTests
```

The `tests-*` modules are only included by the `external-test` Maven profile (active by default, disabled during `release`). Each sub-module targets a specific Spring Boot version (e.g., `test-es-spring-boot-3.5`).

## Test Conventions
- **Unit tests** – `*Test.java`, run by `maven-surefire-plugin`
- **Integration tests** – `*IT.java`, run by `maven-failsafe-plugin` with Testcontainers (Docker required)
- `EmbeddedElasticsearchExtension` (`core/src/test/…`) is a JUnit 5 extension that spins up parameterised containers for multiple Elasticsearch/OpenSearch versions in parallel
- `@SpringBootTest` integration tests start a container via `@TestConfiguration`, bind a fixed port (e.g., `18773:9200`), and disable xpack security: `xpack.security.enabled=false`
- `failIfNoTests=true` is configured per module to catch missing tests early

## Migration Script Format
**Filename:** `V{version}__{description}.http` — e.g., `V1.00__createTemplate.http`  
Version parts are integers separated by `.`; trailing zeros are trimmed (`2` == `2.0.0`). Major version `0` is reserved internally.

**File content** (plain HTTP request):
```http
PUT /${index}/_doc/1?refresh
Content-Type: application/json

{ "field": "value" }
```
- Line 1: `HTTP_METHOD /path` (required)
- Lines 2+: headers `Name: Value` (optional)
- Blank line → body starts (optional)
- Comment lines: `#` or `//`
- Placeholders: `${name}` — configured via `spring.elasticsearch.evolution.placeholders.name=value`

Default script location: `classpath:es/migration`. Example in `tests-elasticsearch/migration-scripts/src/main/resources/es/mig/`.

## Java Migrations
Implement `JavaMigration`, name class `V1_2__MyDesc` (dots → underscores), place on classpath in the migration location:
```java
public class V1_2__AddDocument implements JavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        EvolutionRestResponse res = context.getEvolutionRestClient()
                .execute(HttpMethod.PUT, "/index/_doc/1?refresh",
                         Map.of("Content-Type", "application/json"), null, body);
        if (res.statusCode() != 201) throw new IllegalArgumentException(res.asString());
    }
}
```
Must have a public no-args constructor.

## REST Client Selection (critical)
The auto-configuration in `ElasticsearchEvolutionAutoConfiguration` picks the `EvolutionRestClient` based on classpath presence and property guards:

| Scenario | Dependency to add | Config property to disable conflict |
|---|---|---|
| Elasticsearch 8.x / Spring-Boot 3.x | `elasticsearch-evolution-rest-abstraction-es-client` | `spring.elasticsearch.evolution.es.rest5client.enabled=false` |
| Elasticsearch 9.x / Spring-Boot 4.x | `elasticsearch-evolution-rest-abstraction-es-rest5client` | `spring.elasticsearch.evolution.es.restclient.enabled=false` |
| OpenSearch 2.x | `elasticsearch-evolution-rest-abstraction-os-restclient` | — |
| OpenSearch 3.x+ | `elasticsearch-evolution-rest-abstraction-os-genericclient` | — |

OpenSearch URI is configured under `opensearch.uris` (not `spring.elasticsearch.uris`).

## Key Configuration Properties (`spring.elasticsearch.evolution.*`)
| Property | Default | Notes |
|---|---|---|
| `locations` | `[classpath:es/migration]` | supports `classpath:` and `file:` |
| `historyIndex` | `es_evolution` | index where execution state is stored |
| `historyMaxQuerySize` | `1000` | must exceed total migration count |
| `validateOnMigrate` | `true` | fails if applied script was changed |
| `outOfOrder` | `false` | allows running scripts with lower versions |
| `esMigrationPrefix` | `V` | filename prefix |
| `esMigrationSuffixes` | `[.http]` | case-insensitive |
| `baselineVersion` | `1.0` | versions below are ignored |

## Lombok & Compiler Note
JDK 25 requires `<maven.compiler.proc>full</maven.compiler.proc>` (set in parent POM) to work with Lombok.  
All model classes in `internal.model.*` use Lombok `@Value` / `@Builder`.

## Version Management Scripts
```bash
MVN_CMD=./mvnw ./setVersion.sh 1.2.0          # set a specific version across all modules
MVN_CMD=./mvnw ./setNextVersion.sh 1.2.1-SNAPSHOT
```
These scripts loop over all `tests-*` subdirectories and update their POMs independently (they have their own parent chain using `spring-boot-starter-parent`).