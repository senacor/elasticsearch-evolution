package com.senacor.elasticsearch.evolution.core.internal.model.migration;

import com.senacor.elasticsearch.evolution.core.api.MigrationException;
import org.apache.http.entity.ContentType;

import java.util.*;

import static java.util.Objects.requireNonNull;

/**
 * Represents the HTTP request from the migration script
 *
 * @author Andreas Keefer
 */
public class MigrationScriptRequest {

    private static final String HEADER_NAME_CONTENT_TYPE = "Content-Type";

    /**
     * http method,like POST, PUT or DELETE
     * non-null
     */
    private HttpMethod httpMethod;

    /**
     * relative path to the endpoint without hostname, like /my_index
     * nullable
     */
    private String path;

    /**
     * additional http headers, like Content-Type: application/json
     * May be empty.
     */
    private Map<String, String> httpHeader = new HashMap<>();

    /**
     * HTTP body to send.
     * nullable.
     */
    private StringBuilder body = new StringBuilder();

    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    public MigrationScriptRequest setHttpMethod(HttpMethod httpMethod) {
        this.httpMethod = httpMethod;
        return this;
    }

    public String getPath() {
        return path;
    }

    public MigrationScriptRequest setPath(String path) {
        this.path = path;
        return this;
    }

    public Map<String, String> getHttpHeader() {
        return httpHeader;
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
    public String toString() {
        return "MigrationScript{" +
                "httpMethod='" + httpMethod + '\'' +
                ", path='" + path + '\'' +
                ", httpHeader=" + httpHeader +
                ", body='" + body + '\'' +
                '}';
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
        return body.length() == 0;
    }

    public Optional<ContentType> getContentType() {
        if (null == httpHeader) {
            return Optional.empty();
        }
        return httpHeader.entrySet()
                .stream()
                .filter(entry -> HEADER_NAME_CONTENT_TYPE.equalsIgnoreCase(entry.getKey()))
                .map(entry -> ContentType.parse(entry.getValue()))
                .findFirst();
    }

    public enum HttpMethod {
        GET,
        HEAD,
        POST,
        PUT,
        DELETE,
        OPTIONS,
        PATCH;

        public static HttpMethod create(String method) throws MigrationException {
            String normalizedMethod = requireNonNull(method, "method must not be null")
                    .toUpperCase()
                    .trim();
            return Arrays.stream(values())
                    .filter(m -> m.name().equals(normalizedMethod))
                    .findFirst()
                    .orElseThrow(() -> new MigrationException(String.format(
                            "Method '%s' not supported, only %s is supported.",
                            method, Arrays.toString(values()))));
        }
    }
}
