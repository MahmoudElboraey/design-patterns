# Factory Method (+ Simple / Abstract Factory) — FAANG Interview Test

**Time budget:** 90 min. Whiteboard/IDE. Talk while you build.
**You already know:** SOLID, Observer, Strategy, Template Method, Builder, Singleton.
**Grading axis:** correctness of the creation seam > pattern mechanics (and naming the *right* factory variant) > edge/failure handling > knowing when a factory is the wrong answer > tradeoff talk.

GoF, verbatim — hold this in your head the whole test:
> "Define an interface for creating an object, but let subclasses decide which class to instantiate. Factory Method lets a class defer instantiation to subclasses."

Note what that sentence does **not** say: it says nothing about a `switch` on a type string. That thing everyone calls "a factory" is **Simple Factory** — not a GoF pattern. Knowing which of the three you actually built, and why, is half the score here. Baits are live. The biggest one is calling the wrong variant by the wrong name.

---

## Part A — Design from scratch (40%)

### Scenario

You own the **statement/document generation service** at a fintech. Every night it produces legal financial documents — **account statements, invoices, tax receipts, credit notes** — and delivers them. Each document type shares the *same job lifecycle*:

```
load account data → render the document → validate it → deliver (email / SFTP / archive)
```

…but the **renderer** differs per document type: an `InvoiceRenderer` lays out line items + VAT; a `TaxReceiptRenderer` stamps a legal receipt number; a `StatementRenderer` paginates a transaction ledger. The lifecycle never changes; the *product being created inside it* does.

New document types are added every few quarters by different teams. A wrong or missing renderer means a **legally-wrong document shipped to a customer** — a compliance incident, not a cosmetic bug.

### Rules the design must satisfy
1. The job lifecycle (`load → render → validate → deliver`) is defined **once** and must not be copy-pasted per document type.
2. Each document type decides **which concrete renderer** it uses — adding a type must not touch the lifecycle code or any other type.
3. Client code that triggers a job must not `new` a concrete renderer, and must not know concrete renderer classes exist.
4. An unknown / unconfigured document type must **not** silently produce a blank or default document. (Read that twice.)
5. Renderers are stateless and expensive-ish to spin up; the design should not forbid reuse/caching, but must not force a broken singleton either.

### Deliverables
- **(a)** UML. Show the **Creator / Product / ConcreteCreator / ConcreteProduct** roles explicitly. Mark which arrow is inheritance and which is the "creates" dependency. Mark where the lifecycle lives.
- **(b)** Name the SOLID principles this leans on and where (there are at least three — one is the whole reason rule 2 exists).
- **(c)** Code it. The Creator with the lifecycle + the factory method hook; ≥2 ConcreteCreators; the Product interface + ≥2 ConcreteProducts. Working.
- **(d)** One paragraph: this class has a fixed lifecycle with an overridable creation step. That is **two** patterns living in one class. Name both, say which method is which, and why that's not a code smell.

### Questions baked in (answer inline)
- Exactly which pattern is doing the work in rule 1 vs rule 2? They are different patterns. Name each.
- Rule 4: show the line that makes an unknown type fail loud, and name what shipping-a-blank-document would have cost. When, if ever, is a silent default the *correct* call here?
- A junior says "just make one `RendererFactory` with a `switch(type)` and delete all the subclasses — simpler." Is he wrong? Answer honestly — when is his Simple Factory the *better* engineering call, and when does it break rule 2?

---

## Part B — Production refactor (35%)

Real code. Shipped. Caused a production incident when a fifth document type was added.

```java
public class DocumentService {

    public byte[] generate(String docType, Account account) {
        Renderer renderer;
        if (docType.equals("INVOICE")) {
            renderer = new InvoiceRenderer();
        } else if (docType.equals("STATEMENT")) {
            renderer = new StatementRenderer();
        } else if (docType.equals("RECEIPT")) {
            renderer = new ReceiptRenderer();
        } else {
            renderer = new PlainRenderer();   // <-- incident
        }
        return renderer.render(account);
    }
}
```

The same `if/else` block is **copy-pasted in 9 other places** (email path, SFTP path, archive path, a reprint endpoint, a preview endpoint…), each slightly out of sync.

The incident: a team shipped `"CREDIT_NOTE"` support, registered it in **7 of the 10** blocks, missed 3. On those paths `docType="CREDIT_NOTE"` fell into the `else` → customers received a **`PlainRenderer` blank document** where a credit note should have been. Nobody threw. It was found in a customer complaint two weeks later.

### Deliverables
- **(a)** Name every distinct smell (there are at least 5 — one is the `else` branch, one is the copy-paste, one is the `String` type key, and one is *architectural*, not local). One line each.
- **(b)** UML of the refactor.
- **(c)** SOLID writeup — which principles the original violates (name the two biggest), which the refactor restores. Say exactly where.
- **(d)** Refactor. Kill the 10-way copy-paste **and** the silent-`else`, structurally (impossible to reintroduce). Pick your factory variant deliberately and justify the pick — Simple Factory, Factory Method, or a registry-based factory. State why the other two lose here.
- **(e)** `docType` is a raw `String`. Call it out. What do you change it to, and is that change *part of* the factory or a separate concern? (You answered a question shaped like this on the Builder test — same reasoning, different pattern.)

---

## Part C — Hardening round (15%)

Short answers. Pressure round.

1. **Name the variant.** Given (i) `Calendar.getInstance()`, (ii) `Collection.iterator()`, (iii) `DocumentBuilderFactory.newInstance()` returning a whole family of related parser objects — label each as Simple Factory / Factory Method / Abstract Factory. One reason each.
2. **Registration vs. switch.** You refactor Part B to a `Map<DocType, Supplier<Renderer>>` registry instead of a `switch`. What does that buy over Simple Factory, and what new *silent* failure mode does it introduce that the `switch` didn't have? (Tie back to rule 4.)
3. **Factory returns a Singleton.** Renderers are stateless — you want one shared instance per type, created lazily. Wire that through the factory. What breaks if a renderer secretly holds mutable state and you've cached it? Name the bug class.
4. **Abstract Factory boundary.** Requirement changes: statements must render with a **matching set** of `{CurrencyFormatter, DateFormatter, AddressFormatter}` per locale — EU set vs US set, and mixing an EU currency formatter with a US date formatter is a bug. Which factory variant, and why is Factory Method the *wrong* tool for this specific shape? One sentence.
5. **Three-way it's-just-an-overridden-method.** Factory Method overrides a method to return a product; Template Method overrides a method to do a step; Strategy overrides a method behind an injected interface. In Part A's Creator, `createRenderer()` and the lifecycle `run()` sit in the same class. One sentence each: why `createRenderer()` is Factory Method and not Strategy, and why `run()` is Template Method and not Factory Method.
6. **Factory vs. Builder.** Both are creational, both "make objects." One sentence: what question tells you a problem wants a Factory (or Factory Method) rather than a Builder?

---

## Part D — Tradeoffs & when-NOT (10%)

- **Over-engineering.** One case where introducing Factory Method is speculative generality you'd reject in code review. What's the cheaper thing, and what's the signal that tells you it's premature?
- **Simple Factory's honest sin.** Simple Factory centralizes creation in one `switch`. Name the SOLID principle it violates by design, then argue why you'd *still* ship it sometimes anyway.
- **Abstract Factory's cost.** It kills mix-and-match bugs (C4) but pays a real price when you add a new *product* to the family (not a new family). Name that cost precisely.
- **DI container.** Given Spring/CDI producers and qualifier injection, does a hand-rolled factory or Factory Method still earn its place? Give one concrete case where yes.

---

### Submission
- `A/` package — statement/document generation service
- `B/` package — DocumentService refactor
- Written answers (UML + prose) as PDF or markdown.

Baits live. Rule 4 is the money/correctness path — you know the litmus: fail loud where a wrong object ships a wrong outcome. And name your variant correctly; "I used a factory" is not an answer here.
