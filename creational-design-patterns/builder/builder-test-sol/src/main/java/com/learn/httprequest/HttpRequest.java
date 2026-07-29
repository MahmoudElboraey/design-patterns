package com.learn.httprequest;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class HttpRequest {
    private final String url;
    private final HttpMethod method;
    private final Map<String, List<String>> headers;
    private final int maxRetries;
    private final byte[] body;
    private final String idempotencyKey;
    private final Duration timeout;
    private final Map<String , String> queryParams;


    public HttpRequest(HttpRequestBuilder httpRequestBuilder){
        byte[] rawBody = httpRequestBuilder.getBody();
        this.body = rawBody == null ? null : rawBody.clone();
        this.url = httpRequestBuilder.getUrl();
        this.method = httpRequestBuilder.getMethod();
        this.headers = copyHeaders(httpRequestBuilder.getHeaders());
        this.idempotencyKey = httpRequestBuilder.getIdempotencyKey();
        this.maxRetries = httpRequestBuilder.getMaxRetries();
        this.queryParams = copyParams(httpRequestBuilder.getQueryParams());
        this.timeout = httpRequestBuilder.getTimeout();
    }

    private static Map<String, List<String>> copyHeaders(Map<String, List<String>> src) {
        if (src == null) {
            return Map.of();
        }
        Map<String, List<String>> copy = new HashMap<>();
        for (Map.Entry<String, List<String>> e : src.entrySet()) {
            copy.put(e.getKey(), List.copyOf(e.getValue()));
        }
        return Map.copyOf(copy);
    }

    private static Map<String, String> copyParams(Map<String, String> src) {
        return src == null ? Map.of() : Map.copyOf(src);
    }


    public String getUrl() {
        return url;
    }

    public HttpMethod getMethod() {
        return method;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public byte[] getBody() {
        return body == null ? null : body.clone();
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public Map<String, String> getQueryParams() {
        return queryParams;
    }
}
