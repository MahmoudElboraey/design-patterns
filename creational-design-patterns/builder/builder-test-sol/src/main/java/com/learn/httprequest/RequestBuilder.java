package com.learn.httprequest;

import java.time.Duration;
import java.util.List;
import java.util.Map;

public interface RequestBuilder {

    RequestBuilder url(String url);
    RequestBuilder method(HttpMethod method);
    RequestBuilder headers(Map<String, List<String>> headers);
    RequestBuilder queryParams(Map<String, String> params);
    RequestBuilder timeout(Duration timeout);
    RequestBuilder body(byte[] body);
    RequestBuilder maxRetries(int maxRetries);
    RequestBuilder idempotencyKey(String idempotencyKey);

}
