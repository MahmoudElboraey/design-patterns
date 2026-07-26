# Visitor Pattern — Machine Coding Interview Test

**Budget: ~90 minutes total. Grading: /10 per part, FAANG bar, honest.**
**Patterns you know: SOLID, UML, Observer, Strategy, Template Method, Memento. This test centers on VISITOR — other patterns appear only in Part D comparisons.**

---

## Part A — Machine Coding (~40 min) — /10

### Q1. Build: Itinerary Operations Engine

You work on an OTA platform (you literally do — this is your flight-search service's sibling problem). A confirmed booking is an **itinerary**: an ordered list of heterogeneous line items. Today there are four item types:

- **FlightSegmentItem** — carrier, origin, destination, farePrice, fuelSurcharge
- **HotelStayItem** — hotelName, city, nights, nightlyRate
- **BaggageItem** — weightKg, price, linked segment reference
- **InsuranceItem** — provider, coveredAmount, premium

The finance team needs **operations over an itinerary** — and here is the catch that makes this a Visitor problem: the operations multiply faster than the item types. Currently required:

1. **InvoiceRenderer** — produces a plain-text invoice. Each item type renders differently (flights show route + fare + surcharge separately; hotels show nights × rate; baggage shows weight; insurance shows provider + premium). Ends with a grand total.
2. **TaxCalculator** — VAT rules differ per item type AND per country code passed at construction: flights are tax-exempt for international routes, hotels taxed at the destination-country rate, baggage follows the flight it's attached to, insurance uses a flat insurance-premium-tax rate.
3. **LoyaltyPointsCalculator** — flights earn distance-class points, hotels earn per-night points, baggage and insurance earn nothing (but must still be visited — see grading trap below).

**Functional requirements:**
- The item classes must contain **zero operation logic**. No `renderInvoice()` on `FlightSegmentItem`. Justify why in one paragraph (hint: which SOLID principle dies when the domain model imports an invoice formatter?).
- Double dispatch must be real: `accept(Visitor)` on the element, overloaded `visit(ConcreteType)` on the visitor. You will be asked to explain, line by line, which of the two dispatches is virtual (runtime) and which is overload resolution (compile time).
- An itinerary is traversed once; a visitor may accumulate state across items (running total, points sum).

**Non-functional requirements — ADDRESS EXPLICITLY in code or comments-free design (narrate in your notes):**
- **Thread safety:** `TaxCalculator` instances are constructed per-request with a country code (configuration, final) but accumulate a running total (computation, mutable). State the rule you learned from the concurrency doc: which visitors are shareable across threads, which must be confined per-request, and why a stateless `InvoiceRenderer` that returns a String per item is the safest shape.
- **Failure isolation:** one item with corrupt data (negative nights) must not kill the whole invoice run. Where does the try/catch go — inside the traversal loop or inside each visit method? Defend your choice (you solved the identical question in Observer's notify loop).
- **Scale:** itineraries can hold 10,000 items (group bookings). Your traversal must be a flat iteration, not per-item recursion into sub-visitors.

**Extensibility trap (next sprint, zero core changes):**
- Sprint N+1: finance asks for a **RefundEstimator** operation (per-type refund rules). Requirement: **zero edits** to any item class, the itinerary class, or existing visitors. Your demo must prove this by adding it as if it arrived late.
- Sprint N+2 (answer in prose, don't build): product adds a **CarRentalItem**. Count exactly which files you must now touch and what breaks at compile time. Name the classic problem this exposes (it has a proper CS name — the **expression problem**) and state Visitor's side of the trade: which axis of change is cheap, which is expensive, and why you still chose Visitor given the requirements above.

**Required `main` demo — proves, not claims:**
1. Build one itinerary with all four item types.
2. Run all three operations, print results.
3. Add `RefundEstimator` "late" and run it — zero prior-file edits.
4. Include one corrupt item; show the invoice completes for the other items.
5. Run `TaxCalculator` for two different country codes on the same itinerary — two visitor instances, no shared mutable state.

**Grading traps I will check:**
- `visit(BaggageItem)` and `visit(InsuranceItem)` missing from `LoyaltyPointsCalculator` because "they earn nothing" — if your visitor interface lets an implementor silently skip a type, that's the silent-failure bug from Part C shipped on day one. Decide: abstract methods forcing all overloads, or a default no-op — and defend the choice against "new item type added, seven visitors silently ignore it."
- `accept` implemented once in a base `BookingItem` class as `visitor.visit(this)` — explain why that compiles to the WRONG overload (static type of `this` in the base class) and why every concrete class must implement `accept` itself. This is the single most-failed visitor interview question.
- Visitor doing its own traversal (visitor iterating the itinerary internally) vs the itinerary/client driving traversal — pick one, state who owns iteration order.

---

## Part B — Production Refactor (~20 min) — /10

### Q2. The instanceof pyramid you have definitely seen

This is the "before" — realistic code in the style of an OTA reporting service. Refactor it to Visitor.

```java
public class BookingReportService {

    public String renderHtml(List<BookingItem> items) {
        StringBuilder sb = new StringBuilder("<table>");
        for (BookingItem item : items) {
            if (item instanceof FlightSegmentItem) {
                FlightSegmentItem f = (FlightSegmentItem) item;
                sb.append("<tr><td>FLIGHT</td><td>").append(f.getOrigin())
                  .append("-").append(f.getDestination())
                  .append("</td><td>").append(f.getFarePrice() + f.getFuelSurcharge())
                  .append("</td></tr>");
            } else if (item instanceof HotelStayItem) {
                HotelStayItem h = (HotelStayItem) item;
                sb.append("<tr><td>HOTEL</td><td>").append(h.getHotelName())
                  .append("</td><td>").append(h.getNights() * h.getNightlyRate())
                  .append("</td></tr>");
            } else if (item instanceof BaggageItem) {
                BaggageItem b = (BaggageItem) item;
                sb.append("<tr><td>BAG</td><td>").append(b.getWeightKg())
                  .append("kg</td><td>").append(b.getPrice()).append("</td></tr>");
            }
            // InsuranceItem: nobody added it here. Ships silently missing from every report.
        }
        return sb.append("</table>").toString();
    }

    public double computeTotal(List<BookingItem> items) {
        double total = 0;
        for (BookingItem item : items) {
            if (item instanceof FlightSegmentItem) {
                FlightSegmentItem f = (FlightSegmentItem) item;
                total += f.getFarePrice() + f.getFuelSurcharge();
            } else if (item instanceof HotelStayItem) {
                HotelStayItem h = (HotelStayItem) item;
                total += h.getNights() * h.getNightlyRate();
            } else if (item instanceof BaggageItem) {
                total += ((BaggageItem) item).getPrice();
            } else if (item instanceof InsuranceItem) {
                total += ((InsuranceItem) item).getPremium();
            }
        }
        return total;
    }

    public List<String> validate(List<BookingItem> items) {
        List<String> errors = new ArrayList<>();
        for (BookingItem item : items) {
            if (item instanceof FlightSegmentItem) {
                FlightSegmentItem f = (FlightSegmentItem) item;
                if (f.getFarePrice() < 0) errors.add("negative fare: " + f.getOrigin());
            } else if (item instanceof HotelStayItem) {
                HotelStayItem h = (HotelStayItem) item;
                if (h.getNights() <= 0) errors.add("bad nights: " + h.getHotelName());
            }
            // Baggage and Insurance never validated. Two prod incidents last quarter.
        }
        return errors;
    }
}
```

**Your job:**
1. Name every smell before refactoring: the **type-switch duplication** (same instanceof ladder ×3), the **shotgun surgery** cost (new item type = edit N methods), and the killer — the **inconsistent ladders**: `renderHtml` is missing Insurance, `validate` is missing two types, and the compiler said nothing. State precisely why the compiler *can't* help here and how Visitor (abstract `visit` overloads) converts these silent gaps into compile errors.
2. Refactor to Visitor. `BookingReportService` shrinks to thin methods that instantiate a visitor and traverse.
3. **Guaranteed follow-ups — answer before being asked:**
   - "Why not just put `renderHtml()`, `computeTotal()`, `validate()` as methods ON the item classes?" — answer with SRP + dependency direction (domain entities importing HTML/reporting concerns) + the operations-multiply-faster-than-types argument.
   - "Why not a `switch` on a `getType()` enum?" — same silent-gap problem, plus stringly/enum-typed dispatch duplicates what the JVM already gives you.
   - "You're on Java 21 — why not sealed interface + pattern-matching switch?" — legitimate! Give the honest answer: sealed + exhaustive `switch` ALSO turns missing branches into compile errors. Then state what Visitor still buys here and what your call would be for THIS codebase (Quarkus, Java 17+ — check what your day job actually runs).
4. **Unprompted SOLID narration** — one paragraph, walk OCP (new operation without touching items), SRP (reporting out of domain), DIP (service depends on visitor abstraction), and which principle the ORIGINAL code violated worst.

---

## Part C — Hardening Round (~15 min) — /10

### Q3. Incident reports — name the pitfall, point at YOUR lines, fix it

**Incident 1 — The €0 tax report.**
Ops runs nightly tax reports. A single `TaxCalculator` visitor bean was registered `@ApplicationScoped` in Quarkus and injected into a REST resource handling concurrent requests. Reports started showing totals from OTHER customers' itineraries mixed in; some showed double. No exceptions.
- Name the bug class precisely (you wrote a whole readings doc on this). Which of the three race shapes is `total += item.price()`?
- Point at the exact lines in YOUR Part A `TaxCalculator` that are vulnerable if the instance is shared.
- Fix it TWO ways and rank them: (a) per-request instantiation — configuration vs computation state rule, which field is which in your visitor; (b) the "make it stateless again" transformation — `visit` returns the item's contribution, caller sums. State why (b) is the deeper fix and what it costs (visitor interface returns a value → affects ALL visitors).

**Incident 2 — The invisible item type.**
A team added `CarRentalItem`. To "avoid breaking 9 existing visitors," they gave the visitor interface a default method: `default void visit(CarRentalItem i) {}`. Six months later finance discovers car rentals were never invoiced, never taxed, never validated — for six months. Zero exceptions, zero logs.
- Name this pitfall (silent-skip / default-adapter trap). Explain the exact trade: default methods bought source compatibility and paid with silent data loss.
- Give the three defensible policies and when each is right: (1) abstract methods — all visitors break loudly at compile time, fix them all now; (2) default that THROWS (`UnsupportedOperationException`) — runtime-loud, compile-quiet; (3) default that logs-and-counts with a metric alert. Which would you ship in a finance path?
- Connect to Part A: which policy did you choose for LoyaltyPointsCalculator's "earns nothing" types, and does this incident change your answer?

**Incident 3 — The dispatch that wasn't.**
A junior "simplified" the code: deleted `accept`, and wrote `visitor.visit(item)` directly in the loop, where `item`'s static type is `BookingItem`. It doesn't compile — so they "fixed" it by adding `visit(BookingItem item)` to the visitor. Now every item hits the fallback method; invoices show nothing but item counts.
- Explain the mechanism exactly: Java overload resolution happens at COMPILE time on the STATIC type; only the receiver of a virtual call dispatches on runtime type. `accept` exists to flip the item into the receiver position once (`item.accept(v)` — virtual dispatch #1), so that inside `accept`, `this` has a concrete static type and `v.visit(this)` picks the right overload (compile-time resolution #2). That pair is **double dispatch**.
- Why does Java need this dance at all? Name what the language lacks (multiple dispatch / multimethods) and one language that has it.

### Q4. The modern alternative — sealed types + pattern matching (deep dive)

Java 21 finalized pattern matching for `switch` ([JEP 441](https://openjdk.org/jeps/441)). The JEP's own motivation calls out that expressing ad-hoc polymorphic calculations without it requires "the cumbersome visitor pattern."

```java
sealed interface BookingItem permits FlightSegmentItem, HotelStayItem, BaggageItem, InsuranceItem {}

double contribution(BookingItem item) {
    return switch (item) {
        case FlightSegmentItem f -> f.farePrice() + f.fuelSurcharge();
        case HotelStayItem h    -> h.nights() * h.nightlyRate();
        case BaggageItem b      -> b.price();
        case InsuranceItem i    -> i.premium();
    }; // sealed ⇒ compiler enforces exhaustiveness, no default needed
}
```

1. Map the correspondences: sealed `permits` list ↔ the fixed element hierarchy; exhaustive switch ↔ abstract visit overloads; adding a permitted subtype breaks every switch ↔ adding an element type breaks every visitor. Both sit on the SAME side of the expression problem — cheap operations, expensive new types.
2. What the switch version deletes: `accept` methods in the domain, the visitor interface, double dispatch entirely. What it loses: (a) visitors are OBJECTS — they carry constructor config (country code), accumulate state, get injected/decorated/tested as units; a switch is just code in a method; (b) a visitor interface is a stable SPI you can publish across module/team boundaries — third parties implement it without your permission; a sealed switch requires seeing all types and can't be extended by outsiders; (c) traversal + operation can be packaged together (framework calls YOUR visitor — Hollywood principle, same inversion as Template Method).
3. Verdict question: your day-job service is Quarkus on Java 17+. New heterogeneous-type dispatch need appears tomorrow, ~4 types, 2 operations, all in one module. Visitor or sealed+switch? Commit to one and defend in 4 sentences.

### Q5. Real company post-mortems — analysis questions

**Case 1 — The JDK's own visitors are versioned like an apology ([`ElementVisitor`](https://docs.oracle.com/en/java/javase/17/docs/api/java.compiler/javax/lang/model/element/ElementVisitor.html), [`SimpleElementVisitor6`](https://docs.oracle.com/javase/7/docs/api/javax/lang/model/util/SimpleElementVisitor6.html) → 7 → 8 → 9 → 14).**
The annotation-processing API (`javax.lang.model`) is a Visitor over program elements. The JDK team then had to keep adding language constructs (modules in 9, records in 14+) — i.e., new ELEMENT types — the axis Visitor makes expensive. Their published survival kit: (a) the interface docs warn methods may be ADDED to `ElementVisitor` in future releases; (b) a `visitUnknown` escape hatch for constructs a visitor predates; (c) an entire family of versioned abstract classes — `SimpleElementVisitor6`, `...7`, `...8`, `...9`, `...14` — where each version routes constructs it doesn't know to `visitUnknown` instead of a compile error, and processors are told to extend these instead of implementing the interface raw.
- Questions: (1) This is the expression problem hitting a system that CANNOT do a big-bang recompile of the world (annotation processors are third-party binaries) — explain why "abstract methods, break everyone loudly" was not available to the JDK, unlike your Part B refactor. (2) `visitUnknown` is exactly Incident 2's silent-skip default — why is it the RIGHT call here and the WRONG call in your invoicing system? (State the difference in failure cost: skipped language construct in a lint tool vs untaxed money.) (3) What does the existence of five versioned base classes tell you about choosing Visitor for a hierarchy you know will grow?

**Case 2 — The JDK builds a language feature to retire the pattern ([JEP 441](https://openjdk.org/jeps/441), Java 21).**
Pattern matching for `switch` + sealed classes ([JEP 409](https://openjdk.org/jeps/409)) were explicitly motivated in part by replacing "the cumbersome visitor pattern" for ad-hoc polymorphism, with sealed hierarchies giving the compiler the exhaustiveness knowledge that abstract visit methods used to encode ([InfoQ coverage](https://www.infoq.com/news/2023/07/tranforming-java-pattern/)).
- Questions: (1) "Visitor is dead in modern Java" — attack AND defend this claim in 3 sentences each (your Q4.2 list is the defense material). (2) Scala had pattern matching from birth and its community largely never used Visitor — what does that tell you about how much of this pattern is design wisdom vs language workaround? (3) Your OpenRewrite-style codemod tools now ship recipes auto-converting visitor code to pattern-matching switches — when would you REFUSE that migration on a real codebase?

**Case 3 — ASM: the visitor that breaks the world every September.**
ASM, the bytecode library under Gradle, the Android Gradle Plugin, Mockito, Spring, and half the JVM ecosystem, is built as a streaming Visitor (`ClassVisitor`/`MethodVisitor` — chosen over a tree API for speed and low memory). Every new JDK release bumps the class-file major version, and every build tool pinned to an older ASM dies with `IllegalArgumentException: Unsupported class file major version 61` — documented across [Gradle forums](https://discuss.gradle.org/t/java-lang-illegalargumentexception-unsupported-class-file-major-version-61/42081), [Maven's dependency plugin (MDEP-613)](https://issues.apache.org/jira/browse/MDEP-613), and [Google's issue tracker for AGP/Jetifier](https://issuetracker.google.com/issues/159151549/dupes). ASM refuses to parse class files newer than the visitor API version you constructed it with (`new ClassVisitor(Opcodes.ASM9, ...)`).
- Questions: (1) ASM's hard version check is the OPPOSITE policy of `visitUnknown` — fail loudly on unknown input rather than silently skip attributes it might not understand. For a bytecode REWRITER, why is silent-skip catastrophically worse than crashing the build? (Hint: what happens if you rewrite a class file while ignoring an attribute you didn't know exists?) (2) The streaming-visitor design (no tree in memory) is why ASM is fast enough to sit inside every build — connect this to your Part A scale requirement: what does visitor-as-streaming buy over build-the-full-object-graph-then-walk? (3) The ecosystem pain is real and annual — was ASM's strictness a design mistake? Give the trade-off verdict.

**Case 4 — ANTLR ships BOTH: listener and visitor, and makes you choose.**
The ANTLR4 parser generator emits two traversal APIs for every grammar: a listener (framework walks the tree, fires enter/exit events, no return values) and a visitor (YOU drive traversal, visit methods return values, you can skip subtrees).
- Question: this is Visitor vs Observer as a product decision. State the two axes that decide it: who owns traversal control, and whether operations compute values or react to events. Give one concrete task where listener wins (collecting all string literals) and one where visitor wins (evaluating an expression tree — need return values and short-circuit).

---

## Part D — UML + Trade-offs (~15 min) — /10

### Q6. Hand-drawn UML — graded hard

Draw the complete class diagram of your Part A design. Required, and I will check each:

1. **Two hierarchies, cross-linked**: `ItineraryVisitor` interface with its concrete visitors; `BookingItem` interface with its concrete items. Realization (implements) = **dashed line, hollow triangle** — you have missed the dashed part before; a solid line with hollow triangle means extends, and mixing them is an automatic point off.
2. **The double-dispatch signatures visible**: `accept(v: ItineraryVisitor)` on the element side, one `visit(f: FlightSegmentItem)` etc. overload per concrete type on the visitor side. A `visit(item: BookingItem)` single method in the diagram = you drew the Incident-3 bug.
3. **Dependency arrows** (dashed, open arrowhead): each concrete visitor **depends on** every concrete element type (its overload parameters). This is the visual signature of Visitor's trade — draw them and then answer: what do these N×M dashed arrows tell you about coupling, and which change multiplies them?
4. **Multiplicity everywhere** — your recurring gap, zero tolerance this time: Itinerary `1 → 0..*` BookingItem (0 matters: empty itinerary legal), client → visitor instances, and mark that a visitor instance is used by `0..*` traversals only if stateless — annotate the stateful ones `1 per traversal`.
5. **Who owns traversal**: show the `Itinerary` (or client) with the iteration responsibility — a note is acceptable, absence is not.
6. One **«create»** dependency if your service constructs visitors per request (you learned this arrow in Memento).

### Q7. Cross-pattern judgment

1. **Visitor vs Strategy**: both inject behavior objects. State the structural difference in one sentence (Strategy: ONE algorithm slot chosen per context; Visitor: a FAMILY of type-dispatched methods spanning a whole hierarchy) and give the smell that tells you a Strategy should become a Visitor.
2. **Visitor vs Template Method**: ANTLR's listener walk is Hollywood-principle (framework calls you) — so is a visitor being driven by an external traversal. What did Template Method fix by inheritance that Visitor fixes by composition, and why does Visitor survive the fragile-base-class problem that bit Template Method?
3. **Visitor vs Observer**: both have "one call fans out to many methods." One sentence on why they are unrelated despite the surface similarity (dispatch on TYPE of one element vs broadcast to MANY subscribers).
4. **Visitor + Memento**: your document editor snapshots state; a visitor could EXPORT state. Why must a serializing visitor not become a back door that breaks Memento's encapsulation rule?
5. **When to refuse Visitor entirely** — give all four and one sentence each: (a) one operation only, stable forever → plain polymorphic method on the elements; (b) element types change often, operations don't → you're on the wrong side of the expression problem, use interpreter-style methods or sealed+switch and accept the operation edits; (c) hierarchy isn't yours to modify (can't add `accept`) → no Visitor without intrusion; name the workarounds' cost (reflection, `instanceof` ladder comes back); (d) Java 21 + small closed hierarchy in one module → sealed + pattern matching, pattern would be ceremony. Name the principle the ceremony violates.
6. Eclipse JDT's `ASTVisitor` has ~90 `visit` overloads; Checkstyle, PMD, SpotBugs, Error Prone are all visitors over ASTs. Static-analysis tooling is Visitor's unkillable home turf — state the TWO properties of that domain that make it so (element hierarchy frozen by the language spec; operations = every lint rule anyone ever writes, unbounded).

### Q8. YAGNI evolution ladder

Three stages of the same capability:

- **Stage 1**: `double total()` polymorphic method on each `BookingItem` subclass. One operation, four small methods, zero infrastructure.
- **Stage 2**: sealed `BookingItem` + pattern-matching `switch` per operation, operations live in the service layer. Compile-checked exhaustiveness, no `accept`, no visitor interface.
- **Stage 3**: full Visitor — interface, double dispatch, visitors as injectable/configurable/stateful objects, publishable SPI.

For each requirement, pick the CHEAPEST sufficient stage and one-line why:
1. "We need the total price of an itinerary." →
2. "Finance, tax, loyalty, and refunds each need per-type logic; two more operations expected next quarter; all code in our module; Java 21." →
3. "External plugin teams must add their own operations over our booking model without us recompiling; operations need per-tenant config and DI." →
4. "We need totals now, and marketing 'might want' a points system someday." →
5. Name the principle violated by jumping to Stage 3 for requirement 1, and the cost that appears in the diff (count the ceremony files). Then the reverse: name what it costs to be at Stage 1 when requirement 3 lands, and why that migration is still usually cheaper than carrying Stage 3 for years unused.

---

## Part E — Pattern-or-Not Gauntlet — /10

### Q9. When Visitor fits, when it doesn't, and when it burns you

**A) Five scenarios — verdict (Visitor / not Visitor / different tool) + one-line justification each. Three contain traps.**

1. Lint rules over a language AST: the node hierarchy is frozen by the language spec; new rules arrive weekly, forever.
2. An e-commerce product-type hierarchy gaining a new product type roughly every month; exactly two operations exist (price, render) and no third is planned.
3. One operation (render), stable for years, over four item types.
4. External plugin teams must define their own operations over your closed booking model, without you recompiling.
5. Four types, three operations, all inside one module, Java 21 codebase.

The traps to catch: (2) is the wrong side of the expression problem — types are the growing axis, so every new type means editing every visitor; put the two stable operations ON the types (polymorphism) and adding a type becomes one new self-contained class. (3) is one stable operation — a plain polymorphic method; a visitor interface for it is pure ceremony. (5) is sealed + pattern-matching switch territory — same compile-time exhaustiveness, none of the `accept` plumbing.

**B) Misuse post-mortem — Visitor applied where it didn't belong:**

A platform team, fresh from a design-patterns course, put a Visitor over the booking-item hierarchy "for clean separation of concerns." The product roadmap then did what roadmaps do: CarRental, Transfer, Activity, LoungeAccess, ESim — a new item type nearly every quarter. Each new type required: an `accept` method, a new `visit` overload on the interface, and edits to 12 visitor implementations spread across 5 team repositories. Releases became lockstep — no team could ship a new item type without a coordinated change across all five repos. Under deadline pressure, teams started adding `default void visit(NewThing t) {}` to unblock themselves — and within a year, three item types were silently absent from tax reports (the Incident-2 silent-skip, now at organizational scale). The JDK's `SimpleElementVisitor6→14` version ladder was sitting in their dependency tree the whole time, a warning label nobody read.

1. Name the root cause precisely: pattern chosen against the grain of the DOMINANT change axis. Visitor makes operations cheap and types expensive; their types grew, their operations didn't. The expression problem wasn't considered at design time.
2. State the question that would have prevented it — the one-line design review question you should now always ask before choosing Visitor: **"Over the next two years, which grows faster: the element types or the operations?"** Evidence beats vibes: the roadmap already showed the answer.
3. Name the organizational amplifier: a visitor interface shared across 5 repos turns a compile-time break (the pattern's FEATURE) into cross-team release coupling — the cost scales with the number of visitor OWNERS, not just visitor count. What was a refactor in one module becomes a program-management problem across teams.
4. Prescribe the migration: flip the axis — move the two-to-three real operations onto the item types as polymorphic methods (or sealed + switch per consuming module), delete the visitor interface, and keep Visitor ONLY if/where a genuinely frozen sub-hierarchy with unbounded operations exists (their reporting AST, if any). State what the silent no-op defaults must be replaced with DURING the migration so the three missing item types surface instead of staying invisible (throwing defaults or exhaustive switches — make the gaps loud, then fix them).

---

**Sources used for real-world cases:**
- [JEP 441: Pattern Matching for switch](https://openjdk.org/jeps/441)
- [InfoQ: Transforming Java with Pattern Matching](https://www.infoq.com/news/2023/07/tranforming-java-pattern/)
- [ElementVisitor (Java SE 17)](https://docs.oracle.com/en/java/javase/17/docs/api/java.compiler/javax/lang/model/element/ElementVisitor.html) / [SimpleElementVisitor6](https://docs.oracle.com/javase/7/docs/api/javax/lang/model/util/SimpleElementVisitor6.html)
- [Gradle forums: Unsupported class file major version 61](https://discuss.gradle.org/t/java-lang-illegalargumentexception-unsupported-class-file-major-version-61/42081)
- [MDEP-613: Maven dependency plugin ASM failure](https://issues.apache.org/jira/browse/MDEP-613)
- [Google issue tracker: AGP/Jetifier ASM version](https://issuetracker.google.com/issues/159151549/dupes)
