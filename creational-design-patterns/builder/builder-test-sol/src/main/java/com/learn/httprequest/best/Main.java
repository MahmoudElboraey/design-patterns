package com.learn.httprequest.best;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== best.HttpRequest — scenario tests ===\n");

        // ---- happy paths ----
        happyGet();
        happyPostWithBody();

        // ---- Rule 1: no method / no url -> cannot exist ----
        expectReject("Rule1: missing method rejected",
                () -> HttpRequest.builder().url("https://api.svc").build());
        expectReject("Rule1: missing url rejected",
                () -> HttpRequest.builder().method(HttpMethod.GET).build());
        expectReject("Rule1: blank url rejected",
                () -> HttpRequest.builder().method(HttpMethod.GET).url("   ").build());
        expectReject("Rule1: relative url rejected",
                () -> HttpRequest.builder().method(HttpMethod.GET).url("/users").build());

        // ---- Rule 2: body on GET/DELETE illegal ----
        expectReject("Rule2: body on GET rejected",
                () -> HttpRequest.builder().method(HttpMethod.GET).url("https://api.svc").body("x".getBytes()).build());
        expectReject("Rule2: body on DELETE rejected",
                () -> HttpRequest.builder().method(HttpMethod.DELETE).url("https://api.svc").body("x".getBytes()).build());

        // ---- Rule 3: timeout > 0 ----
        expectReject("Rule3: zero timeout rejected",
                () -> HttpRequest.builder().method(HttpMethod.GET).url("https://api.svc").timeout(Duration.ZERO).build());
        expectReject("Rule3: negative timeout rejected",
                () -> HttpRequest.builder().method(HttpMethod.GET).url("https://api.svc").timeout(Duration.ofSeconds(-1)).build());
        expectReject("Rule3: null timeout rejected",
                () -> HttpRequest.builder().method(HttpMethod.GET).url("https://api.svc").timeout(null).build());

        // ---- Rule 4: maxRetries >= 0 ----
        expectReject("Rule4: negative maxRetries rejected",
                () -> HttpRequest.builder().method(HttpMethod.GET).url("https://api.svc").maxRetries(-1).build());
        zeroRetriesAllowed();

        // ---- multi-map headers ----
        multiValueHeaderPreserved();

        // ---- immutability / defensive copies ----
        mutatingSourceHeadersDoesNotLeak();
        returnedHeadersUnmodifiable();
        mutatingSourceBodyDoesNotLeak();
        mutatingReturnedBodyDoesNotLeak();

        // ---- defaults ----
        defaultsApplied();

        // ---- transport retries same spec on many threads ----
        sameSpecReadByManyThreads();

        System.out.println("\n=== " + passed + " passed, " + failed + " failed ===");
    }

    private static void happyGet() {
        try {
            HttpRequest r = HttpRequest.builder()
                    .method(HttpMethod.GET)
                    .url("https://api.svc/users")
                    .build();
            check("GET with url builds", r.getMethod() == HttpMethod.GET && "https://api.svc/users".equals(r.getUrl()));
        } catch (RuntimeException e) {
            fail("GET with url builds", "unexpected: " + e.getMessage());
        }
    }

    private static void happyPostWithBody() {
        try {
            byte[] body = "{\"x\":1}".getBytes();
            HttpRequest r = HttpRequest.builder()
                    .method(HttpMethod.POST)
                    .url("https://api.svc/users")
                    .body(body)
                    .build();
            check("POST with body builds", Arrays.equals(body, r.getBody()));
        } catch (RuntimeException e) {
            fail("POST with body builds", "unexpected: " + e.getMessage());
        }
    }

    private static void zeroRetriesAllowed() {
        try {
            HttpRequest r = HttpRequest.builder()
                    .method(HttpMethod.GET)
                    .url("https://api.svc")
                    .maxRetries(0)
                    .build();
            check("Rule4: zero maxRetries allowed", r.getMaxRetries() == 0);
        } catch (RuntimeException e) {
            fail("Rule4: zero maxRetries allowed", "unexpected: " + e.getMessage());
        }
    }

    private static void multiValueHeaderPreserved() {
        try {
            Map<String, List<String>> headers = new HashMap<>();
            headers.put("Accept", List.of("a", "b"));
            HttpRequest r = HttpRequest.builder()
                    .method(HttpMethod.GET)
                    .url("https://api.svc")
                    .headers(headers)
                    .build();
            List<String> accept = r.getHeaders().get("Accept");
            check("Headers: one name keeps many values",
                    accept.size() == 2 && accept.contains("a") && accept.contains("b"));
        } catch (RuntimeException e) {
            fail("Headers: one name keeps many values", "unexpected: " + e.getMessage());
        }
    }

    private static void mutatingSourceHeadersDoesNotLeak() {
        try {
            Map<String, List<String>> headers = new HashMap<>();
            headers.put("Accept", List.of("a", "b"));
            HttpRequest r = HttpRequest.builder()
                    .method(HttpMethod.GET)
                    .url("https://api.svc")
                    .headers(headers)
                    .build();
            headers.put("Authorization", List.of("secret"));
            check("Immutable: mutating source headers does not leak",
                    r.getHeaders().size() == 1 && !r.getHeaders().containsKey("Authorization"));
        } catch (RuntimeException e) {
            fail("Immutable: mutating source headers does not leak", "unexpected: " + e.getMessage());
        }
    }

    private static void returnedHeadersUnmodifiable() {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Accept", List.of("a"));
        HttpRequest r = HttpRequest.builder()
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
            HttpRequest r = HttpRequest.builder()
                    .method(HttpMethod.POST)
                    .url("https://api.svc")
                    .body(body)
                    .build();
            Arrays.fill(body, (byte) 0);
            check("Immutable: mutating source body does not leak",
                    Arrays.equals("payload".getBytes(), r.getBody()));
        } catch (RuntimeException e) {
            fail("Immutable: mutating source body does not leak", "unexpected: " + e.getMessage());
        }
    }

    private static void mutatingReturnedBodyDoesNotLeak() {
        try {
            HttpRequest r = HttpRequest.builder()
                    .method(HttpMethod.POST)
                    .url("https://api.svc")
                    .body("payload".getBytes())
                    .build();
            Arrays.fill(r.getBody(), (byte) 0);
            check("Immutable: mutating returned body does not corrupt spec",
                    Arrays.equals("payload".getBytes(), r.getBody()));
        } catch (RuntimeException e) {
            fail("Immutable: mutating returned body does not corrupt spec", "unexpected: " + e.getMessage());
        }
    }

    private static void defaultsApplied() {
        try {
            HttpRequest r = HttpRequest.builder()
                    .method(HttpMethod.GET)
                    .url("https://api.svc")
                    .build();
            check("Defaults: timeout=30s, maxRetries=0",
                    Duration.ofSeconds(30).equals(r.getTimeout()) && r.getMaxRetries() == 0);
        } catch (RuntimeException e) {
            fail("Defaults: timeout=30s, maxRetries=0", "unexpected: " + e.getMessage());
        }
    }

    private static void sameSpecReadByManyThreads() throws InterruptedException {
        HttpRequest spec = HttpRequest.builder()
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
        if (cond) pass(name);
        else fail(name, "assertion false");
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
