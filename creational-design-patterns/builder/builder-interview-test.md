# Builder Pattern — FAANG Interview Test

**Time budget:** 90 min. Whiteboard/IDE. Talk while you build.
**You are allowed to assume:** you already know SOLID, Observer, Strategy, Template Method.
**Grading axis:** correctness of object model > pattern mechanics > edge/failure handling > tradeoff talk.

There are baits in here. They are not typos. Step on one and it costs points.

---

## Part A — Design from scratch (40%)

### Scenario

You own the internal HTTP client library every backend team at the company calls to reach other services. The core value object is an **outbound request spec**: teams build one, hand it to the transport layer, and the transport layer may **retry it on several threads at once**.

A request spec has:

**Required**
- `method` — GET / POST / PUT / DELETE / PATCH
- `url` — absolute URL string

**Optional**
- `headers` — a multi-map: one header name can have many values (e.g. `Accept: a`, `Accept: b`)
- `queryParams` — map of name → value
- `body` — raw bytes (only legal for POST/PUT/PATCH)
- `timeout` — duration, default 30s
- `maxRetries` — int, default 0
- `idempotencyKey` — string

### Rules the object itself must guarantee (not the caller)
1. A spec with no `method` or no `url` **cannot exist**.
2. `body` on a GET or DELETE is illegal.
3. `timeout` must be > 0.
4. `maxRetries` must be ≥ 0.
5. Once built, a spec is **read-only forever**. The transport layer retries it on multiple threads — it must be impossible for one thread to observe a half-mutated or shared-mutable spec.
6. Two teams building specs at the same time must never interfere.

### Deliverables
- **(a)** UML class diagram. Correct arrows (you know the diamond rules from Strategy — same rules apply). Mark what is immutable.
- **(b)** Name the SOLID principles this design leans on and where.
- **(c)** Code: the value object + its builder. Fully working. Include the validation and the multi-thread-safety guarantees — don't just claim them, show them.
- **(d)** One paragraph: why Builder here and not a constructor, a factory, or a plain settermap.

### Questions baked in (answer inline)
- Where does validation live, and what happens on a bad spec — exactly?
- `headers` and `queryParams` are collections the caller hands you. After `build()`, can the caller still reach in and change the spec's headers? Prove your answer in code.
- Can one builder instance be safely reused to build a second, slightly-different spec? What's the trap if the answer is "yes"?

---

## Part B — Production refactor (35%)

Below is real code shipped by a team. It works in the happy path and has caused two incidents.

```java
public class NotificationConfig {
    public String channel;          // "email" | "sms" | "push"
    public String recipient;
    public String subject;
    public String body;
    public List<String> ccList;
    public Map<String, String> headers;
    public int retries;
    public boolean urgent;

    public NotificationConfig(String channel, String recipient) {
        this.channel = channel;
        this.recipient = recipient;
    }
    public NotificationConfig(String channel, String recipient, String subject, String body) {
        this.channel = channel;
        this.recipient = recipient;
        this.subject = subject;
        this.body = body;
    }
    public NotificationConfig(String channel, String recipient, String subject,
                              String body, List<String> ccList, Map<String,String> headers,
                              int retries, boolean urgent) {
        this.channel = channel;
        this.recipient = recipient;
        this.subject = subject;
        this.body = body;
        this.ccList = ccList;
        this.headers = headers;
        this.retries = retries;
        this.urgent = urgent;
    }
}
```

Call site that caused **incident #1**:
```java
List<String> cc = new ArrayList<>();
cc.add("boss@corp.com");
NotificationConfig cfg = new NotificationConfig("email", "a@corp.com", "Hi", "body", cc, null, 3, false);
sendAsync(cfg);
cc.add("leaked@evil.com");   // added AFTER handing config off. boss email now has an extra recipient.
```

Call site that caused **incident #2**:
```java
NotificationConfig cfg = new NotificationConfig("sms", "+123456789");
cfg.subject = "won't be read for sms but nobody validates it";
cfg.retries = -5;            // negative retries => retry loop underflowed, hammered the provider
sendAsync(cfg);
```

### Deliverables
- **(a)** Name every distinct smell (there are at least 4). One line each.
- **(b)** UML of the refactor.
- **(c)** SOLID writeup — which principles the original violates, which the refactor restores. Name them exactly.
- **(d)** Refactor to Builder. Fix **both** incidents in code — the fixes must be structural (impossible to reintroduce), not "remember not to do that".
- **(e)** `channel` is `"email" | "sms" | "push"` as a raw string. Called it out? What would you change and why — and is that change part of Builder or a separate concern?

---

## Part C — Hardening round (15%)

Answer short. This is the pressure round.

1. **Required-field enforcement at compile time.** In Part A, forgetting `url` blows up at `build()` — *runtime*. A staff engineer asks: can you make "you must set method and url" a **compile error** instead? Sketch the technique and name it. State the cost.
2. **Reuse leak.** A caller does:
   ```java
   RequestSpec.Builder b = RequestSpec.builder().method(GET).url("/a");
   RequestSpec a = b.build();
   RequestSpec c = b.url("/b").build();
   ```
   Is `a` corrupted by the second build? Depends on how you wrote it. Show the one line that decides it.
3. **Director.** When is a separate `Director` worth it, and when is it ceremony? One concrete example each.
4. **Builder vs. Strategy vs. Template Method.** You just studied all three. Object construction has "steps." Template Method also has "steps." One sentence: how do you know which pattern a "has steps" problem actually wants?
5. **Observer boundary.** The transport layer wants to be notified every time a spec finishes its retry cycle (success or exhausted) so it can emit metrics. Which pattern, and does that notification machinery belong **inside** the builder / spec, or **outside** it? Justify the boundary — a wrong answer here bloats the value object. One sentence on how you'd wire it without the immutable spec holding listener state.

---

## Part D — Tradeoffs & when-NOT (10%)

- One case where Builder is **over-engineering** — you'd reject it in code review.
- Builder vs. a language with named/default arguments (Kotlin, Python, Scala) — does Builder still earn its place? When?
- Cost of Builder you must be honest about (there's a real one — allocation/verbosity/duplication). Name it.

---

### Submission
- `A/` package — code for Part A
- `B/` package — refactor for Part B
- Written answers (UML + prose) as PDF or markdown.

Baits are live. Fail-loud where money/correctness depends on it; you know the litmus.
