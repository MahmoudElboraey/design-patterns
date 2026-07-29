package com.learn.httprequest.best;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class HttpRequest {

    private final HttpMethod method;
    private final String url;
    private final Map<String, List<String>> headers;
    private final Map<String, String> queryParams;
    private final byte[] body;
    private final Duration timeout;
    private final int maxRetries;
    private final String idempotencyKey;

    private HttpRequest(Builder b) {
        this.method = b.method;
        this.url = b.url;
        this.headers = deepCopyHeaders(b.headers);
        this.queryParams = b.queryParams == null ? Map.of() : Map.copyOf(b.queryParams);
        this.body = b.body == null ? null : b.body.clone();
        this.timeout = b.timeout;
        this.maxRetries = b.maxRetries;
        this.idempotencyKey = b.idempotencyKey;
    }

    public static Builder builder() {
        return new Builder();
    }

    private static Map<String, List<String>> deepCopyHeaders(Map<String, List<String>> src) {
        if (src == null) {
            return Map.of();
        }
        Map<String, List<String>> copy = new HashMap<>(src.size());
        for (Map.Entry<String, List<String>> e : src.entrySet()) {
            copy.put(e.getKey(), List.copyOf(e.getValue()));
        }
        return Map.copyOf(copy);
    }

    public HttpMethod getMethod() {
        return method;
    }

    public String getUrl() {
        return url;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public Map<String, String> getQueryParams() {
        return queryParams;
    }

    public byte[] getBody() {
        return body == null ? null : body.clone();
    }

    public Duration getTimeout() {
        return timeout;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public static final class Builder {
        private HttpMethod method;
        private String url;
        private Map<String, List<String>> headers;
        private Map<String, String> queryParams;
        private byte[] body;
        private Duration timeout = Duration.ofSeconds(30);
        private int maxRetries = 0;
        private String idempotencyKey;

        private Builder() {
        }

        public Builder method(HttpMethod method) {
            this.method = method;
            return this;
        }

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder headers(Map<String, List<String>> headers) {
            this.headers = headers == null ? null : deepCopyHeaders(headers);
            return this;
        }

        public Builder queryParams(Map<String, String> queryParams) {
            this.queryParams = queryParams == null ? null : Map.copyOf(queryParams);
            return this;
        }

        public Builder body(byte[] body) {
            this.body = body == null ? null : body.clone();
            return this;
        }

        public Builder timeout(Duration timeout) {
            Objects.requireNonNull(timeout, "timeout");
            if (timeout.isZero() || timeout.isNegative()) {
                throw new IllegalArgumentException("timeout must be > 0, was " + timeout);
            }
            this.timeout = timeout;
            return this;
        }

        public Builder maxRetries(int maxRetries) {
            if (maxRetries < 0) {
                throw new IllegalArgumentException("maxRetries must be >= 0, was " + maxRetries);
            }
            this.maxRetries = maxRetries;
            return this;
        }

        public Builder idempotencyKey(String idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
            return this;
        }

        public HttpRequest build() {
            validate();
            return new HttpRequest(this);
        }

        private void validate() {
            if (method == null) {
                throw new IllegalArgumentException("method is required");
            }
            requireAbsoluteUrl(url);
            if (body != null && !method.allowsBody()) {
                throw new IllegalArgumentException("body is not allowed for method " + method);
            }
        }

        private static void requireAbsoluteUrl(String url) {
            if (url == null || url.isBlank()) {
                throw new IllegalArgumentException("url is required");
            }
            final URI uri;
            try {
                uri = new URI(url);
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException("url is not a valid URI: " + url, e);
            }
            if (!uri.isAbsolute() || uri.getHost() == null) {
                throw new IllegalArgumentException("url must be an absolute URL, was: " + url);
            }
        }
    }
}
