package com.senacor.elasticsearch.evolution.rest.abstracion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HttpMethodTest {

    @ParameterizedTest
    @EnumSource(HttpMethod.class)
    void create_shouldReturnCorrectHttpMethod_whenValidMethodNameProvided(HttpMethod method) {
        // when
        HttpMethod result = HttpMethod.create(method.name());

        // then
        assertThat(result).isEqualTo(method);
    }

    @ParameterizedTest
    @ValueSource(strings = {"get", "GET", "Get", "gEt", " get "})
    void create_shouldHandleDifferentCasing_forGetMethod(String methodName) {
        // when
        HttpMethod result = HttpMethod.create(methodName);

        // then
        assertThat(result).isEqualTo(HttpMethod.GET);
    }

    @ParameterizedTest
    @ValueSource(strings = {"post", "POST", "Post", "pOsT", " post "})
    void create_shouldHandleDifferentCasing_forPostMethod(String methodName) {
        // when
        HttpMethod result = HttpMethod.create(methodName);

        // then
        assertThat(result).isEqualTo(HttpMethod.POST);
    }

    @ParameterizedTest
    @ValueSource(strings = {"put", "PUT", "Put", " put "})
    void create_shouldHandleDifferentCasing_forPutMethod(String methodName) {
        // when
        HttpMethod result = HttpMethod.create(methodName);

        // then
        assertThat(result).isEqualTo(HttpMethod.PUT);
    }

    @ParameterizedTest
    @ValueSource(strings = {"delete", "DELETE", "Delete", " delete "})
    void create_shouldHandleDifferentCasing_forDeleteMethod(String methodName) {
        // when
        HttpMethod result = HttpMethod.create(methodName);

        // then
        assertThat(result).isEqualTo(HttpMethod.DELETE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"head", "HEAD", "Head", " head "})
    void create_shouldHandleDifferentCasing_forHeadMethod(String methodName) {
        // when
        HttpMethod result = HttpMethod.create(methodName);

        // then
        assertThat(result).isEqualTo(HttpMethod.HEAD);
    }

    @ParameterizedTest
    @ValueSource(strings = {"options", "OPTIONS", "Options", " options "})
    void create_shouldHandleDifferentCasing_forOptionsMethod(String methodName) {
        // when
        HttpMethod result = HttpMethod.create(methodName);

        // then
        assertThat(result).isEqualTo(HttpMethod.OPTIONS);
    }

    @ParameterizedTest
    @ValueSource(strings = {"patch", "PATCH", "Patch", " patch "})
    void create_shouldHandleDifferentCasing_forPatchMethod(String methodName) {
        // when
        HttpMethod result = HttpMethod.create(methodName);

        // then
        assertThat(result).isEqualTo(HttpMethod.PATCH);
    }

    @Test
    void create_shouldThrowNullPointerException_whenMethodIsNull() {
        assertThatThrownBy(() -> HttpMethod.create(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("method must not be null");
    }

    @ParameterizedTest
    @ValueSource(strings = {"CONNECT", "TRACE", "INVALID", "get_post", "123", "G E T"})
    void create_shouldThrowIllegalArgumentException_whenMethodIsNotSupported(String method) {
        // when & then
        assertThatThrownBy(() -> HttpMethod.create(method))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not supported")
                .hasMessageContaining(method.trim());
    }
}