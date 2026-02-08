package com.senacor.elasticsearch.evolution.spring.boot.starter.autoconfigure;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * @author Andreas Keefer
 */
@SpringBootApplication
public class SpringBootTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringBootTestApplication.class, args);
    }

    @Bean
    ElasticsearchEvolutionConfigCustomizer myCustomizer() {
        return config -> config.setJavaMigrations(List.of(new V1_2__AddDocument()));
    }
}
