# Singleton Pattern — FAANG Interview Test

**Time budget:** 90 min. Whiteboard/IDE. Talk while you build.
**You already know:** SOLID, Observer, Strategy, Template Method, Builder.
**Grading axis:** correctness of the one-instance guarantee > thread-safety > attack-resistance (reflection/serialize/clone) > **knowing when Singleton is the wrong answer** > tradeoff talk.

Warning: on this pattern the highest-scoring candidates spend as much time arguing *against* Singleton as implementing it. There are baits. The biggest bait is the whole premise — watch for it.

---

## Part A — Design from scratch (40%)

### Scenario

You own the platform-wide **feature-flag registry** for a 300-service backend. Every request path reads flags thousands of times/sec. Rules the org gave you:

1. There must be **exactly one** flag registry per running service process. Two copies = two services disagree on whether a flag is on = split-brain incident.
2. It is read on **every request thread** concurrently, hot path — reads must not serialize on a lock.
3. It is **lazy** — some processes (batch jobs) never read a flag; don't pay init cost for them.
4. Init needs a **runtime source** (a config URL + an env name) that isn't known at class-load time.
5. **Tests must be able to swap it** for an in-memory fake with known flags. A test that flips flag X must not bleed into the next test.
6. Nobody must be able to quietly create a second one — not by `new`, not by reflection, not by deserializing one off the wire.

Notice rules 1–4 pull toward "global static single instance" and rules 5–6 pull the other way. That tension **is the question.**

### Deliverables
- **(a)** UML. Show the instance-holding, the access point, and how a test double gets in. Mark thread-safety points.
- **(b)** Code the registry. Thread-safe, lazy, correct one-instance guarantee. Show the memory-model detail that makes lazy+concurrent safe — don't hand-wave it.
- **(c)** Reconcile rules 4 and 6: init needs runtime args (smells like Builder) but constructor must stay locked down. How? Show it.
- **(d)** Reconcile rules 1 and 5: exactly-one in prod, swappable in tests, no cross-test bleed. Show the mechanism. Name the principle you're leaning on (hint: you know it — it's a SOLID letter).

### Questions baked in (answer inline)
- Exactly which line makes the double-checked version correct, and what silently breaks if it's missing? Describe the two-thread interleaving that corrupts a caller.
- Is `getInstance()` called from 200 files across the codebase a good design? Answer honestly. If not, what's the smell called and what's the fix?
- Your lazy holder is thread-safe *without* you writing any `synchronized`. Why? What guarantees it?

---

## Part B — Production refactor (35%)

Real code. Shipped. Caused a split-brain incident and a flaky test suite.

```java
public class MetricsClient {
    private static MetricsClient instance;

    public String endpoint;
    public List<String> buffer = new ArrayList<>();

    private MetricsClient() {
        this.endpoint = System.getenv("METRICS_URL");
    }

    public static MetricsClient getInstance() {
        if (instance == null) {
            instance = new MetricsClient();   // <-- incident #1
        }
        return instance;
    }

    public void record(String metric) {
        buffer.add(metric);                    // <-- incident #2
    }
}
```

Usage, sprinkled across ~180 files:
```java
MetricsClient.getInstance().record("checkout.success");
```

A test:
```java
@Test void testA() {
    MetricsClient.getInstance().record("a");   // buffer now ["a"]
}
@Test void testB() {
    // buffer still ["a"] from testA — leaked. flaky assert count.
    assertEquals(1, MetricsClient.getInstance().buffer.size()); // sometimes 2
}
```

### Deliverables
- **(a)** Name every distinct smell (there are at least 5 — one is not a thread bug, it's an architecture bug). One line each.
- **(b)** UML of the refactor.
- **(c)** SOLID writeup: which principles the original violates (there are two big ones), which the refactor restores. Name them exactly, say where.
- **(d)** Refactor. Fix **incident #1** (race → two clients) and **incident #2** (shared mutable buffer + test bleed) **structurally**. The test-bleed fix in particular — argue whether the real fix is "reset the singleton between tests" or "stop making it a global singleton at all." Pick a side, defend it.
- **(e)** `getInstance()` is hard-wired into 180 files. Sketch the migration path to whatever you refactored to, without a 180-file big-bang PR.

---

## Part C — Hardening round (15%)

Short answers. Pressure round.

1. **Reflection attack.** `Constructor.setAccessible(true)` → `newInstance()` builds a second copy through your private constructor. One code change that makes the constructor itself refuse. Name the technique that sidesteps the whole class of attack.
2. **Serialization attack.** Serialize the singleton, deserialize → new instance. Which single method restores the guarantee, and what does it return?
3. **Clone attack.** Someone calls `clone()`. Two lines to stop it.
4. **ClassLoader.** "One instance per JVM" is a lie under what condition? One sentence.
5. **Enum singleton.** Bloch says one-element enum is the best singleton. What three attacks does it kill for free — and the one real limitation that makes it unusable for Part A. (Tie it back to Part A rule 4.)
6. **Cross-pattern boundary.** Your event bus is a Singleton and everyone does `EventBus.getInstance().subscribe(this)` (Observer). Name the two patterns colliding and why that specific combo is a testability landmine. How do you keep the Observer wiring but kill the global access?
7. **Singleton vs. Builder vs. Template Method.** Builder makes *many* configured objects; Singleton makes *one*; both control construction. One sentence: what question tells you which the problem wants?

---

## Part D — Tradeoffs & when-NOT (10%)

- The honest one: name three concrete costs Singleton imposes (hidden dependency, global mutable state, test coupling — expand each to one line of *consequence*, not just the label).
- "Singleton is an anti-pattern." Steelman it, then rebut it. When is the naive `getInstance()` actually fine, and when is a DI-container-scoped singleton (one instance, but injected) strictly better?
- Registry vs. Singleton — a registry of many named single-instances. When does that beat a pile of individual singletons?
- Given a DI framework (Spring `@Singleton`/`@ApplicationScoped`), does the hand-rolled `getInstance()` Singleton ever still earn its place? Give one case where yes.

---

### Submission
- `A/` package — feature-flag registry
- `B/` package — MetricsClient refactor
- Written answers (UML + prose) as PDF or markdown.

Baits live. The premise itself is one of them — a top answer knows when to refuse the pattern. Fail-loud where correctness depends on it; you know the litmus.
