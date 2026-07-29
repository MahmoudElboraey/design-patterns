package com.learn.httprequest;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== HttpRequest builder — scenario tests ===\n");

        happyPathGetBuilds();
        happyPathPostWithBodyBuilds();

        // Rule 1: no method or no url cannot exist
        missingMethodRejected();
        missingUrlRejected();

        // Rule 2: body on GET or DELETE illegal
        bodyOnGetRejected();
        bodyOnDeleteRejected();

        // Rule 3: timeout must be > 0
        zeroTimeoutRejected();
        negativeTimeoutRejected();

        // Rule 4: maxRetries must be >= 0
        negativeRetriesRejected();
        zeroRetriesAllowed();

        // headers is a multi-map: one name -> many values
        multiValueHeaderPreserved();

        // immutability / defensive copies (object guarantees its own safety)
        mutatingSourceHeadersDoesNotLeak();
        returnedHeadersUnmodifiable();
        mutatingSourceBodyDoesNotLeak();
        mutatingReturnedBodyDoesNotLeak();

        // defaults
        defaultsApplied();

        // transport layer retries the same spec on several threads at once
        sameSpecReadByManyThreads();

        System.out.println("\n=== " + passed + " passed, " + failed + " failed ===");
    }

    // ---- happy paths ----

    private static void happyPathGetBuilds() {
        try {
            HttpRequest r = new HttpRequestBuilder()
                    .method(HttpMethod.GET)
                    .url("https://api.svc/users")
                    .build();
            check("GET with url builds", r.getMethod() == HttpMethod.GET && "https://api.svc/users".equals(r.getUrl()));
        } catch (RuntimeException e) {
            fail("GET with url builds", "unexpected: " + e.getMessage());
        }
    }

    private static void happyPathPostWithBodyBuilds() {
        try {
            byte[] body = "{\"x\":1}".getBytes();
            HttpRequest r = new HttpRequestBuilder()
                    .method(HttpMethod.POST)
                    .url("https://api.svc/users")
                    .body(body)
                    .build();
            check("POST with body builds", Arrays.equals(body, r.getBody()));
        } catch (RuntimeException e) {
            fail("POST with body builds", "unexpected: " + e.getMessage());
        }
    }

    // ---- Rule 1 ----

    private static void missingMethodRejected() {
        expectReject("Rule1: missing method rejected", () -> new HttpRequestBuilder()
                .url("https://api.svc")
                .build());
    }

    private static void missingUrlRejected() {
        expectReject("Rule1: missing url rejected", () -> new HttpRequestBuilder()
                .method(HttpMethod.GET)
                .build());
    }

    // ---- Rule 2 ----

    private static void bodyOnGetRejected() {
        expectReject("Rule2: body on GET rejected", () -> new HttpRequestBuilder()
                .method(HttpMethod.GET)
                .url("https://api.svc")
                .body("nope".getBytes())
                .build());
    }

    private static void bodyOnDeleteRejected() {
        expectReject("Rule2: body on DELETE rejected", () -> new HttpRequestBuilder()
                .method(HttpMethod.DELETE)
                .url("https://api.svc")
                .body("nope".getBytes())
                .build());
    }

    // ---- Rule 3 ----

    private static void zeroTimeoutRejected() {
        expectReject("Rule3: zero timeout rejected", () -> new HttpRequestBuilder()
                .method(HttpMethod.GET)
                .url("https://api.svc")
                .timeout(Duration.ZERO)
                .build());
    }

    private static void negativeTimeoutRejected() {
        expectReject("Rule3: negative timeout rejected", () -> new HttpRequestBuilder()
                .method(HttpMethod.GET)
                .url("https://api.svc")
                .timeout(Duration.ofSeconds(-1))
                .build());
    }

    // ---- Rule 4 ----

    private static void negativeRetriesRejected() {
        expectReject("Rule4: negative maxRetries rejected", () -> new HttpRequestBuilder()
                .method(HttpMethod.GET)
                .url("https://api.svc")
                .maxRetries(-1)
                .build());
    }

    private static void zeroRetriesAllowed() {
        try {
            HttpRequest r = new HttpRequestBuilder()
                    .method(HttpMethod.GET)
                    .url("https://api.svc")
                    .maxRetries(0)
                    .build();
            check("Rule4: zero maxRetries allowed", r.getMaxRetries() == 0);
        } catch (RuntimeException e) {
            fail("Rule4: zero maxRetries allowed", "unexpected: " + e.getMessage());
        }
    }

    // ---- headers multi-map ----

    private static void multiValueHeaderPreserved() {
        try {
            Map<String, List<String>> headers = new ConcurrentHashMap<>();
            headers.put("Accept", List.of("a", "b"));
            HttpRequest r = new HttpRequestBuilder()
                    .method(HttpMethod.GET)
                    .url("https://api.svc")
                    .headers(headers)
                    .build();
            List<String> accept = r.getHeaders().get("Accept");
            check("Headers: one name keeps many values", accept.size() == 2
                    && accept.contains("a") && accept.contains("b"));
        } catch (RuntimeException e) {
            fail("Headers: one name keeps many values", "unexpected: " + e.getMessage());
        }
    }

    // ---- immutability / defensive copies ----

    private static void mutatingSourceHeadersDoesNotLeak() {
        try {
            Map<String, List<String>> headers = new HashMap<>();
            headers.put("Accept", List.of("a", "b"));
            HttpRequest r = new HttpRequestBuilder()
                    .method(HttpMethod.GET)
                    .url("https://api.svc")
                    .headers(headers)
                    .build();

            // caller mutates its own map AFTER build -> spec must not change
            headers.put("Authorization", List.of("secret"));
            headers.get("Accept"); // untouched

            check("Immutable: mutating source headers map does not leak in",
                    r.getHeaders().size() == 1 && !r.getHeaders().containsKey("Authorization"));
        } catch (RuntimeException e) {
            fail("Immutable: mutating source headers map does not leak in", "unexpected: " + e.getMessage());
        }
    }

    private static void returnedHeadersUnmodifiable() {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Accept", List.of("a"));
        HttpRequest r = new HttpRequestBuilder()
                .method(HttpMethod.GET)
                .url("https://api.svc")
                .headers(headers)
                .build();
        try {
            r.getHeaders().put("Injected", List.of("x"));
            fail("Immutable: returned headers reject mutation", "expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
            pass("Immutable: returned headers reject mutation");
        }
    }

    private static void mutatingSourceBodyDoesNotLeak() {
        try {
            byte[] body = "payload".getBytes();
            HttpRequest r = new HttpRequestBuilder()
                    .method(HttpMethod.POST)
                    .url("https://api.svc")
                    .body(body)
                    .build();

            // caller scribbles over its own array after build
            Arrays.fill(body, (byte) 0);

            check("Immutable: mutating source body array does not leak in",
                    Arrays.equals("payload".getBytes(), r.getBody()));
        } catch (RuntimeException e) {
            fail("Immutable: mutating source body array does not leak in", "unexpected: " + e.getMessage());
        }
    }

    private static void mutatingReturnedBodyDoesNotLeak() {
        try {
            HttpRequest r = new HttpRequestBuilder()
                    .method(HttpMethod.POST)
                    .url("https://api.svc")
                    .body("payload".getBytes())
                    .build();

            // caller scribbles over the array handed back by the getter
            Arrays.fill(r.getBody(), (byte) 0);

            check("Immutable: mutating returned body array does not corrupt spec",
                    Arrays.equals("payload".getBytes(), r.getBody()));
        } catch (RuntimeException e) {
            fail("Immutable: mutating returned body array does not corrupt spec", "unexpected: " + e.getMessage());
        }
    }

    // ---- defaults ----

    private static void defaultsApplied() {
        try {
            HttpRequest r = new HttpRequestBuilder()
                    .method(HttpMethod.GET)
                    .url("https://api.svc")
                    .build();
            check("Defaults: timeout=30s, maxRetries=0",
                    Duration.ofSeconds(30).equals(r.getTimeout()) && r.getMaxRetries() == 0);
        } catch (RuntimeException e) {
            fail("Defaults: timeout=30s, maxRetries=0", "unexpected: " + e.getMessage());
        }
    }

    // ---- transport retries same spec on several threads ----

    private static void sameSpecReadByManyThreads() throws InterruptedException {
        HttpRequest spec = new HttpRequestBuilder()
                .method(HttpMethod.POST)
                .url("https://api.svc/orders")
                .body("payload".getBytes())
                .maxRetries(3)
                .idempotencyKey("k-123")
                .build();

        int threads = 16;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger ok = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    for (int j = 0; j < 10_000; j++) {
                        if (spec.getMethod() == HttpMethod.POST
                                && "https://api.svc/orders".equals(spec.getUrl())
                                && "k-123".equals(spec.getIdempotencyKey())
                                && spec.getMaxRetries() == 3) {
                            ok.incrementAndGet();
                        }
                    }
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        done.await(5, TimeUnit.SECONDS);
        pool.shutdownNow();

        check("Concurrency: same spec read consistently by " + threads + " threads",
                ok.get() == threads * 10_000);
    }

    // ---- tiny harness ----

    private static void expectReject(String name, Runnable r) {
        try {
            r.run();
            fail(name, "expected exception, none thrown");
        } catch (RuntimeException expected) {
            pass(name);
        }
    }

    private static void check(String name, boolean cond) {
        if (cond) {
            pass(name);
        } else {
            fail(name, "assertion false");
        }
    }

    private static void pass(String name) {
        passed++;
        System.out.println("PASS  " + name);
    }

    private static void fail(String name, String why) {
        failed++;
        System.out.println("FAIL  " + name + "  (" + why + ")");
    }
}
