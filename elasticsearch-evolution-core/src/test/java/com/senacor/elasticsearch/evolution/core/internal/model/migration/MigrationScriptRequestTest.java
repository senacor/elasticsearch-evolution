package com.senacor.elasticsearch.evolution.core.internal.model.migration;

import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Andreas Keefer
 */
class MigrationScriptRequestTest {

    @Nested
    class getContentType {
        @Test
        void nullHeaders() {
            assertThat(new MigrationScriptRequest()
                    .setHttpHeader(null)
                    .getContentType()).isEmpty();
        }

        @Test
        void noContentTypeHeaders() {
            assertThat(new MigrationScriptRequest()
                    .setHttpHeader(new HashMap<>())
                    .getContentType()).isEmpty();
        }

        @Test
        void contentTypeHeadersWasFound() {
            String contentType = "application/json";

            assertThat(
                    new MigrationScriptRequest()
                            .addHttpHeader("content-TYPE", contentType)
                            .getContentType()
                            .map(ContentType::toString))
                    .contains(contentType);
        }
    }
}