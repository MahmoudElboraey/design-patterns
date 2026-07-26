# Machine Coding Round — Template Method Pattern in Production

> Format mirrors a real LLD / machine-coding interview (Amazon/Spring-team style):
> one build problem, one production refactor, one hardening round, UML + trade-offs.
> Scope: Template Method + UML + SOLID, with Strategy/Observer cross-questions —
> Template Method is Strategy's inheritance-based twin, and interviewers ALWAYS make you defend the choice between them.
> The tell for this pattern: N classes that are 90% copy-paste with the same step order.
> Budget like a real round: ~90 min total.

---

## Part A — Machine Coding: Settlement File Ingestion Pipeline (the connector problem)

**Q1.** You're building the settlement-file ingestion core of an OTA back office
(this is literally your day job's shape: Amadeus, Air Arabia, Aegean each send
booking settlement files that must be reconciled).

Every provider's ingestion follows the SAME algorithm, in the SAME order:

1. `fetch` — get the file (SFTP for Amadeus, HTTPS download for Air Arabia, email attachment for Aegean)
2. `parse` — provider-specific format (fixed-width text / CSV / XML) → common `List<SettlementRecord>`
3. `validate` — common rules for everyone: no duplicate booking refs, amounts non-negative, currency present (SAME logic for all providers)
4. `persist` — same DB write for everyone
5. `notifyFinance` — OPTIONAL: only some providers require a finance email after ingestion (Amadeus yes, others currently no)

**Functional requirements:**

1. The step ORDER is law. A provider implementation must be physically unable to reorder, skip validation, or forget persistence.
2. New provider next sprint (Sabre) = ONE new class implementing only its fetch + parse. Zero changes to the pipeline, validation, or persistence code.
3. `notifyFinance` must be overridable but default to "do nothing" — name what this kind of method is called in the pattern.
4. The pipeline must produce a final `IngestionReport` (records processed, failures) — assembled by the base, not by providers.

**Non-functional expectations (state how design meets them):**

- A provider author (different team) must not be ABLE to break the common validation — enforce with language features, not code review. Name the keywords doing the enforcement and where each sits.
- Common steps live in exactly one place — duplicating validation across providers is the disease this pattern cures.

**Deliverables:**
- Working Java: abstract base + 2 concrete providers (fake the I/O with prints/hardcoded data)
- A `main` demo: ingest two providers' files, show same step order both times, show Amadeus triggering finance-notify and the other not
- Short paragraph: which method is the template method, which steps are abstract, which are hooks, which are final-and-common — and state the Hollywood Principle in one sentence and point to where your code embodies it

---

## Part B — Production Refactor: the copy-paste triplets

**Q2.** Real production smell — the INVERSE of strategy's if/else pyramid:
instead of one method with branches, three classes that are 90% identical.
Your team has these (abbreviated — imagine each is 200 lines):

```java
public class AmadeusReportJob {
    public void run() {
        log.info("starting amadeus report");
        List<Booking> bookings = repo.findByProvider("AMADEUS");     // same
        List<Booking> valid = bookings.stream()
                .filter(b -> b.getAmount() != null)                  // same
                .filter(b -> b.getCurrency() != null)                // same
                .toList();
        String csv = toAmadeusCsv(valid);            // DIFFERENT — special columns
        ftpClient.upload("/amadeus/reports", csv);   // DIFFERENT — target
        log.info("finished amadeus report");
        metrics.increment("report.amadeus");                         // same shape
    }
}

public class AirarabiaReportJob {
    public void run() {
        log.info("starting airarabia report");
        List<Booking> bookings = repo.findByProvider("AIRARABIA");   // same
        List<Booking> valid = bookings.stream()
                .filter(b -> b.getAmount() != null)                  // same
                .filter(b -> b.getCurrency() != null)                // same
                .toList();
        String json = toAirarabiaJson(valid);        // DIFFERENT
        blobClient.put("airarabia-container", json); // DIFFERENT
        log.info("finished airarabia report");
        metrics.increment("report.airarabia");                       // same shape
    }
}

// AegeanReportJob — same story, third copy
```

Last month a validation bug was fixed in `AmadeusReportJob` only — the other
two shipped the bug to production for three weeks. That is the cost of this smell.

**Tasks:**

a) Refactor to Template Method: abstract base owning the algorithm, subclasses
providing only what genuinely differs. Show the full base class and one subclass.

b) Guaranteed follow-ups — answer before asked:
   - Why must the template method (`run`) be `final`? What EXACTLY goes wrong the day it isn't? Give the one-sentence scenario.
   - Why are the step methods `protected abstract` and not `public`? Who is allowed to call a step?

c) The bug story above ("fixed in one copy, shipped in two") — name the principle
that was violated by the ORIGINAL code, and state precisely how the refactor makes
that class of bug impossible rather than just less likely.

d) Name every SOLID principle the refactor improves, one sentence each — unprompted narration, still drilling the habit.

---

## Part C — Hardening Round: production pitfalls

**Q3.** Your Part A pipeline goes to production. Three incidents. For each:
name the classic pitfall (proper term where one exists), point at the vulnerable
line(s) in YOUR code, show the fix:

a) **The fragile base class strikes.** A teammate adds a new step
`archiveRawFile()` into the middle of the base template method and, while there,
changes `validate` to also normalize currency codes to uppercase. Two of five
provider subclasses break in production — one double-archives (it already
archived inside its own `fetch`), another fails because its parser emitted
lowercase currencies on purpose and downstream code depended on that. Name the
general problem (it has a famous name), explain why inheritance specifically
causes it (what do the subclasses implicitly depend on that no interface
declares?), and give TWO concrete defenses — one documentation-based
(Effective Java has a rule: "design and document for inheritance or else ___"),
one structural.

b) **NullPointerException during construction.** Someone "improves" the base:

```java
public abstract class IngestionPipeline {
    private final IngestionReport report;
    protected IngestionPipeline() {
        this.report = initReport();          // calls overridable method
    }
    protected IngestionReport initReport() { return new IngestionReport("generic"); }
}

public class SabrePipeline extends IngestionPipeline {
    private final String label = "SABRE";
    @Override
    protected IngestionReport initReport() { return new IngestionReport(label); }  // NPE: label is null!
}
```

Explain the EXACT order of operations that makes `label` null at the moment
`initReport()` runs (walk through Java object construction: base ctor vs
subclass field initializers). State the rule that prevents this whole bug class.

c) **Hook explosion.** Two years later the base has 14 hooks
(`beforeFetch`, `afterFetch`, `beforeParse`, `onParseError`, `beforeValidate`...)
because every team needed "just one more extension point." New devs can't tell
what actually runs for a given provider without opening 6 files. Diagnose:
what does a base class with 14 hooks tell you about the abstraction? What is
the structural escape (hint: you built it in the strategy test — steps become
injected objects), and what does that refactor trade away?

**Q4.** The modern variant — template without inheritance:

a) Spring's `JdbcTemplate` is the most famous "template" in Java, yet you never
subclass it — you pass behavior in: `jdbc.query(sql, rs -> mapRow(rs))`.
The varying step arrives as a lambda/callback parameter instead of an overridden
method. Rewrite your Part B solution in this style (base becomes a CONCRETE
class taking the varying steps as constructor/method parameters). What pattern
did Template Method just turn into? What did you gain (name two — think:
testing, runtime flexibility, the Part C-a incident) and what did you lose
(what did the abstract class give you that a parameter list doesn't)?

b) Rule of thumb time: given both tools, when is classic inheritance-based
Template Method still the RIGHT call? Give two concrete conditions
(hint: number of steps that vary together, and who the extenders are).

**Q5.** Real post-mortems — big companies shipped inheritance-based templates
at massive scale, then walked them back. Analyze through the pattern lens:

a) **Google — Android's `AsyncTask`, deprecated in Android 11.** The textbook
Template Method: you subclass, override `doInBackground()` (abstract step),
optionally `onPreExecute()`/`onProgressUpdate()`/`onPostExecute()` (hooks), the
final framework-owned lifecycle calls them in order. Used in millions of apps
for a decade. Deprecated because in production it caused: Context/memory leaks
(the subclass instance — often an inner class holding an Activity reference —
outlives the screen it belongs to), silently swallowed exceptions from
`doInBackground`, behavior that changed across OS versions (serial vs parallel
execution flipped), and crashes on configuration changes. Google's own verdict:
"does not provide much utility over using Executors directly."

   Answer:
   - Map it: name the template method, the abstract step, the hooks.
   - Which of these failures are INHERENT to the inheritance design vs just bad
     implementation? (Hint: the leak comes from WHERE your step code lives —
     inside a subclass INSTANCE with a lifecycle you don't control. Would the
     JdbcTemplate/callback style from Q4a have the same leak shape?)
   - Your Part A pipeline: what long-lived reference could a provider subclass
     accidentally capture, and what rule do you write in code review to prevent it?

b) **Facebook/React — class components → hooks (composition).** React's
component model was inheritance-shaped: extend `React.Component`, override
lifecycle template slots (`componentDidMount`, `componentDidUpdate`,
`componentWillUnmount`). At Facebook scale it produced: logic for ONE feature
smeared across THREE lifecycle methods, the same subscribe/unsubscribe code
copy-pasted into hundreds of components (inheritance gave no good way to SHARE
step implementations between siblings), and deep wrapper hierarchies as
workarounds. React's official guidance became "composition over inheritance";
the docs now have no class components, and reuse ships as composable hooks.

   Answer:
   - The core lesson: Template Method lets a subclass FILL slots but gives
     siblings no way to SHARE slot implementations. Where in your Part A design
     would two providers wanting the same fetch logic expose this same weakness,
     and what's the fix (you built it in Q4a)?
   - Both Google and Facebook retreated from inheritance templates toward
     composition. Does that mean Template Method is dead? Defend "no" with the
     one context where it remains the best tool (hint: who owned AsyncTask's
     template vs who owns YOUR pipeline's — stability of the algorithm matters).

---

## Part D — UML + Trade-offs

**Q6.** Full UML class diagram of your Part A design (hand-drawn like before). Graded hard on:

- Base ← subclasses: INHERITANCE arrow — SOLID line + hollow triangle pointing at the base (do not confuse with realization's dashed line; you know the difference, show it)
- Abstract class and abstract methods marked (italics or `{abstract}` tag); the template method marked `final`
- Hook vs abstract step visually distinguishable (stereotype or note)
- Any collaborator the base owns (repo, metrics) — association with correct diamond + defended fill + multiplicity
- SOLID narration written next to the diagram, unprompted

**Q7.** Judgment questions, 2–3 sentences each:

a) **Template Method vs Strategy** — same question as the strategy test but
argue the OTHER direction now: give one scenario where Template Method genuinely
beats Strategy, and name the cost you accept for it.
b) **Template Method vs Observer:** the `notifyFinance` hook could instead be
an event published at pipeline end (your Part B observer refactor style).
When does a hook stop being enough and demand events? Give the concrete signal.
c) Java's own library has famous inheritance misuse: `Stack extends Vector` and
`Properties extends Hashtable` — both let callers do
`stack.add(2, element)` / `properties.put(nonString, x)`, corrupting the
abstraction. What rule of inheritance did the JDK designers break, and what
should they have used instead? Why is Template Method NOT guilty of this when
done right (who calls whom)?
d) A teammate says "make every service extend `AbstractBaseService` with logging,
metrics and error handling — DRY!" Push back: what's the difference between
sharing an ALGORITHM (legit Template Method) and sharing UTILITIES through a
base class, and what pain arrives at class #30?

**Q8.** The evolution ladder (YAGNI judgment). Report generation, three stages:

```java
// Stage 1 — one concrete class, no variation exists yet
public class ReportJob {
    public void run() { fetch(); validate(); export(); }
}

// Stage 2 — Template Method: variation arrived, order is law
public abstract class ReportJob {
    public final void run() { List<Row> d = fetch(); validate(d); export(d); }
    protected abstract List<Row> fetch();
    protected abstract void export(List<Row> d);
    private void validate(List<Row> d) { ... }         // common, untouchable
}

// Stage 3 — steps as injected strategies (pipeline object, composition)
public class ReportJob {
    private final Fetcher fetcher;
    private final Exporter exporter;
    public void run() { List<Row> d = fetcher.fetch(); validate(d); exporter.export(d); }
}
```

For each requirement, pick the CHEAPEST stage that satisfies it, and say what
NEW capability each stage buys over the previous:

a) "One report, one format, internal tool, no second variant on any roadmap."
b) "Three providers, each varies fetch+export, step order and validation must be
untamperable, provider classes written by junior devs."
c) "Ops wants to mix and match at RUNTIME: fetch from Amadeus but export in the
new JSON format for an A/B test; QA wants to inject a fake fetcher in tests
without any subclassing."
d) A teammate jumps to Stage 3 for requirement (b) "because composition over
inheritance, always." Push back — what does Stage 2 enforce here that Stage 3
cannot (think: who can construct a `ReportJob` with a WRONG combination), and
name the principle he's turning into a slogan.

---

## Part E — Pattern-or-Not Gauntlet — /10

### Q9. When Template Method fits, when it doesn't, and when it burns you

**A) Five scenarios — verdict (Template Method / not Template Method / different tool) + one-line justification each. Three contain traps.**

1. Three report jobs share an identical skeleton (fetch → validate → transform → export → notify); only fetch and export vary per provider; the skeleton must be untamperable.
2. Two batch jobs share the same five steps, but one runs them in a DIFFERENT ORDER (validate after transform).
3. A request handler must swap ONE step per request at runtime (different export format chosen by a query param).
4. Four unrelated classes all need the same date-parsing helpers — someone proposes an `AbstractDateAwareBase` they all extend.
5. You publish a framework SPI: third parties implement lifecycle steps, you must guarantee your invariants run before/after their code in a fixed order.

The traps to catch: (2) breaks the pattern's core promise — the skeleton IS the fixed order; varying order means there is no common template, use a pipeline/composed steps instead. (3) is a binding-time mismatch — inheritance binds the step at class-definition time, one subclass per combination; a runtime-chosen step is a Strategy/callback slot. (4) is inheritance-for-code-reuse — no is-a relationship, no shared skeleton, just wanted methods; that's a static utility or composed helper, and the base class will grow into a god-parent everyone fears touching.

**B) Misuse post-mortem — Template Method applied where it didn't belong (two real exhibits):**

*Exhibit 1 — the god base class.* A team created `AbstractBaseService` and mandated every service extend it. It started with 3 steps; two years later: 14 hooks, 6 boolean flags that reroute the skeleton, and subclasses overriding steps to no-ops to escape behavior they never wanted. A change to the base's retry step broke 9 services in one deploy — the **fragile base class problem** at maximum blast radius, because the base was shared by services with NOTHING in common except being services.

*Exhibit 2 — the JDK's own scar.* `java.util.Properties extends Hashtable` — inheritance chosen where composition belonged. Consequence shipped in every Java version since 1.0: calling inherited `put("key", 42)` bypasses the String-only invariant, and a later `getProperty("key")` returns null while the value silently sits in the table. Real production config corruption, documented for decades, unfixable without breaking compatibility — the JDK's own docs tell you not to use the inherited methods.

1. For Exhibit 1: name the two smells (fragile base class; flag-driven skeleton = the template no longer has ONE fixed algorithm) and state the design-time signal missed — Template Method is for ONE family sharing ONE genuine algorithm skeleton, not for "all services" sharing infrastructure. Infrastructure cross-cuts → composition, interceptors/decorators, not inheritance.
2. For Exhibit 2: state the rule it proves — inheritance exposes the ENTIRE parent API as your contract forever; if you only wanted the storage, you wanted a field, not a parent. One sentence: why `final` template methods + `protected abstract` steps (your Part A design) would have prevented the `put` bypass structurally.
3. Prescribe: for Exhibit 1, which of the 14 hooks survive as a real Template Method (the ones forming one true skeleton for one true family) and where the rest go (interceptors, composed collaborators, deleted). Name the principle that says prefer the smaller contract (composition over inheritance / ISP).

---

*Working code goes wherever you put this pattern's project, diagrams on paper.
Grading /10 per part, honest FAANG machine-coding bar: correctness,
extensibility proven not claimed, narration of trade-offs unprompted.*
