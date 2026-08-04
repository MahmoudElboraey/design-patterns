# Prototype Pattern — FAANG Interview Test

**Time budget:** 90 min. Whiteboard/IDE. Talk while you build.
**You already know:** SOLID, Observer, Strategy, Template Method, Builder, Singleton, Factory / Factory Method.
**Grading axis:** correctness of the copy (shallow vs deep — this is the whole pattern) > pattern mechanics > edge/failure handling > knowing when *not* to clone > tradeoff talk.

Two things every strong candidate says out loud on this pattern:
> Prototype = "create a new object by **copying an existing configured instance**," not by constructing from scratch. You reach for it when construction is **expensive** and you already hold a ready-made instance to copy.

> Java's `Cloneable` / `Object.clone()` is **broken** (Bloch, *Effective Java*: `Cloneable` doesn't even declare `clone()`, it does a shallow copy by default, and it bypasses constructors). Modern Java implements Prototype with a **copy constructor or a copy factory**, not `implements Cloneable`.

The baits on this one are almost all **aliasing** baits — a copy that secretly shares mutable state with its source. That failure is silent and it is the reason the pattern is graded on the copy first and everything else second.

---

## Part A — Design from scratch (40%)

### Scenario

You own the **quote/pricing engine** for a checkout service. For each product catalog, there is a **base `Quote` template** that is *expensive* to assemble: it loads the active tax schedule, regional fee tables, the discount matrix, and a list of default line items — pulled from the DB and a rules service, then validated. Building one from scratch takes ~hundreds of ms.

Every customer checkout needs a quote that **starts from that base template** and then gets per-customer tweaks: add line items, apply a coupon, adjust the shipping fee. Thousands of checkouts/sec. You cannot rebuild the template per checkout, and you must not let one checkout's edits touch the template or any other checkout's quote.

A `Quote` holds:
- `currency` (immutable)
- `List<LineItem> lineItems` — **mutable**, edited per checkout (LineItem itself is mutable: qty, price)
- `Map<String, Discount> discounts` — **mutable**
- `taxSchedule` — a large, **immutable**, shared-safe reference object (never mutated after load)
- `shippingFee` — mutable number

### Rules the design must satisfy
1. A per-checkout quote is produced by **copying** the base template, not by re-running the expensive assembly.
2. Editing a copied quote's `lineItems` / `discounts` / `shippingFee` must have **zero** effect on the template or on any other copy. Prove it.
3. The `taxSchedule` is immutable and huge — copies should **share** it by reference, not deep-copy it. (So "deep copy everything" is the wrong answer too.)
4. A copy must be a **valid** `Quote` — the same invariants the constructor enforces must hold on the copy. (Think about what `clone()` skips.)
5. New quote templates are registered at runtime keyed by catalog id; client code asks for "a fresh quote for catalog X" and never sees concrete construction.

### Deliverables
- **(a)** UML. Show the prototype, the copy operation, and the registry/access point. Mark every field as deep-copied vs shared-by-reference and **why** for each.
- **(b)** Name the SOLID principles this leans on and where.
- **(c)** Code it. Implement the copy **without** `Cloneable`/`Object.clone()` — use a copy constructor or copy factory. Show the deep copy of the mutable fields, the by-reference share of the immutable one, and the validation on copy. Then a small registry that hands out fresh copies.
- **(d)** One paragraph: why Prototype here and not a Builder, a Factory Method, or just calling the constructor again.

### Questions baked in (answer inline)
- Rule 2 vs rule 3: which fields do you deep-copy, which do you share, and what is the exact rule you used to decide? State the rule as one sentence.
- Show, in a 4-line snippet, the aliasing bug that a shallow copy would cause here — one checkout corrupting the template — and show your copy not doing it.
- Rule 4: `Object.clone()` produces an instance **without running a constructor**. Why is that dangerous for a `Quote`, and how does your copy-constructor approach fix it?

---

## Part B — Production refactor (35%)

Real code. Shipped. Caused a pricing incident.

```java
public class QuoteTemplateRegistry {

    private final Map<String, Quote> templates = new HashMap<>();

    public void register(String catalogId, Quote template) {
        templates.put(catalogId, template);
    }

    public Quote getQuote(String catalogId) {
        return templates.get(catalogId);      // <-- incident #1
    }
}

class Quote implements Cloneable {
    String currency;
    List<LineItem> lineItems;
    Map<String, Discount> discounts;
    TaxSchedule taxSchedule;
    double shippingFee;

    @Override
    public Quote clone() throws CloneNotSupportedException {
        return (Quote) super.clone();          // <-- incident #2
    }
}
```

Usage in the checkout path:
```java
Quote q = registry.getQuote("catalog-eu");
q.lineItems.add(new LineItem("SKU-9", 2, 19.99));   // customer's item
q.discounts.put("WELCOME10", tenPercent);
// ... price it, charge the customer
```

The incident: checkout traffic mutated `q.lineItems` and `q.discounts`. Because `getQuote` returned the **stored template itself** (and even where someone used `clone()`, `super.clone()` is a **shallow** copy sharing the same list/map), every checkout was appending to the **same** `lineItems`. Customers were charged for **other customers' items**. Cross-customer contamination on a money path. Found in a chargeback spike.

### Deliverables
- **(a)** Name every distinct smell (there are at least 5 — including the `Cloneable`/`super.clone()` choice itself, the registry returning the live template, and the raw `String` key). One line each.
- **(b)** UML of the refactor.
- **(c)** SOLID writeup — which principles the original violates, which the refactor restores. Name them, say where.
- **(d)** Refactor. Fix **incident #1** (registry hands out the live instance) and **incident #2** (shallow `clone()` shares mutable state) structurally — impossible to reintroduce. Rip out `Cloneable`. Decide per-field deep-vs-shared and justify each.
- **(e)** A teammate says "just deep-copy everything, problem solved." Why is that both a correctness *over-fix* and a performance bug here? Name the field it breaks and the field it wastes.

---

## Part C — Hardening round (15%)

Short answers. Pressure round.

1. **Why not `Cloneable`.** State three concrete defects of `Cloneable`/`Object.clone()` that make it the wrong tool, and the modern replacement. (Bloch has said this — get it right.)
2. **The decision rule.** Give the one-line rule for when a field must be deep-copied vs shared-by-reference in a clone. Apply it to `taxSchedule` vs `lineItems`.
3. **Clone skips validation.** `clone()` (and deserialization) can produce an object that never passed a constructor. Name the invariant on `Quote` this could violate, and how a copy constructor closes the hole.
4. **Registry + Singleton collision.** The `QuoteTemplateRegistry` is a process-wide Singleton holding the prototypes. What is the exact danger of a Singleton registry that stores **mutable** prototypes, and what one guarantee kills it? (Connect it to Part B incident #1.)
5. **Prototype vs Factory Method vs Builder.** All three hand you a new object. One sentence each: when the problem wants Prototype, when Factory Method, when Builder.
6. **Deep copy hazard.** Your deep copy recurses through references. Name the one input-graph shape that makes a naive deep copy infinite-loop or blow the stack, and the standard fix.

---

## Part D — Tradeoffs & when-NOT (10%)

- **Over-engineering.** One case where Prototype is the wrong call and plain `new` (or a Builder) is correct — and the signal that tells you construction is cheap enough that copying buys nothing.
- **Three ways to copy.** Copy constructor vs copy factory vs serialization-based deep clone (serialize→deserialize). One honest tradeoff for each — when serialization-clone is tempting and why it's usually a trap (perf, `transient`, must-be-Serializable).
- **The maintenance cost.** Prototype's real long-term tax is *clone drift*. Explain what happens when someone adds a new mutable field to `Quote` six months later, and what discipline (or language feature) prevents the resulting bug.
- **Records / immutability.** If `Quote` and its fields were deeply immutable (records + immutable collections), what happens to the entire need for Prototype's deep copy? State the principle.

---

### Submission
- `A/` package — quote/pricing prototype + registry
- `B/` package — QuoteTemplateRegistry refactor
- Written answers (UML + prose) as PDF or markdown.

Baits live — and they are aliasing baits. The copy is the pattern: get shallow-vs-deep-vs-shared right per field, or the money path leaks. Fail loud where an invalid copy would ship a wrong price.
