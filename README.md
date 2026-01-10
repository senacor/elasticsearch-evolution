# Elasticsearch-Evolution

> A library to migrate Elasticsearch and Opensearch mappings. Inspired by flyway.

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

## 1 Evolve your Elasticsearch and Opensearch mapping easily and reliable across all your instances

Elasticsearch-Evolution executes versioned migration scripts reliable and persists the execution state in an internal Elasticsearch / Opensearch index.
Successful executed migration scripts will not be executed again! 

## 2 Features

- tested on Java 17, 21 and 25
- runs on Spring-Boot 3.x (and of course without Spring-Boot)
- runs on Elasticsearch version 7.5.x - 9.x
  - but focussing on maintained versions, see [elastic.co/support/eol](https://www.elastic.co/support/eol) or [endoflife.date/elasticsearch](https://endoflife.date/elasticsearch)
- runs on Opensearch version 1.x - 3.x
  - but focussing on maintained versions, see [opensearch.org/releases](https://opensearch.org/releases/) or [endoflife.date/opensearch](https://endoflife.date/opensearch)
- highly configurable (e.g. location(s) of your migration files, migration files format pattern)
- placeholder substitution in migration scripts
- easily extendable to your needs
- supports microservices / multiple parallel running instances via logical database locks
- ready to use default configuration
- line comments in migration files

| Compatibility                    | Spring Boot                                      | Elasticsearch        | Opensearch |
|----------------------------------|--------------------------------------------------|----------------------|------------|
| elasticsearch-evolution >= 0.7.0 | 3.2 - 3.5                                        | 7.5.x - 9.x          | 1.x - 3.x  |
| elasticsearch-evolution >= 0.6.1 | 3.0 - 3.2                                        | 7.5.x - 8.19.x       | 1.x - 2.x  |
| elasticsearch-evolution >= 0.4.2 | 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 3.0, 3.1, 3.2 | 7.5.x - 8.13.x       | 1.x - 2.x  |
| elasticsearch-evolution >= 0.4.0 | 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7                | 7.5.x - 8.6.x        | 1.x - 2.x  |
| elasticsearch-evolution 0.3.x    | 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7                | 7.5.x - 7.17.x       |            |
| elasticsearch-evolution 0.2.x    | 1.5, 2.0, 2.1, 2.2, 2.3, 2.4                     | 7.0.x - 7.4.x, 6.8.x |            |

## 3 Quickstart

### 3.1 Quickstart with Spring-Boot starter

First add the latest version of Elasticsearch-Evolution spring boot starter as a dependency in your maven pom.xml:

```xml
<dependency>
    <groupId>com.senacor.elasticsearch.evolution</groupId>
    <artifactId>spring-boot-starter-elasticsearch-evolution</artifactId>
    <version>0.7.0</version>
</dependency>
<dependency>
    <groupId>com.senacor.elasticsearch.evolution</groupId>
    <artifactId>elasticsearch-evolution-rest-abstraction-es-client</artifactId>
    <version>0.7.0</version>
</dependency>
```

Place your migration scripts in your application classpath at `es/migration`

That's it. Elasticsearch-Evolution runs at application startup and expects your Elasticsearch/Opensearch at <http://localhost:9200>

### 3.2 Quickstart with core library

First add the latest version of Elasticsearch-Evolution core as a dependency:

```xml
<dependency>
    <groupId>com.senacor.elasticsearch.evolution</groupId>
    <artifactId>elasticsearch-evolution-core</artifactId>
    <version>0.7.0</version>
</dependency>
<dependency>
  <groupId>com.senacor.elasticsearch.evolution</groupId>
  <artifactId>elasticsearch-evolution-rest-abstraction-es-client</artifactId>
  <version>0.7.0</version>
</dependency>
```

Place your migration scripts in your application classpath at `es/migration`

Create a `ElasticsearchEvolution` instance and execute the migration.

```java
// first create an Elastic RestClient
RestClient restClient = RestClient.builder(HttpHost.create("http://localhost:9200")).build();
// then create a EvolutionRestClient abstraction for Elasticsearch
EvolutionRestClient evolutionRestClient = new EvolutionESRestClient(restClient);
// then create a ElasticsearchEvolution configuration and create an instance of ElasticsearchEvolution with that configuration
ElasticsearchEvolution elasticsearchEvolution = ElasticsearchEvolution.configure()
        .load(evolutionRestClient);
// execute the migration
elasticsearchEvolution.migrate();
```

#### 3.2.1 Validation without execution

If you just want to validate your migration scripts without executing them, you can use the `validate()` method:

```java
ElasticsearchEvolution elasticsearchEvolution = ...;
// just validate the migrations
elasticsearchEvolution.validate();
```

This will validate applied migrations against resolved ones (on the filesystem or classpath) to detect accidental
changes that may prevent the schema(s) from being recreated exactly.

Validation fails if:

* a previously applied migration has been modified after it was applied (if enabled in configuration, see
  `validateOnMigrate`)
* versions have been resolved that haven't been applied yet

Then a `ValidateException` is thrown.

### 3.3 REST Client abstraction

Elasticsearch-Evolution uses an REST client abstraction (`EvolutionRestClient`). Currently, these implementations exist:

* Elasticsearch [RestClient](https://central.sonatype.com/artifact/org.elasticsearch.client/elasticsearch-rest-client) implementation: `EvolutionESRestClient`
  ```xml
  <dependency>
    <groupId>com.senacor.elasticsearch.evolution</groupId>
    <artifactId>elasticsearch-evolution-rest-abstraction-es-client</artifactId>
    <version>0.7.0</version>
  </dependency>
  ```
* OpenSearch implementation is coming soon

You can provide your own implementation of `EvolutionRestClient` if you want to use another HTTP client like _Apache HTTPClient_ or _OkHttpClient_. This interface is located in:
```xml
<dependency>
  <groupId>com.senacor.elasticsearch.evolution</groupId>
  <artifactId>elasticsearch-evolution-rest-abstraction</artifactId>
  <version>0.7.0</version>
</dependency>
```
- with the Spring-Boot starter you have to create a bean with your custom `EvolutionRestClient` implementation like this:
  ```java
  @Bean
  public EvolutionRestClient customEvolutionRestClient() {
      return new MyOkHttpClientEvolutionRestClient(...);
  }
  ```

## 4 Migration script format

### 4.1 Migration script file content

A Elasticsearch-Evolutions migration script represents just a rest call. Here is an Example:

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

The first line defines the HTTP method `PUT` and the relative path to the Elasticsearch/Opensearch endpoint `/_template/my_template` to create a new mapping template.
Followed by a HTTP Header `Content-Type: application/json`.
After a blank line the HTTP body is defined.

The pattern is strongly oriented in ordinary HTTP requests and consist of 4 parts:

1.  **The HTTP method (required)**. Supported HTTP methods are `GET`, `HEAD`, `POST`, `PUT`, `DELETE`, `OPTIONS` and `PATCH`. 
    The First non-comment line must always start with a HTTP method.
2.  **The path to the Elasticsearch/Opensearch endpoint to call (required)**. The path is separated by a _blank_ from the HTTP method. 
    You can provide any query parameters like in a ordinary browser like this `/my_index_1/_doc/1?refresh=true&op_type=create`
3.  **HTTP Header(s) (optional)**. All non-comment lines after the _HTTP method_ line will be interpreted as HTTP headers. Header name and content are separated by `:`.
4.  **HTTP Body (optional)**. The HTTP Body is separated by a blank line and can contain any content you want to sent to Elasticsearch/Opensearch.

#### 4.1.1 Comments

Elasticsearch-Evolution supports line-comments in its migration scripts. Every line starting with `#` or `//` will be interpreted as a comment-line.
Comment-lines are not send to Elasticsearch/Opensearch, they will be filtered by Elasticsearch-Evolution.

#### 4.1.2 Placeholders

Elasticsearch-Evolution supports named placeholder substitution. Placeholders are marked in your migration script like this: `${my-placeholder}`

-   starts with `placeholderPrefix` which is by default `${` and is configurable.
-   followed by the `placeholder name` which can be any string, but must not contain `placeholderPrefix` or `placeholderSuffix`  
-   ends with `placeholderSuffix` which is by default `}` and is configurable.
     

### 4.2 Migration script file name

Here is an example filename: `V1.0__my-description.http`

The filename has to follow a pattern:

-   starts with `esMigrationPrefix` which is by default `V` and is configurable.
-   followed by a version, which have to be numeric and can be structured by separating the version parts with `.`
-   followed by the `versionDescriptionSeparator`: `__`
-   followed by a description which can be any text your filesystem supports
-   ended with `esMigrationSuffixes` which is by default `.http` and is configurable and case-insensitive.

Elasticsearch-Evolution uses the version for ordering your scripts and enforces strict ordered execution of your scripts, by default. Out-of-Order execution is supported, but disabled by default.
Elasticsearch-Evolution interprets the version parts as Integers, so each version part must be between 1 (inclusive) and 2,147,483,647 (inclusive).

Here is an example which indicates the ordering: `1.0.1` &lt; `1.1` &lt; `1.2.1` &lt; (`2.0.0` == `2`).
In this example version `1.0.1` is the smallest version and is executed first, after that version `1.1`, `1.2.1` and in the end `2`. 
`2` is the same as `2.0` or `2.0.0` - so trailing zeros will be trimmed.

**NOTE:** Versions with major version `0` are reserved for internal usage, so the smallest version you can define is `1`

## 5 Configuration options

Elasticsearch-Evolution can be configured to your needs:

-   **enabled** (default=true): Whether to enable or disable Elasticsearch-Evolution.
-   **locations** (default=\[classpath:es/migration]): List of locations of migrations scripts. Supported is classpath:some/path and file:/some/path. The location is scanned recursive, but only to a depth of 10. **NOTE**: all scripts in all locations / subdirectories will be flatted and only the version number will be used to order them.
-   **encoding** (default=UTF-8): Encoding of migration files.
-   **defaultContentType** (default=application/json; charset=UTF-8): This content type will be used as default if no contentType header is specified in the header section of a migration script. If no charset is defined, the `encoding` charset is used.
-   **esMigrationPrefix** (default=V): File name prefix for migration files.
-   **esMigrationSuffixes** (default=\[.http]): List of file name suffixes for migration files. The suffix is checked case-insensitive. 
-   **placeholderReplacement** (default=true): Whether to enable or disable placeholder replacement in migration scripts.
-   **placeholders** (default=\[]): Map of placeholders and their replacements to apply to migration scripts.
-   **placeholderPrefix** (default=${): Prefix of placeholders in migration scripts.
-   **placeholderSuffix** (default=}): Suffix of placeholders in migration scripts.
-   **historyIndex** (default=es_evolution): Name of the history index that will be used by Elasticsearch-Evolution. In this index Elasticsearch-Evolution will persist his internal state and tracks which migration script has already been executed.
-   **historyMaxQuerySize** (default=1000): The maximum query size while validating already executed scripts. This query size have to be higher than the total count of your migration scripts.
-   **validateOnMigrate** (default=true): Whether to fail when a previously applied migration script has been modified after it was applied.
-   **baselineVersion** (default=1.0): Version to use as a baseline. versions lower than it will not be applied.
-   **lineSeparator** (default=\n): Line separator, used only temporary between reading raw migration file line-by-line and parsing it later. Only needed for backward compatibility / checksum stability! Should be one of `\n`, `\r` or `\r\n`
-   **outOfOrder** (default=false): Allows migrations to be run "out of order". If you already have versions 1.0 and 3.0 applied, and now a version 2.0 is found, it will be applied too instead of being rejected.
- **trimTrailingNewlineInMigrations** (default=false): Whether to remove a trailing newline in migration scripts. Only
  needed for backward compatibility / checksum stability!

### 5.1 Spring Boot

You can set the above configurations via Spring Boots default configuration way. Just use the prefix `spring.elasticsearch.evolution`. Here is an example `application.properties`:

```properties
spring.elasticsearch.evolution.locations[0]=classpath:es/migration
spring.elasticsearch.evolution.locations[1]=classpath:es/more_migration_scripts
spring.elasticsearch.evolution.placeholderReplacement=true
spring.elasticsearch.evolution.placeholders.indexname=myIndexReplacement
spring.elasticsearch.evolution.placeholders.docType=_doc
spring.elasticsearch.evolution.placeholders.foo=bar
spring.elasticsearch.evolution.historyIndex=es_evolution
```

#### 5.1.1 Elasticsearch AutoConfiguration (since spring boot 2.1)

Since spring boot 2.1 AutoConfiguration for Elasticsearchs REST client is provided (see org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration).
You can configure the `RestClient`, required for Elasticsearch-Evolution, just like that in your `application.properties`:

```properties
spring.elasticsearch.uris[0]=https://example.com:9200
spring.elasticsearch.username=my-user-name
spring.elasticsearch.password=my-secret-pw
```

#### 5.1.2 Customize Elasticsearch-Evolutions AutoConfiguration

##### 5.1.2.1 Custom Elasticsearch RestClient

Elasticsearch-Evolutions just needs a `EvolutionRestClient` as spring bean. The Elasticsearch `EvolutionESRestClient` impl needs a `RestClient` spring bean.
If you don't have spring boot 2.1 or later or you need a special `RestClient` configuration e.g. to accept self-signed certificates or disable hostname validation, you can provide a custom `RestClient` like this:

```java
@Bean
public RestClient restClient() {
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

##### 5.1.2.2 Custom ElasticsearchEvolutionInitializer

Maybe you want to provide a customised Initializer for Elasticsearch-Evolution e.g with another order:

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

### 5.2 core library

You can set the above configurations via the `ElasticsearchEvolutionConfig` fluent builder like this:

```java
ElasticsearchEvolution.configure()
    .setLocations(Collections.singletonList("classpath:es/migration"))
    .setPlaceholderReplacement(true)
    .setPlaceholders(Collections.singletonMap("indexname", "myIndexReplacement"))
    .setHistoryIndex("es_evolution");
```

## 6 changelog

### v0.7.0-SNAPSHOT

- **breaking change**: Added abstraction for the underlying HTTP Client (`EvolutionRestClient` in new artifact `com.senacor.elasticsearch.evolution:elasticsearch-evolution-rest-abstraction`) and an implementation for the Elasticsearch `RestClient` (`EvolutionESRestClient` in artifact `com.senacor.elasticsearch.evolution:elasticsearch-evolution-rest-abstraction-es-client`).
  - You have to add the dependency `com.senacor.elasticsearch.evolution:elasticsearch-evolution-rest-abstraction-es-client`
- Drop spring boot 3.0 and 3.1 compatibility tests. Further versions may run on spring boot 3.0 and 3.1, but it is not tested anymore.
- Added regression tests for spring boot 3.4 and 3.5.
- Added regression tests against ElasticSearch 9.0 - 9.2
- Added regression tests against OpenSearch 3.3 and 3.4
- Added option to just validate without executing migrations ([#435](https://github.com/senacor/elasticsearch-evolution/issues/435)).

### v0.6.1

- added regression tests against OpenSearch 2.19
- bump spring boot version to 3.5
- added regression tests for spring boot 3.3
- bugfix ([#536](https://github.com/senacor/elasticsearch-evolution/issues/536)): don't do HTTP GET request with a body
- added regression tests on JDK 25
- added regression tests against ElasticSearch 8.14 - 8.19

### v0.6.0

- Added option to trim a trailing newline in migration scripts (fixes [#298](https://github.com/senacor/elasticsearch-evolution/issues/298)). NOTE: This option is only needed for backward compatibility / checksum stability!
- The minimum supported Java version is now 17.
- Drop spring boot 2 compatibility. Further versions may run on spring boot 2, but it is not tested anymore.

### v0.5.2

- bugfix ([#293](https://github.com/senacor/elasticsearch-evolution/issues/293)): trailing newlines will no longer be removed from migration scripts.
- added regression tests against OpenSearch 2.13
- added regression tests against ElasticSearch 8.13

### v0.5.1

- version updates (spring-boot 2.7.18)
- added regression tests for spring boot 3.2
- remove deprecated query parameter `ignore_throttled` from ES requests

### v0.5.0

- added spring boot configuration metadata [#240](https://github.com/senacor/elasticsearch-evolution/pull/240)
- replaces unmaintained [org.reflections](https://github.com/ronmamo/reflections) library with [classgraph](https://github.com/classgraph/classgraph) to scan the classpath for migration files. Fixes [#239](https://github.com/senacor/elasticsearch-evolution/issues/239) 

### v0.4.3

- support out of order migration execution.
- version updates (spring-boot 2.7.17)
- added regression tests against OpenSearch 2.11, 2.10 and 2.9
- added regression tests against ElasticSearch 8.11. 8.10 and 8.9
- drop older Elasticsearch and OpenSearch versions in regression tests. Only test against the last 3 minor versions of the latest major release.
- added regression tests on JDK 21
- added regression tests for spring boot 3.1
- update org.reflections:reflections from 0.9.12 to 0.10.2 [#233](https://github.com/senacor/elasticsearch-evolution/pull/233) thanks @RiVogel
- **KNOWN ISSUES**:
  - [#239](https://github.com/senacor/elasticsearch-evolution/issues/239): Migration files not found in Spring Boot jar
    - Workaround 1: downgrade `org.reflections:reflections` to `0.10.1`
    - Workaround 2: downgrade `elasticsearch-evolution` to `0.4.2`

### v0.4.2

- bugfix ([#182](https://github.com/senacor/elasticsearch-evolution/issues/182)): checksum calculation was based on system dependent line separators which lead to different checksums on different operating systems (e.g. windows vs linux). The default is now `\n`. For backward compatibility you can set other line separator via `lineSeparator` config property.
- version updates (spring-boot 2.7.8)
- spring boot 3 compatibility + tests
- added Opensearch 2.5 compatibility tests

### v0.4.1

- Optimization: Don't acquire lock if no scripts need to be executed ([#172](https://github.com/senacor/elasticsearch-evolution/issues/172))
- Previously applied migration scripts are now checked for modifications and rejected if they've been modified after they were applied. The old behaviour can be restored by setting the new configuration parameter `validateOnMigrate` to false (default: true) ([#155](https://github.com/senacor/elasticsearch-evolution/issues/155))
- version updates (spring-boot 2.7.7)
- added java 19 compatibility tests
- added spring boot 2.7 compatibility tests
- added Elasticsearch 8.6, 8.5, 8.4, 8.3, and 8,2 compatibility test
- added Opensearch 2.4, 2.3, 2.2, 2.1 and 2.0 compatibility tests
- It is now possible to set a `baselineVersion` to skip migrations with versions lower than the defined `baselineVersion` ([#164](https://github.com/senacor/elasticsearch-evolution/issues/164))

### v0.4.0

- **breaking change**: drop `org.elasticsearch.client.RestHighLevelClient` and replace with `org.elasticsearch.client.RestClient` (LowLevelClient). This will drop the big transitive dependency `org.elasticsearch:elasticsearch` and opens compatibility to Elasticsearch 8 and OpenSearch.
- version updates (spring-boot 2.6.6)
- added spring boot 2.5 and 2.6 compatibility tests
- added java 17 and 18 compatibility tests
- added Elasticsearch 8.1, 8.0, 7.17, 7.16, 7.15, 7.14 and 7.13 compatibility tests
- added Opensearch 1.0, 1.1, 1.2 and 1.3 compatibility tests
- fixed issue [#114](https://github.com/senacor/elasticsearch-evolution/issues/114)

### v0.3.2

-   fixed issue [#36](https://github.com/senacor/elasticsearch-evolution/issues/36)
-   version updates (spring-boot 2.4.5)    
-   added java 16 compatibility tests
-   added Elasticsearch 7.12 compatibility tests

### v0.3.1

-   fixed issue [#29](https://github.com/senacor/elasticsearch-evolution/issues/29)
-   fixed issue [#27](https://github.com/senacor/elasticsearch-evolution/issues/27)
-   version updates (spring-boot 2.4.3)

### v0.3.0

-   version upgrade elasticsearch-rest-high-level-client to 7.5.2 (Es version < 7.5.0 are no longer supported)
-   remove Spring-Boot 1.5 and 2.0 support
-   version updates (spring-boot 2.4.0)
-   added spring-boot 2.4 compatibility tests

### v0.2.1

-   version updates (spring-boot 2.3.5.RELEASE)

### v0.2.0

-   version updates (spring-boot 2.3.0.RELEASE, elasticsearch 6.8.6, jackson 2.10.3, slf4j 1.7.30, reflections 0.9.12)
-   added spring-boot 2.2 compatibility tests
-   added spring-boot 2.3 compatibility tests

### v0.1.3

-   new configuration parameter `historyMaxQuerySize`
-   version updates (spring-boot 2.1.9.RELEASE; elasticsearch 6.8.3)
-   ElasticsearchEvolutionInitializer logs now the migration duration

### v0.1.2

-   version updates (spring-boot 2.1.3.RELEASE; elasticsearch 6.7.0)
-   bugfix: support more than 10 migration scripts: now 1000.

### v0.1.1

-   improved logging
-   fixed classpath scanning in fat-jars like spring-boot 

### v0.1.0

-   initial version

## 7 Contributing

We welcome contributions to Elasticsearch-Evolution! Here's how you can help: [CONTRIBUTING.md](CONTRIBUTING.md)

## 8 Code of Conduct

see [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md)

## 9 Stats

### 9.1 Star History

[![Star History Chart](https://api.star-history.com/svg?repos=senacor/elasticsearch-evolution&type=date&legend=top-left)](https://www.star-history.com/#senacor/elasticsearch-evolution&type=date&legend=top-left)