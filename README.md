# Elasticsearch-Evolution

> A library to migrate Elasticsearch mappings. Inspired by flyway.

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://raw.githubusercontent.com/senacor/elasticsearch-evolution/master/LICENSE)
[![LICENSE](https://img.shields.io/badge/license-Anti%20996-blue.svg)](https://github.com/996icu/996.ICU/blob/master/LICENSE)
[![996.icu](https://img.shields.io/badge/link-996.icu-red.svg)](https://996.icu)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.senacor.elasticsearch.evolution/elasticsearch-evolution-core/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.senacor.elasticsearch.evolution/elasticsearch-evolution-core)
[![Javadocs](https://www.javadoc.io/badge/com.senacor.elasticsearch.evolution/elasticsearch-evolution-core.svg)](https://www.javadoc.io/doc/com.senacor.elasticsearch.evolution/elasticsearch-evolution-core)
[![Build Status](https://travis-ci.org/senacor/elasticsearch-evolution.svg?branch=master)](https://travis-ci.org/senacor/elasticsearch-evolution)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/a629ba3201104ecc81c6af7671b29b05)](https://www.codacy.com/app/xtermi2/elasticsearch-evolution?utm_source=github.com&utm_medium=referral&utm_content=senacor/elasticsearch-evolution&utm_campaign=Badge_Grade)
[![codebeat badge](https://codebeat.co/badges/29dc74db-88e2-4b26-963b-14eb340ae275)](https://codebeat.co/projects/github-com-senacor-elasticsearch-evolution-master)
[![Coverage Status](https://coveralls.io/repos/github/senacor/elasticsearch-evolution/badge.svg?branch=master)](https://coveralls.io/github/senacor/elasticsearch-evolution?branch=master)

## 1 Evolve your Elasticsearch mapping easily and reliable across all your instances

Elasticsearch-Evolution executes versioned migration scripts reliable and persists the execution state in an internal Elasticsearch index.
Successful executed migration scripts will not be executed again! 

## 2 Features

-   tested on Java 8, 9, 10, 11, 12, 13 and 14
-   runs on Spring-Boot 1.5, 2.0, 2.1, 2.2 and 2.3 (and of course without Spring-Boot)
-   runs on Elasticsearch 7.x, 6.8.x, 6.7.x, 6.6.x, 6.5.x, 6.4.x, 6.3.x, 6.2.x
-   highly configurable (e.g. location(s) of your migration files, migration files format pattern)
-   placeholder substitution in migration scripts
-   easily extendable to your needs
-   supports microservices / multiple parallel running instances via logical database locks
-   ready to use default configuration
-   line comments in migration files

## 3 Quickstart

### 3.1 Quickstart with Spring-Boot starter

First add the latest version of Elasticsearch-Evolution spring boot starter as a dependency in your maven pom.xml:

```xml
<dependency>
    <groupId>com.senacor.elasticsearch.evolution</groupId>
    <artifactId>spring-boot-starter-elasticsearch-evolution</artifactId>
    <version>0.1.3</version>
</dependency>
```

Elasticsearch-Evolution uses internally Elastics RestHighLevelClient and requires at minimum version 6.6.0. Spring boot uses a older version, so update it in your pom.xml:

```xml
<properties>
    <elasticsearch.version>6.6.0</elasticsearch.version>
</properties>
```

Place your migration scripts in your application classpath at `es/evolution`

That's it. Elasticsearch-Evolution runs at application startup and expects your Elasticsearch at <http://localhost:9200>

### 3.2 Quickstart with core library

First add the latest version of Elasticsearch-Evolution core as a dependency:

```xml
<dependency>
    <groupId>com.senacor.elasticsearch.evolution</groupId>
    <artifactId>elasticsearch-evolution-core</artifactId>
    <version>0.1.3</version>
</dependency>
```

Place your migration scripts in your application classpath at `es/evolution`

Create a `ElasticsearchEvolution` instance and execute the migration.

```java
// first create a Elastic RestHighLevelClient
RestHighLevelClient restHighLevelClient = new RestHighLevelClient(
        RestClient.builder(HttpHost.create("http://localhost:9200")));
// then create a ElasticsearchEvolution configuration and create a instance of ElasticsearchEvolution with that configuration
ElasticsearchEvolution elasticsearchEvolution = ElasticsearchEvolution.configure()
        .load(restHighLevelClient);
// execute the migration
elasticsearchEvolution.migrate();
```

## 4 Migration script format

### 4.1 Migration script file content

A Elasticsearch-Evolutions migration script represents just a rest call. Here is an Example:

```http request
PUT _template/my_template
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
    "_doc": {
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
}
```

The first line defines the HTTP method `PUT` and the relative path to the Elasticsearch endpoint `_template/my_template` to create a new mapping template.
Followed by a HTTP Header `Content-Type: application/json`.
After a blank line the HTTP body is defined.

The pattern is strongly oriented in ordinary HTTP requests and consist of 4 parts:

1.  **The HTTP method (required)**. Supported HTTP methods are `GET`, `HEAD`, `POST`, `PUT`, `DELETE`, `OPTIONS` and `PATCH`. 
    The First non-comment line must always start with a HTTP method.
2.  **The path to the Elasticsearch endpoint to call (required)**. The path is separated by a _blank_ from the HTTP method. 
    You can provide any query parameters like in a ordinary browser like this `/my_index_1/_doc/1?refresh=true&op_type=create`
3.  **HTTP Header(s) (optional)**. All non-comment lines after the _HTTP method_ line will be interpreted as HTTP headers. Header name and content are separated by `:`.
4.  **HTTP Body (optional)**. The HTTP Body is separated by a blank line and can contain any content you want to sent to Elasticsearch.

#### 4.1.1 Comments

Elasticsearch-Evolution supports line-comments in its migration scripts. Every line starting with `#` or `//` will be interpreted as a comment-line.
Comment-lines are not send to Elasticsearch, they will be filtered by Elasticsearch-Evolution.

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
-   followed ba a description which can be any text your filesystem supports
-   ended with `esMigrationSuffixes` which is by default `.http` and is configurable and case-insensitive.

Elasticsearch-Evolution uses the version for ordering your scripts and enforces strict ordered execution of your scripts. Out-of-Order execution is not supported.
Elasticsearch-Evolution interprets the version parts as Integers, so each version part must be between 1 (inclusive) and 2,147,483,647 (inclusive).

Here is and example which indicates the ordering: `1.0.1` &lt; `1.1` &lt; `1.2.1` &lt; (`2.0.0` == `2`).
In this example version `1.0.1` is the smallest version and is executed first, after that version `1.1`, `1.2.1` and in the end `2`. 
`2` is the same as `2.0` or `2.0.0` - so leading zeros will be trimed.

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

### 5.1 Spring Boot

You can set the above configurations via Spring Boots default configuration way. Just use the prefix `spring.elasticsearch.evolution`. Here is a example `application.properties`:

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

Since spring boot 2.1 AutoConfiguration for Elasticsearchs REST client is provided (see org.springframework.boot.autoconfigure.elasticsearch.rest.RestClientAutoConfiguration).
You can configure the RestHighLevelClient, required for Elasticsearch-Evolution, just like that in your `application.properties`:

```properties
spring.elasticsearch.rest.uris[0]=https://example.com:9200
spring.elasticsearch.rest.username=my-user-name
spring.elasticsearch.rest.password=my-secret-pw
```

#### 5.1.2 Customize Elasticsearch-Evolutions AutoConfiguration

##### 5.1.2.1 Custom RestHighLevelClient

Elasticsearch-Evolutions just needs a `RestHighLevelClient` as spring bean. 
If you don't have spring boot 2.1 or later or you need a special `RestHighLevelClient` configuration e.g. to accept self signed certificates or disable hostname validation, you can provide a custom `RestHighLevelClient` like this:   

```java
@Bean
public RestHighLevelClient restHighLevelClient() {
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
    return new RestHighLevelClient(builder);
}
```

##### 5.1.2.2 Custom ElasticsearchEvolutionInitializer

Maybe you want to provide a customised Initializer for Elasticsearch-Evolution e.g with another Order:

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