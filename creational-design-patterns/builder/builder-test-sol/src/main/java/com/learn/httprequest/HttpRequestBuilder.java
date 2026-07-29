package com.learn.httprequest;

import java.time.Duration;
import java.util.List;
import java.util.Map;

public class HttpRequestBuilder implements  RequestBuilder{
    private String url;
    private HttpMethod method;
    private Map<String, List<String>> headers;
    private int maxRetries = 0;
    private byte[] body;
    private String idempotencyKey;
    private Duration timeout = Duration.ofSeconds(30);
    private Map<String , String> queryParams;

    @Override
    public HttpRequestBuilder url(String url) {
        this.url = url;
        return this;
    }

    @Override
    public HttpRequestBuilder method(HttpMethod method) {
        this.method = method;
        return this;
    }

    @Override
    public HttpRequestBuilder headers(Map<String, List<String>> headers) {
        this.headers = headers;
        return this;
    }

    @Override
    public HttpRequestBuilder queryParams(Map<String, String> params) {
        this.queryParams = params;
        return this;
    }

    @Override
    public HttpRequestBuilder timeout(Duration timeout) {
        if (timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("timeout must be > 0");
        }
        this.timeout = timeout;
        return this;
    }

    @Override
    public HttpRequestBuilder body(byte[] body) {
        this.body = body == null ? null : body.clone();
        return this;
    }

    @Override
    public HttpRequestBuilder maxRetries(int maxRetries) {
        if (maxRetries < 0){
            throw new IllegalArgumentException("maxRetries can't be less than 0");
        }
        this.maxRetries = maxRetries;
        return this;
    }

    @Override
    public HttpRequestBuilder idempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
        return this;
    }

    public HttpRequest build() {
        vaildateMethod();
        vailateUrl();
        validateBodyExistence();
        return new HttpRequest(this);
    }

    private void vaildateMethod() {
        if (method == null) {
            throw new IllegalArgumentException("method can't be null");
        }
    }

    private void validateBodyExistence() {
        if (body != null && (method.equals(HttpMethod.GET) || method.equals(HttpMethod.DELETE))) {
            throw new IllegalArgumentException("body can't be null");
        }
    }

    private void vailateUrl() {
        if (url == null) {
            throw new IllegalArgumentException("url can't be null");
        }
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
        return body;
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
