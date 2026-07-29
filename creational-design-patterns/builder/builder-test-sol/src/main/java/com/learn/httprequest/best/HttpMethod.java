package com.learn.httprequest.best;

import java.util.Set;

public enum HttpMethod {
    GET,
    POST,
    PUT,
    DELETE,
    PATCH;

    private static final Set<HttpMethod> BODY_ALLOWED = Set.of(POST, PUT, PATCH);

    public boolean allowsBody() {
        return BODY_ALLOWED.contains(this);
    }
}
