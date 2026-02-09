# Elasticsearch-Evolution

> A library to migrate Elasticsearch and OpenSearch mappings. Inspired by Flyway.

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://raw.githubusercontent.com/senacor/elasticsearch-evolution/master/LICENSE)
[![LICENSE](https://img.shields.io/badge/license-Anti%20996-blue.svg)](https://github.com/996icu/996.ICU/blob/master/LICENSE)
[![996.icu](https://img.shields.io/badge/link-996.icu-red.svg)](https://996.icu)
[![Maven Central](https://img.shields.io/maven-central/v/com.senacor.elasticsearch.evolution/elasticsearch-evolution-core)](https://central.sonatype.com/artifact/com.senacor.elasticsearch.evolution/elasticsearch-evolution-core/)
[![Javadocs](https://www.javadoc.io/badge/com.senacor.elasticsearch.evolution/elasticsearch-evolution-core.svg)](https://www.javadoc.io/doc/com.senacor.elasticsearch.evolution/elasticsearch-evolution-core)
[![Maven Matrix Build](https://github.com/senacor/elasticsearch-evolution/actions/workflows/maven-matrix.yml/badge.svg)](https://github.com/senacor/elasticsearch-evolution/actions/workflows/maven-matrix.yml)
[![Quality analysis](https://github.com/senacor/elasticsearch-evolution/actions/workflows/quality.yml/badge.svg)](https://github.com/senacor/elasticsearch-evolution/actions/workflows/quality.yml)
[![Coverage Status](https://coveralls.io/repos/github/senacor/elasticsearch-evolution/badge.svg?branch=master)](https://coveralls.io/github/senacor/elasticsearch-evolution?branch=master)
![Libraries.io dependency status for GitHub repo](https://img.shields.io/librariesio/github/senacor/elasticsearch-evolution)
[![OpenSSF Best Practices](https://www.bestpractices.dev/projects/11573/badge)](https://www.bestpractices.dev/projects/11573)
<!-- explicitly send a `page` query parameter to Scarf to work around GitHub pre-caching -->
<img referrerpolicy="no-referrer-when-downgrade" src="https://static.scarf.sh/a.png?x-pxid=24949dce-e8ef-4b52-82f1-1b2664b9bbc8&page=README.md" />

<img src="Elasticsearch-Evolution-logo-544x545.png" alt="Elasticsearch-Evolution logo" width="200" height="200" title="Elasticsearch-Evolution logo"/>

## 1 Evolve your Elasticsearch and OpenSearch mappings easily and reliably across all your instances

Elasticsearch-Evolution executes versioned migration scripts reliably and persists the execution state in an internal Elasticsearch/OpenSearch index.
Successfully executed migration scripts will not be executed again! 

## 2 Features

- tested on Java 17, 21 and 25
- runs on Spring-Boot 3.x and 4.x (and of course without Spring-Boot)
- runs on Elasticsearch version 8.x - 9.x
  - but focussing on maintained versions, see [elastic.co/support/eol](https://www.elastic.co/support/eol) or [endoflife.date/elasticsearch](https://endoflife.date/elasticsearch)
- runs on OpenSearch version 2.x - 3.x
  - but focussing on maintained versions, see [opensearch.org/releases](https://opensearch.org/releases/) or [endoflife.date/opensearch](https://endoflife.date/opensearch)
- highly configurable (e.g. location(s) of your migration files, migration file format patterns)
- placeholder substitution in migration scripts
- easily extendable to your needs
- supports microservices / multiple parallel running instances via logical database locks
- ready to use default configuration
- line comments in migration files
- Migrations as Java code

| Compatibility                    | Spring Boot                                      | Elasticsearch        | OpenSearch |
|----------------------------------|--------------------------------------------------|----------------------|------------|
| elasticsearch-evolution >= 0.9.0 | 3.3 - 4.0                                        | 8.x - 9.x            | 2.x - 3.x  |
| elasticsearch-evolution >= 0.8.0 | 3.2 - 4.0                                        | 7.5.x - 9.x          | 2.x - 3.x  |
| elasticsearch-evolution >= 0.7.0 | 3.2 - 3.5                                        | 7.5.x - 9.x          | 2.x - 3.x  |
| elasticsearch-evolution >= 0.6.1 | 3.0 - 3.2                                        | 7.5.x - 8.19.x       | 1.x - 2.x  |
| elasticsearch-evolution >= 0.4.2 | 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 3.0, 3.1, 3.2 | 7.5.x - 8.13.x       | 1.x - 2.x  |
| elasticsearch-evolution >= 0.4.0 | 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7                | 7.5.x - 8.6.x        | 1.x - 2.x  |
| elasticsearch-evolution 0.3.x    | 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7                | 7.5.x - 7.17.x       |            |
| elasticsearch-evolution 0.2.x    | 1.5, 2.0, 2.1, 2.2, 2.3, 2.4                     | 7.0.x - 7.4.x, 6.8.x |            |

## 3 Quickstart

### 3.1 Quickstart with Spring Boot starter

First, add the latest version of the Elasticsearch-Evolution Spring Boot starter as a dependency to your Maven pom.xml
and choose an `elasticsearch-evolution-rest-abstraction` implementation (here: Elasticsearch RestClient implementation):

```xml
<dependency>
    <groupId>com.senacor.elasticsearch.evolution</groupId>
    <artifactId>spring-boot-starter-elasticsearch-evolution</artifactId>
    <version>1.0.0</version>
</dependency>
<dependency>
    <groupId>com.senacor.elasticsearch.evolution</groupId>
    <artifactId>elasticsearch-evolution-rest-abstraction-es-client</artifactId>
    <version>1.0.0</version>
</dependency>
```

Place your migration scripts in your application classpath under `es/migration`.

That's it. Elasticsearch-Evolution runs at application startup and expects your Elasticsearch/OpenSearch instance at <http://localhost:9200>.

### 3.2 Quickstart with core library

First, add the latest version of Elasticsearch-Evolution core as a dependency
and choose an `elasticsearch-evolution-rest-abstraction` implementation (here: Elasticsearch RestClient implementation):

```xml
<dependency>
    <groupId>com.senacor.elasticsearch.evolution</groupId>
    <artifactId>elasticsearch-evolution-core</artifactId>
    <version>1.0.0</version>
</dependency>
<dependency>
    <groupId>com.senacor.elasticsearch.evolution</groupId>
    <artifactId>elasticsearch-evolution-rest-abstraction-es-client</artifactId>
    <version>1.0.0</version>
</dependency>
```

Place your migration scripts in your application classpath under `es/migration`.

Create an `ElasticsearchEvolution` instance and execute the migration:

```java
// First, create an Elasticsearch RestClient
RestClient restClient = RestClient.builder(HttpHost.create("http://localhost:9200")).build();
// Then, create an EvolutionRestClient abstraction for Elasticsearch
EvolutionRestClient evolutionRestClient = new EvolutionESRestClient(restClient);
// Then, create an ElasticsearchEvolution configuration and instance
ElasticsearchEvolution elasticsearchEvolution = ElasticsearchEvolution.configure()
        .load(evolutionRestClient);
// Execute the migration
elasticsearchEvolution.migrate();
```

#### 3.2.1 Validation without execution

If you just want to validate your migration scripts without executing them, you can use the `validate()` method:

```java
ElasticsearchEvolution elasticsearchEvolution = ...;
// Just validate the migrations
elasticsearchEvolution.validate();
```

This will validate applied migrations against resolved ones (on the filesystem or classpath) to detect accidental changes that may prevent the schema(s) from being recreated exactly.

Validation fails if:

* a previously applied migration has been modified after it was applied (if enabled in configuration, see `validateOnMigrate`)
* versions have been resolved that haven't been applied yet

When validation fails, a `ValidateException` is thrown.

### 3.3 REST Client abstraction

Elasticsearch-Evolution uses a REST client abstraction (`EvolutionRestClient`). Currently, these implementations exist:

* Elasticsearch [RestClient](https://central.sonatype.com/artifact/org.elasticsearch.client/elasticsearch-rest-client) implementation: `EvolutionESRestClient`
  ```xml
  <dependency>
    <groupId>com.senacor.elasticsearch.evolution</groupId>
    <artifactId>elasticsearch-evolution-rest-abstraction-es-client</artifactId>
    <version>1.0.0</version>
  </dependency>
  ```
* Elasticsearch [Rest5Client](https://central.sonatype.com/artifact/co.elastic.clients/elasticsearch-rest5-client) implementation: `EvolutionESRest5Client` which is designed for Elasticsearch `9.x` and Spring Boot `4.x`.
  ```xml
  <dependency>
    <groupId>com.senacor.elasticsearch.evolution</groupId>
    <artifactId>elasticsearch-evolution-rest-abstraction-es-rest5client</artifactId>
    <version>1.0.0</version>
  </dependency>
  ```  
* OpenSearch [RestClient](https://central.sonatype.com/artifact/org.opensearch.client/opensearch-rest-client) implementation: `EvolutionOpenSearchRestClient` which is designed for OpenSearch `2.x` because the `RestClientTransport` is deprecated for removal since `3.x`.
  ```xml
  <dependency>
    <groupId>com.senacor.elasticsearch.evolution</groupId>
    <artifactId>elasticsearch-evolution-rest-abstraction-os-restclient</artifactId>
    <version>1.0.0</version>
  </dependency>
  ```
* OpenSearch [OpenSearchGenericClient](https://central.sonatype.com/artifact/org.opensearch.client/opensearch-java) implementation: `EvolutionOpenSearchGenericClient` which is designed for OpenSearch `3.x` and later.
  ```xml
  <dependency>
    <groupId>com.senacor.elasticsearch.evolution</groupId>
    <artifactId>elasticsearch-evolution-rest-abstraction-os-genericclient</artifactId>
    <version>1.0.0</version>
  </dependency>
  ```  

You can provide your own implementation of `EvolutionRestClient` if you want to use another HTTP client like _Apache HTTPClient_ or _OkHttpClient_. This interface is located in:
```xml
<dependency>
  <groupId>com.senacor.elasticsearch.evolution</groupId>
  <artifactId>elasticsearch-evolution-rest-abstraction</artifactId>
  <version>1.0.0</version>
</dependency>
```
- With the Spring Boot starter, you have to create a bean with your custom `EvolutionRestClient` implementation like this:
  ```java
  @Bean
  public EvolutionRestClient customEvolutionRestClient() {
      return new MyOkHttpClientEvolutionRestClient(...);
  }
  ```

## 4 Migration script format

### 4.1 Migration script file content

An Elasticsearch-Evolution migration script represents a REST call. Here is an example:

```http request
PUT /_template/my_template
Content-Type: application/json

{
  "index_patterns": [
    "my_index_*"
  ],
  "order": 1,
  "version": 1,
  "settings": {
    "number_of_shards": 1
  },
  "mappings": {
    "properties": {
    "version": {
      "type": "keyword",
      "ignore_above": 20,
      "similarity": "boolean"
    },
    "locked": {
      "type": "boolean"
    }
    }
  }
}
```

The first line defines the HTTP method `PUT` and the relative path to the Elasticsearch/OpenSearch endpoint `/_template/my_template` to create a new mapping template.
This is followed by an HTTP header `Content-Type: application/json`.
After a blank line, the HTTP body is defined.

The pattern is strongly oriented towards ordinary HTTP requests and consists of 4 parts:

1.  **The HTTP method (required)**. Supported HTTP methods are `GET`, `HEAD`, `POST`, `PUT`, `DELETE`, `OPTIONS` and `PATCH`. 
    The first non-comment line must always start with an HTTP method.
2.  **The path to the Elasticsearch/OpenSearch endpoint to call (required)**. The path is separated from the HTTP method by a _blank_ space. 
    You can provide any query parameters like in an ordinary browser, such as `/my_index_1/_doc/1?refresh=true&op_type=create`.
3.  **HTTP header(s) (optional)**. All non-comment lines after the _HTTP method_ line will be interpreted as HTTP headers. Header name and content are separated by `:`.
4.  **HTTP body (optional)**. The HTTP body is separated by a blank line and can contain any content you want to send to Elasticsearch/OpenSearch.

#### 4.1.1 Comments

Elasticsearch-Evolution supports line comments in its migration scripts. Every line starting with `#` or `//` will be interpreted as a comment line.
Comment lines are not sent to Elasticsearch/OpenSearch; they will be filtered by Elasticsearch-Evolution.

#### 4.1.2 Placeholders

Elasticsearch-Evolution supports named placeholder substitution. Placeholders are marked in your migration script like this: `${my-placeholder}`

-   Starts with `placeholderPrefix`, which is by default `${` and is configurable.
-   Followed by the `placeholder name`, which can be any string but must not contain `placeholderPrefix` or `placeholderSuffix`.
-   Ends with `placeholderSuffix`, which is by default `}` and is configurable.
     

### 4.2 Migration script file name

Here is an example filename: `V1.0__my-description.http`

The filename must follow a pattern:

-   Starts with `esMigrationPrefix`, which is by default `V` and is configurable.
-   Followed by a version, which must be numeric and can be structured by separating the version parts with `.`.
-   Followed by the `versionDescriptionSeparator`: `__`.
-   Followed by a description, which can be any text your filesystem supports.
-   Ends with `esMigrationSuffixes`, which is by default `.http` and is configurable and case-insensitive.

Elasticsearch-Evolution uses the version for ordering your scripts and enforces strictly ordered execution of your scripts by default. Out-of-order execution is supported but disabled by default.
Elasticsearch-Evolution interprets the version parts as integers, so each version part must be between 1 (inclusive) and 2,147,483,647 (inclusive).

Here is an example that indicates the ordering: `1.0.1` < `1.1` < `1.2.1` < (`2.0.0` == `2`).
In this example, version `1.0.1` is the smallest version and is executed first, followed by versions `1.1`, `1.2.1`, and finally `2`. 
`2` is the same as `2.0` or `2.0.0` - trailing zeros will be trimmed.

**NOTE:** Versions with major version `0` are reserved for internal usage, so the smallest version you can define is `1`.

### 4.3 Java Migrations

Elasticsearch-Evolution also supports Java Migrations. A Java Migration is a Java class that implements the `JavaMigration` interface. 

* The filename must follow the same pattern as described in the previous section, but the suffix check is ignored. 
  * For the version dots `.` can be replaced with underscores `_` in the class name, so `V1_2__my_description.java` is a valid filename for a Java Migration with version `1.2`.
  * You can provide a version and description directly in your `JavaMigration`, just overwrite the default implementation of the `getMetadata()` method. In this case, the filename is not relevant for the version and description.
* The Java class must be located in the same location as your other migration scripts (e.g., `classpath:es/migration`). 
  * Only `classpath` locations are supported for Java Migrations, `file` locations are not supported.
* The `JavaMigration` must have a public no-args constructor, so that Elasticsearch-Evolution can create an instance of it via reflection.

Here is an example of a Java Migration:
```java
public class V1_2__AddDocument implements JavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        String body = """
                {
                  "doc": {
                    "version": "2",
                    "success": true,
                    "a": "a a a"
                  }
                }""";
        final EvolutionRestResponse res = context.getEvolutionRestClient().execute(HttpMethod.PUT,
                "/test_1/_doc/2?refresh",
                Map.of("Content-Type", "application/json"),
                null,
                body);
        if (res.statusCode() != 201) {
            throw new IllegalArgumentException("Failed to add document. " + res.asString());
        }
    }
}
```

## 5 Configuration options

Elasticsearch-Evolution can be configured to your needs:

-   **enabled** (default=`true`): Whether to enable or disable Elasticsearch-Evolution.
-   **locations** (default=`[classpath:es/migration]`): List of locations of migration scripts. Supported are `classpath:some/path` and `file:/some/path`. The location is scanned recursively, but only to a depth of 10. **NOTE**: All scripts in all locations/subdirectories will be flattened, and only the version number will be used to order them.
-   **encoding** (default=`UTF-8`): Encoding of migration files.
-   **defaultContentType** (default=`application/json; charset=UTF-8`): This content type will be used as the default if no `Content-Type` header is specified in the header section of a migration script. If no charset is defined, the `encoding` charset is used.
-   **esMigrationPrefix** (default=`V`): File name prefix for migration files.
-   **esMigrationSuffixes** (default=`[.http]`): List of file name suffixes for migration files. The suffix is checked case-insensitively. 
-   **placeholderReplacement** (default=`true`): Whether to enable or disable placeholder replacement in migration scripts.
-   **placeholders** (default=`[]`): Map of placeholders and their replacements to apply to migration scripts.
-   **placeholderPrefix** (default=`${`): Prefix of placeholders in migration scripts.
-   **placeholderSuffix** (default=`}`): Suffix of placeholders in migration scripts.
-   **historyIndex** (default=`es_evolution`): Name of the history index that will be used by Elasticsearch-Evolution. In this index, Elasticsearch-Evolution will persist its internal state and track which migration scripts have already been executed.
-   **historyMaxQuerySize** (default=`1000`): The maximum query size while validating already executed scripts. This query size must be higher than the total count of your migration scripts.
-   **validateOnMigrate** (default=`true`): Whether to fail when a previously applied migration script has been modified after it was applied.
-   **baselineVersion** (default=`1.0`): Version to use as a baseline. Versions lower than this will not be applied.
-   **lineSeparator** (default=`\n`): Line separator, used only temporarily between reading raw migration file line-by-line and parsing it later. Only needed for backward compatibility/checksum stability! Should be one of `\n`, `\r` or `\r\n`.
-   **outOfOrder** (default=`false`): Allows migrations to be run "out of order". If you already have versions 1.0 and 3.0 applied, and now version 2.0 is found, it will be applied too instead of being rejected.
- **trimTrailingNewlineInMigrations** (default=`false`): Whether to remove a trailing newline in migration scripts. Only needed for backward compatibility/checksum stability!
- **javaMigrations** (default=`[]`): These are not Java-based migrations discovered through classpath scanning and instantiated by Elasticsearch-Evolution. Instead, these are manually added instances of `JavaMigration` This is particularly useful when working with a dependency injection container, where you may want the DI container to instantiate the class and wire up its dependencies for you.
- **javaMigrationClassProvider** (default=`null`): A custom ClassProvider to be used to look up `JavaMigration` classes. If not set, the default strategy will be used which is described in the Java Migrations section.

### 5.1 Spring Boot

You can set the above configurations via Spring Boot's default configuration mechanism. Just use the prefix `spring.elasticsearch.evolution`. Here is an example `application.properties`:

```properties
spring.elasticsearch.evolution.locations[0]=classpath:es/migration
spring.elasticsearch.evolution.locations[1]=classpath:es/more_migration_scripts
spring.elasticsearch.evolution.placeholderReplacement=true
spring.elasticsearch.evolution.placeholders.indexname=myIndexReplacement
spring.elasticsearch.evolution.placeholders.docType=_doc
spring.elasticsearch.evolution.placeholders.foo=bar
spring.elasticsearch.evolution.historyIndex=es_evolution
```

#### 5.1.1 Elasticsearch AutoConfiguration (since Spring Boot 2.1)

Since Spring Boot 2.1, AutoConfiguration for Elasticsearch's REST client is provided (see `org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration` or `org.springframework.boot.elasticsearch.autoconfigure.ElasticsearchRestClientAutoConfiguration` for Spring Boot 4).
You can configure the client, required for Elasticsearch-Evolution, just like this in your `application.properties`:

```properties
spring.elasticsearch.uris[0]=https://example.com:9200
spring.elasticsearch.username=my-user-name
spring.elasticsearch.password=my-secret-pw
```

#### 5.1.2 OpenSearch AutoConfiguration

Elasticsearch-Evolution tries to be compatible with [spring-data-opensearch-starter](https://github.com/opensearch-project/spring-data-opensearch) and its AutoConfiguration.

AutoConfiguration for OpenSearch's REST client is provided (see `org.opensearch.spring.boot.autoconfigure.OpenSearchRestClientAutoConfiguration`).

AutoConfiguration for OpenSearch's java client is provided (see `org.opensearch.spring.boot.autoconfigure.OpenSearchClientAutoConfiguration`).

You can configure the client, required for Elasticsearch-Evolution, just like this in your `application.properties`:

```properties
opensearch.uris[0]=https://example.com:9200
opensearch.username=my-user-name
opensearch.password=my-secret-pw
```

#### 5.1.3 Customize Elasticsearch-Evolutions AutoConfiguration

##### 5.1.3.1 Custom Elasticsearch RestClient

Elasticsearch-Evolution just needs an `EvolutionRestClient` as a Spring bean. The Elasticsearch `EvolutionESRestClient` implementation needs a `RestClient` Spring bean.
If you don't have Spring Boot 2.1 or later, or you need a special `RestClient` configuration (e.g., to accept self-signed certificates or disable hostname validation), you can provide a custom `RestClient` like this:

```java
@Bean
public RestClient myRestClient() {
    RestClientBuilder builder = RestClient.builder(HttpHost.create("https://localhost:9200"))
            .setHttpClientConfigCallback(httpClientBuilder -> {
                        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials("my-user-name", "my-secret-pw"));
                        httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                        try {
                            httpClientBuilder
                                    .setSSLContext(new SSLContextBuilder().loadTrustMaterial(null, TrustAllStrategy.INSTANCE).build())
                                    .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE);
                        } catch (GeneralSecurityException e) {
                            throw new IllegalStateException("could not configure http client to accept all certificates", e);
                        }
                        return httpClientBuilder;
                    }
            );
    return builder.build();
}
```

##### 5.1.3.2 Custom OpenSearch RestClient

Elasticsearch-Evolution just needs an `EvolutionRestClient` as a Spring bean. The OpenSearch `EvolutionOpenSearchRestClient` implementation needs a `RestClient` Spring bean.
If you don't use already the [spring-data-opensearch-starter](https://central.sonatype.com/artifact/org.opensearch.client/spring-data-opensearch-starter) or you need a special `RestClient` configuration (e.g., to accept self-signed certificates or disable hostname validation), you can provide a custom `RestClient` similar to the Elasticsearch example above.

##### 5.1.3.3 Custom OpenSearchGenericClient

Elasticsearch-Evolution just needs an `EvolutionRestClient` as a Spring bean. The OpenSearch `EvolutionOpenSearchGenericClient` implementation needs a `OpenSearchGenericClient` or `OpenSearchClient` Spring bean.
If you don't use already the [spring-data-opensearch-starter](https://central.sonatype.com/artifact/org.opensearch.client/spring-data-opensearch-starter) or you need a special `OpenSearch*Client` configuration (e.g., to accept self-signed certificates or disable hostname validation), you can provide a custom `OpenSearchGenericClient` or `OpenSearchClient` as Spring bean like this:

```java
@Bean
public OpenSearchClient myOpenSearchClient() throws URISyntaxException {
    final HttpHost httpHost = HttpHost.create("https://localhost:9200");
    OpenSearchTransport openSearchTransport = ApacheHttpClient5TransportBuilder.builder(httpHost)
            // I want to enable compression for better performance
            .setCompressionEnabled(true)
            .build();
    return new OpenSearchClient(openSearchTransport);
}
```

Providing a `OpenSearchTransport` Spring bean is also possible, the `ElasticsearchEvolutionAutoConfiguration` will create the `EvolutionOpenSearchGenericClient` from it:
```java
@Bean
public OpenSearchTransport myOpenSearchTransport() throws URISyntaxException {
    final HttpHost httpHost = HttpHost.create("https://localhost:9200");
    return ApacheHttpClient5TransportBuilder.builder(httpHost)
            // I want to enable compression for better performance
            .setCompressionEnabled(true)
            .build();
}
```

##### 5.1.3.4 Custom ElasticsearchEvolutionInitializer

If you want to provide a customized initializer for Elasticsearch-Evolution (e.g., with a different order):

```java
@Bean
public ElasticsearchEvolutionInitializer customElasticsearchEvolutionInitializer(ElasticsearchEvolution elasticsearchEvolution) {
    return new ElasticsearchEvolutionInitializer(elasticsearchEvolution) {
        @Override
        public int getOrder() {
            return Ordered.LOWEST_PRECEDENCE;
        }
    };
}
```

##### 5.1.3.5 Customize Elasticsearch-Evolution configuration

If you want to customize the Elasticsearch-Evolution configuration after the configuration properties have been applied, you can provide a Spring Bean implementing `ElasticsearchEvolutionConfigCustomizer` like this:

```java
@Bean
ElasticsearchEvolutionConfigCustomizer javaMigrationBeansCustomizer(ObjectProvider<JavaMigration> javaMigrations) {
    // provide additional Java Migrations to Elasticsearch-Evolution managed as Spring Beans
    return config -> config.setJavaMigrations(javaMigrations.stream().toList());
}
```

### 5.2 Core library

You can set the above configurations via the `ElasticsearchEvolutionConfig` fluent builder like this:

```java
ElasticsearchEvolution.configure()
    .setLocations(Collections.singletonList("classpath:es/migration"))
    .setPlaceholderReplacement(true)
    .setPlaceholders(Collections.singletonMap("indexname", "myIndexReplacement"))
    .setHistoryIndex("es_evolution");
```

## 6 Changelog

### v1.0.1-SNAPSHOT

- ...

### v1.0.0

- Support Java Migrations ([#577](https://github.com/senacor/elasticsearch-evolution/issues/577)).

### v0.9.0

- Drop Elasticsearch 7.x and OpenSearch 1.x support ([#570](https://github.com/senacor/elasticsearch-evolution/issues/570)).
  - Focusing on maintained versions.
- Drop deprecated RestHighLevelClient in tests ([#549](https://github.com/senacor/elasticsearch-evolution/issues/549)).
- Drop Spring Boot 3.2 compatibility tests. Further versions may run on Spring Boot 3.2, but they are not tested anymore.
- Improve release process ([#562](https://github.com/senacor/elasticsearch-evolution/issues/562)).
  - Trigger a release from within the GitHub Actions Website ([via workflow_dispatch Event](https://docs.github.com/en/actions/how-tos/manage-workflow-runs/manually-run-a-workflow)).
  - Automatically set the release version and the next development version after the release finished.

### v0.8.0

- fixed `EvolutionOpenSearchRestClient` (`elasticsearch-evolution-rest-abstraction-os-restclient`) compatibility with OpenSearch 2.x client libs ([#565](https://github.com/senacor/elasticsearch-evolution/issues/565)),
- added Spring Boot 4 compatibility ([#564](https://github.com/senacor/elasticsearch-evolution/issues/564)).
  - Added Elasticsearch `EvolutionRestClient` implementation: `EvolutionESRest5Client`. It uses the [Apache HttpClient 5](https://hc.apache.org/) based `Rest5Client` from `co.elastic.clients:elasticsearch-rest5-client`.

### v0.7.2

- Added 2 OpenSearch `EvolutionRestClient` implementations ([#198](https://github.com/senacor/elasticsearch-evolution/issues/198), [#220](https://github.com/senacor/elasticsearch-evolution/issues/220), [#287](https://github.com/senacor/elasticsearch-evolution/issues/287), [#348](https://github.com/senacor/elasticsearch-evolution/issues/348)).
    - `EvolutionOpenSearchRestClient` for OpenSearch `RestClient`.
    - `EvolutionOpenSearchGenericClient` for `OpenSearchGenericClient` and `OpenSearchClient`.

### v0.7.1

- Release process: Replace archived `actions/create-release` with `softprops/action-gh-release` ([#554](https://github.com/senacor/elasticsearch-evolution/issues/554)).
- Release process: Switch to new maven central publishing API / central-publishing-maven-plugin ([#538](https://github.com/senacor/elasticsearch-evolution/issues/538)).
- Added unique version number validation ([#551](https://github.com/senacor/elasticsearch-evolution/issues/551)).
  - When two migration scripts have the same version number, the migration fails with a `MigrationException`.

### v0.7.0

- **Breaking change**: Added abstraction for the underlying HTTP Client (`EvolutionRestClient` in new artifact `com.senacor.elasticsearch.evolution:elasticsearch-evolution-rest-abstraction`) and an implementation for the Elasticsearch `RestClient` (`EvolutionESRestClient` in artifact `com.senacor.elasticsearch.evolution:elasticsearch-evolution-rest-abstraction-es-client`) ([#553](https://github.com/senacor/elasticsearch-evolution/pull/553)).
  - You have to add the dependency `com.senacor.elasticsearch.evolution:elasticsearch-evolution-rest-abstraction-es-client`.
- Drop Spring Boot 3.0 and 3.1 compatibility tests. Further versions may run on Spring Boot 3.0 and 3.1, but they are not tested anymore.
- Added regression tests for Spring Boot 3.4 and 3.5.
- Added regression tests against Elasticsearch 9.0 - 9.2.
- Added regression tests against OpenSearch 3.3 and 3.4.
- Added option to just validate without executing migrations ([#435](https://github.com/senacor/elasticsearch-evolution/issues/435)).

### v0.6.1

- Added regression tests against OpenSearch 2.19.
- Bump Spring Boot version to 3.5.
- Added regression tests for Spring Boot 3.3.
- Bugfix ([#536](https://github.com/senacor/elasticsearch-evolution/issues/536)): Don't do HTTP GET request with a body.
- Added regression tests on JDK 25.
- Added regression tests against Elasticsearch 8.14 - 8.19.

### v0.6.0

- Added option to trim a trailing newline in migration scripts (fixes [#298](https://github.com/senacor/elasticsearch-evolution/issues/298)). NOTE: This option is only needed for backward compatibility/checksum stability!
- The minimum supported Java version is now 17.
- Drop Spring Boot 2 compatibility. Further versions may run on Spring Boot 2, but they are not tested anymore.

### v0.5.2

- Bugfix ([#293](https://github.com/senacor/elasticsearch-evolution/issues/293)): Trailing newlines will no longer be removed from migration scripts.
- Added regression tests against OpenSearch 2.13.
- Added regression tests against Elasticsearch 8.13.

### v0.5.1

- Version updates (Spring Boot 2.7.18).
- Added regression tests for Spring Boot 3.2.
- Remove deprecated query parameter `ignore_throttled` from ES requests.

### v0.5.0

- Added Spring Boot configuration metadata [#240](https://github.com/senacor/elasticsearch-evolution/pull/240).
- Replaces unmaintained [org.reflections](https://github.com/ronmamo/reflections) library with [classgraph](https://github.com/classgraph/classgraph) to scan the classpath for migration files. Fixes [#239](https://github.com/senacor/elasticsearch-evolution/issues/239). 

### v0.4.3

- Support out-of-order migration execution.
- Version updates (Spring Boot 2.7.17).
- Added regression tests against OpenSearch 2.11, 2.10 and 2.9.
- Added regression tests against Elasticsearch 8.11, 8.10 and 8.9.
- Drop older Elasticsearch and OpenSearch versions in regression tests. Only test against the last 3 minor versions of the latest major release.
- Added regression tests on JDK 21.
- Added regression tests for Spring Boot 3.1.
- Update org.reflections:reflections from 0.9.12 to 0.10.2 [#233](https://github.com/senacor/elasticsearch-evolution/pull/233) thanks @RiVogel.
- **KNOWN ISSUES**:
  - [#239](https://github.com/senacor/elasticsearch-evolution/issues/239): Migration files not found in Spring Boot jar.
    - Workaround 1: Downgrade `org.reflections:reflections` to `0.10.1`.
    - Workaround 2: Downgrade `elasticsearch-evolution` to `0.4.2`.

### v0.4.2

- Bugfix ([#182](https://github.com/senacor/elasticsearch-evolution/issues/182)): Checksum calculation was based on system-dependent line separators which led to different checksums on different operating systems (e.g., Windows vs. Linux). The default is now `\n`. For backward compatibility, you can set a different line separator via the `lineSeparator` config property.
- Version updates (Spring Boot 2.7.8).
- Spring Boot 3 compatibility + tests.
- Added OpenSearch 2.5 compatibility tests.

### v0.4.1

- Optimization: Don't acquire lock if no scripts need to be executed ([#172](https://github.com/senacor/elasticsearch-evolution/issues/172)).
- Previously applied migration scripts are now checked for modifications and rejected if they've been modified after they were applied. The old behaviour can be restored by setting the new configuration parameter `validateOnMigrate` to false (default: true) ([#155](https://github.com/senacor/elasticsearch-evolution/issues/155)).
- Version updates (Spring Boot 2.7.7).
- Added Java 19 compatibility tests.
- Added Spring Boot 2.7 compatibility tests.
- Added Elasticsearch 8.6, 8.5, 8.4, 8.3, and 8.2 compatibility tests.
- Added OpenSearch 2.4, 2.3, 2.2, 2.1 and 2.0 compatibility tests.
- It is now possible to set a `baselineVersion` to skip migrations with versions lower than the defined `baselineVersion` ([#164](https://github.com/senacor/elasticsearch-evolution/issues/164)).

### v0.4.0

- **Breaking change**: Drop `org.elasticsearch.client.RestHighLevelClient` and replace with `org.elasticsearch.client.RestClient` (LowLevelClient). This will drop the big transitive dependency `org.elasticsearch:elasticsearch` and opens compatibility to Elasticsearch 8 and OpenSearch.
- Version updates (Spring Boot 2.6.6).
- Added Spring Boot 2.5 and 2.6 compatibility tests.
- Added Java 17 and 18 compatibility tests.
- Added Elasticsearch 8.1, 8.0, 7.17, 7.16, 7.15, 7.14 and 7.13 compatibility tests.
- Added OpenSearch 1.0, 1.1, 1.2 and 1.3 compatibility tests.
- Fixed issue [#114](https://github.com/senacor/elasticsearch-evolution/issues/114).

### v0.3.2

-   Fixed issue [#36](https://github.com/senacor/elasticsearch-evolution/issues/36).
-   Version updates (Spring Boot 2.4.5).
-   Added Java 16 compatibility tests.
-   Added Elasticsearch 7.12 compatibility tests.

### v0.3.1

-   Fixed issue [#29](https://github.com/senacor/elasticsearch-evolution/issues/29).
-   Fixed issue [#27](https://github.com/senacor/elasticsearch-evolution/issues/27).
-   Version updates (Spring Boot 2.4.3).

### v0.3.0

-   Version upgrade elasticsearch-rest-high-level-client to 7.5.2 (ES versions < 7.5.0 are no longer supported).
-   Remove Spring Boot 1.5 and 2.0 support.
-   Version updates (Spring Boot 2.4.0).
-   Added Spring Boot 2.4 compatibility tests.

### v0.2.1

-   Version updates (Spring Boot 2.3.5.RELEASE).

### v0.2.0

-   Version updates (Spring Boot 2.3.0.RELEASE, Elasticsearch 6.8.6, Jackson 2.10.3, SLF4J 1.7.30, Reflections 0.9.12).
-   Added Spring Boot 2.2 compatibility tests.
-   Added Spring Boot 2.3 compatibility tests.

### v0.1.3

-   New configuration parameter `historyMaxQuerySize`.
-   Version updates (Spring Boot 2.1.9.RELEASE; Elasticsearch 6.8.3).
-   ElasticsearchEvolutionInitializer now logs the migration duration.

### v0.1.2

-   Version updates (Spring Boot 2.1.3.RELEASE; Elasticsearch 6.7.0).
-   Bugfix: Support more than 10 migration scripts: now 1000.

### v0.1.1

-   Improved logging.
-   Fixed classpath scanning in fat-jars like Spring Boot.

### v0.1.0

-   Initial version.

## 7 Contributing

We welcome contributions to Elasticsearch-Evolution! Here's how you can help: [CONTRIBUTING.md](CONTRIBUTING.md)

## 8 Code of Conduct

See [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md)

## 9 Stats

### 9.1 Star History

[![Star History Chart](https://api.star-history.com/svg?repos=senacor/elasticsearch-evolution&type=date&legend=top-left)](https://www.star-history.com/#senacor/elasticsearch-evolution&type=date&legend=top-left)
