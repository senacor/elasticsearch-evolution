package com.senacor.elasticsearch.evolution.core.internal.model.migration;

import com.senacor.elasticsearch.evolution.rest.abstracion.HttpMethod;
import lombok.Getter;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents the HTTP request from the migration script
 *
 * @author Andreas Keefer
 */
@ToString
public class MigrationScriptRequest {

    /**
     * http method,like POST, PUT or DELETE
     * non-null
     */
    @Getter
    private HttpMethod httpMethod;

    /**
     * relative path to the endpoint without hostname, like /my_index
     * nullable
     */
    @Getter
    private String path;

    /**
     * additional http headers, like Content-Type: application/json
     * May be empty.
     */
    @Getter
    private Map<String, String> httpHeader = new HashMap<>();

    /**
     * HTTP body to send.
     * nullable.
     */
    private StringBuilder body = new StringBuilder();

    public MigrationScriptRequest setHttpMethod(HttpMethod httpMethod) {
        this.httpMethod = httpMethod;
        return this;
    }

    public MigrationScriptRequest setPath(String path) {
        this.path = path;
        return this;
    }

    public MigrationScriptRequest setHttpHeader(Map<String, String> httpHeader) {
        this.httpHeader = httpHeader;
        return this;
    }

    public MigrationScriptRequest addHttpHeader(String header, String value) {
        this.httpHeader.put(header, value);
        return this;
    }

    public String getBody() {
        return body.toString();
    }

    public MigrationScriptRequest setBody(String body) {
        this.body = new StringBuilder(body);
        return this;
    }

    public MigrationScriptRequest addToBody(String bodyPart) {
        this.body.append(bodyPart);
        return this;
    }

    @Override
    public int hashCode() {
        return Objects.hash(httpMethod, path, httpHeader, body);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final MigrationScriptRequest other = (MigrationScriptRequest) obj;
        return Objects.equals(this.httpMethod, other.httpMethod)
                && Objects.equals(this.path, other.path)
                && Objects.equals(this.httpHeader, other.httpHeader)
                && Objects.equals(this.body.toString(), other.body.toString());
    }

    public boolean isBodyEmpty() {
        return body.isEmpty();
    }
}
