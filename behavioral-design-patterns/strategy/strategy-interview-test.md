# Machine Coding Round — Strategy Pattern in Production

> Format mirrors a real LLD / machine-coding interview (Uber/Google/Netflix style):
> one build problem, one production refactor, one hardening round, UML + trade-offs.
> Scope: Strategy + UML + SOLID, with Observer cross-questions (you know both now — interviewers WILL make you pick).
> Strategy is the single most-asked pattern in LLD rounds — the tell is always "a pile of if/else switching on type."
> Budget like a real round: ~90 min total.

---

## Part A — Machine Coding: Ride Fare Engine (the Uber problem)

**Q1.** You're building the fare-calculation core of a ride-hailing app (think Uber/Careem).

**Functional requirements:**

1. A trip has: distance (km), duration (minutes), ride type. Fare is computed by `calculateFare(Trip trip)`.
2. Ride types at launch, each with different math:
   - **ECONOMY** — base 10 + 2.5/km + 0.5/min
   - **PREMIUM** — base 25 + 4/km + 1/min
   - **POOL** — like economy but 30% off, minimum fare 15 (discount can never go below it)
3. **Surge pricing** is a SEPARATE axis: at any moment a zone is either normal (×1.0) or surging (×1.5, ×2.0, ...). Surge multiplies the ride-type fare. Surge logic must be swappable without touching any ride-type class (real Uber runs ML-driven surge; your v1 is a fixed multiplier — design so the ML version drops in later).
4. **Promo codes** are a THIRD axis: "10% off", "flat 20 off", or none. Applied after surge.
5. Next sprint (do NOT implement, design must absorb with zero changes to existing classes):
   - new ride type: **MOTO** (bike taxi, different formula)
   - new promo type: "first ride free up to cap"

**Non-functional expectations (state how design meets them):**

- Thousands of concurrent trips price simultaneously — strategy objects must be safely shareable across threads (this constrains how you write them; say the word for that property).
- Adding MOTO must be: one new class + one registration line. Nowhere else.

**Deliverables:**
- Interfaces + classes, working Java
- A `main` demo: same trip priced as ECONOMY normal, ECONOMY at ×2.0 surge, POOL with "flat 20 off" proving the minimum-fare floor holds
- Short paragraph: who is context, who is strategy, how many independent strategy axes did you find, and how they compose. Where does selection happen (who decides WHICH strategy runs)?

---

## Part B — Production Refactor: the if/else pyramid

**Q2.** Real production smell — this is your own codebase's shape (compare the margin appliers in flight-search: `AmadeusMarginApplier` / `AirarabiaMarginApplier` / `AegeanMarginApplier` exist precisely so this method doesn't). Refactor this checkout-fee service:

```java
public class CheckoutService {

    public double calculateProcessingFee(Order order, String paymentMethod) {
        double fee;
        if (paymentMethod.equals("CREDIT_CARD")) {
            fee = order.getTotal() * 0.029 + 0.30;
            if (order.getTotal() > 5000) {
                fee = order.getTotal() * 0.025;          // volume discount
            }
        } else if (paymentMethod.equals("PAYPAL")) {
            fee = order.getTotal() * 0.034 + 0.35;
        } else if (paymentMethod.equals("BANK_TRANSFER")) {
            fee = 5.0;
            if (order.getCurrency().equals("EUR")) {
                fee = 0.0;                                // SEPA is free
            }
        } else if (paymentMethod.equals("CRYPTO")) {
            fee = order.getTotal() * 0.01;
        } else {
            fee = 0.0;                                    // ← ticking bomb, see Part C
        }
        return fee;
    }
}
```

Product adds a payment method roughly every quarter. Each addition = editing this method = re-testing everything = one merge conflict per team.

**Tasks:**

a) Refactor to Strategy: interface, concrete strategies, selection mechanism, rewritten `CheckoutService`. New payment method = new class + one registration, `calculateProcessingFee` never edited again.

b) Guaranteed interview follow-up — answer it before asked: **"You didn't delete the if/else, you moved it. Where is it now, and why is THAT acceptable when the original wasn't?"**

c) The `else → fee = 0.0` branch: your factory must make a decision here — fail loud or silent default? Pick one, justify with what happens in production for each choice. (You shipped `getOrDefault(customerType, order -> 0)` in your discount solution — same decision, this time real money leaks through it.)

d) Name every SOLID principle the refactor improves, one sentence each — unprompted narration, still drilling the habit.

---

## Part C — Hardening Round: production pitfalls

**Q3.** Your Part A engine goes to production. Three incidents. For each: name the root cause, point at the vulnerable line(s) in YOUR code, show the fix:

a) **Random wrong fares under load.** A teammate "optimized" a strategy by adding a field: `private double runningTotal;` used as scratch space inside `calculateFare`, and the strategy is a shared singleton. Fares are correct in every unit test, wrong ~0.1% of the time in production, unreproducible locally. Name the exact concurrency problem, explain why tests never catch it, give the fix — and state the general rule about strategy state that prevents the whole class of bug.

b) **Strategy explosion.** Product now wants: 4 ride types × 3 surge modes × 5 promo types. A junior starts writing `EconomySurgePromoStrategy`, `EconomySurgeNoPromoStrategy`, ... 60 classes. Stop him. What structural mistake is he making, and how does YOUR Part A design already avoid it? (One word answer exists; then explain.)

c) **New country launch, silent revenue loss.** Ops adds ride type `TUKTUK` via config, forgets to register a strategy. If your Q1 selection used a silent default: every tuktuk ride priced as ECONOMY for 3 weeks, nobody notices, finance finds it in reconciliation. If it failed loud: launch blocked day one with a clear error. Which did YOUR code do? Rewrite the selection to the behavior you now believe in, and say when (if ever) the opposite is defensible.

**Q4.** Selection is the hidden second half of Strategy:

a) Your factory picks by ride-type enum. Real Uber surge is picked by an ML model reading live supply/demand; real Netflix picks an encoding ladder per title by analyzing content complexity (their per-title encoding — animation gets a different "strategy" than action films). Generalize: the selector itself is a decision that can be dumb (map lookup), rule-based, or learned. Show how your design lets the SELECTOR be swapped without touching strategies OR context — what does that make the selector? (Look at your own design honestly: is it already this, or did you hardcode selection into the context?)

b) Spring/Quarkus reality: in your day job, CDI can inject `Instance<PaymentFeeStrategy>` (all implementations) and you pick from the list. Compare that to a hand-written `Map.of` factory — what does DI-discovery buy you and what new failure mode does it introduce (hint: what happens when someone forgets an annotation vs forgets a map entry)?

**Q5.** Real post-mortems — these actually happened; analyze them through the strategy lens:

a) **Knight Capital, Aug 1 2012 — $460M gone in 45 minutes.** Their trading system SMARS selected order-handling algorithms via a feature flag. An algorithm called "Power Peg" was deprecated in 2003 but its code was left in place, dead, behind the flag. In 2012 engineers REUSED that flag name for a new algorithm (RLP). Deploy went to 8 servers; a technician missed one. Flag turned on: 7 servers ran the new algorithm, the 8th woke up 9-year-old dead code that bought high and sold low in a loop. Ops rolled back the code on all servers — but left the flag on, so now ALL 8 ran Power Peg. 4 million trades, $7B in positions, company effectively dead by morning.

   Answer:
   - Map this to strategy vocabulary: what was the strategy interface, the concrete strategies, the selector? Which part failed?
   - Name THREE distinct engineering failures in that story (each one alone would have prevented the loss). At least one must be about dead strategies and one about the selector.
   - Your Part A factory registers strategies at startup. What is your equivalent of "Power Peg still registered"? Write the rule you'd enforce in code review.

b) **Airline mistake fares — currency as silent strategy input.** United sold $4,000 flights for $79 (DKK→GBP conversion applied by the wrong rule); Cathay Pacific sold $16,000 first-class seats for $675 and honored them. These are pricing strategies fed wrong currency context, and the number LOOKED valid — no exception, no crash, just a plausible small number. Your flight-search service applies margins per `(FlightSource, AmadeusSource, Currency)` — same shaped risk.

   Answer:
   - Why does fail-loud in the factory NOT save you here? (The strategy was found and ran fine.) What kind of check DOES catch "result is valid-typed but absurd"?
   - Name the cheapest guard you'd add to your Part A engine so a fare of 0.4 instead of 40 never reaches a customer. Where does it live — inside strategies, or in the context after them? Defend.

---

## Part D — UML + Trade-offs

**Q6.** Full UML class diagram of your Part A design (hand-drawn like before). Graded hard on:

- Context → strategy interface: association type, diamond or not, which side owns, multiplicity — and defend fill of any diamond (strategies injected from outside: what does that force the diamond to be?)
- Concrete strategies → interface: realization, dashed line, HOLLOW triangle
- Three strategy axes (ride type / surge / promo): show all three hanging off the context correctly — this is where diagrams get messy; keep it readable
- Factory arrows: dependency arrows at the CONCRETE classes it creates + the interface it returns. Recurring bug — third test now, prove it's dead
- SOLID narration written next to the diagram, unprompted

**Q7.** Judgment questions, 2–3 sentences each:

a) **Strategy vs Observer** — you now know both. One sentence each on what problem each solves, then: checkout completes and you must (i) compute the fee, (ii) notify email+analytics+loyalty. Which pattern for which, and why can't they swap?
b) **Strategy vs plain lambdas** — your discount factory was `Map<String, CustomerDiscount>` with lambdas. When is a lambda-in-a-map enough, and what forces you to promote to real classes? Name two concrete forcing conditions.
c) **Strategy vs Template Method** — both vary an algorithm. Template Method uses inheritance (abstract base + overridden steps), Strategy uses composition. Why does modern advice default to Strategy? Name the principle, and one case where Template Method is still the better fit.
d) A teammate says "every if/else is a Strategy waiting to happen, let's convert them all." Push back: give two concrete signals that an if/else should STAY an if/else.

**Q8.** The evolution ladder (YAGNI judgment). Fee calculation, three stages:

```java
// Stage 1 — if/else in place
double fee = method.equals("CARD") ? total * 0.029 : 5.0;

// Stage 2 — strategy interface + hand-written factory map
Map<String, FeeStrategy> strategies = Map.of("CARD", new CardFee(), "BANK", new BankFee());

// Stage 3 — registry: strategies self-register / discovered via DI,
// selection rules themselves configurable (per-country overrides, A/B tests, rollout %)
```

For each requirement, pick the CHEAPEST stage that satisfies it, and say what NEW capability each stage buys over the previous:

a) "Two payment methods, spec frozen, small internal tool."
b) "Payment methods added quarterly by different teams; each must ship theirs without touching shared code."
c) "Business wants per-country fee rules, A/B tests on fee formulas, and gradual rollout of new pricing — changed by ops without a deploy."
d) A teammate builds Stage 3 for requirement (a) "so we're future-proof." Same question as always — name the principle violated and the concrete costs paid today. NEW twist: name one cost that is specific to Stage 3 here and did NOT exist in the Observer Stage-3 overbuild (hint: think about where selection rules live and who can change them).

---

## Part E — Pattern-or-Not Gauntlet — /10

### Q9. When Strategy fits, when it doesn't, and when it burns you

**A) Five scenarios — verdict (Strategy / not Strategy / different tool) + one-line justification each. Three contain traps.**

1. Two tax formulas, unchanged for six years, no third on any roadmap, small internal tool.
2. Per-tenant pricing algorithm, chosen at runtime from an admin panel, new algorithms shipped quarterly by different teams.
3. Behavior differs by the TYPE of item in a heterogeneous list — flights price one way, hotels another, bags another.
4. The "algorithm" needs nine fields from the context object and mutates four of them.
5. Discounting has 4 independent axes (customer tier × season × channel × promo), 5 options each — someone proposes one strategy class per combination.

The traps to catch: (3) is TYPE dispatch, not algorithm selection — that's polymorphism on the items or Visitor territory, not Strategy (Strategy = one algorithm slot varying per context; the items aren't choosing algorithms, they ARE different types). (4) is a leaking-context smell — either the method belongs ON the context, or the strategy interface needs a narrow parameter object; a strategy that mutates its context is a lieutenant rewriting the general's orders. (5) is combinatorial explosion — 625 classes; axes must COMPOSE (chain/decorate strategies, or data-driven rule objects), one class per combination is the pattern applied to the wrong granularity.

**B) Misuse post-mortem — Strategy applied where it didn't belong:**

A platform team, burned once by an if/else pyramid, swore "never again" and made EVERYTHING pluggable: 40 strategy interfaces across the codebase — `GreetingStrategy`, `TimestampFormatStrategy`, `NullCheckStrategy` — each with exactly ONE production implementation, wired through DI. Two years later: new hires need days to trace a single request through the indirection; every debugging session bounces through interface → DI container → sole implementation; test setup requires mocking strategies that never varied; and when a REAL variation finally arrived (a second tax formula), it shipped late anyway because the existing `TaxStrategy` interface had the wrong signature for it.

1. Name the smell with its proper term: **speculative generality** — abstraction built for variation that was imagined, not demonstrated.
2. State the design-time signal the team ignored: an interface with one implementation and no concrete second use case on any roadmap is not an abstraction, it's a costume. The rule of thumb they needed: extract the strategy when the SECOND variant arrives (rule of three for the cautious), because only real variants tell you where the interface boundary actually is — which is exactly why their pre-built `TaxStrategy` signature was wrong.
3. Prescribe the fix: inline the 37 single-implementation strategies back into their callers; keep the 3 with real runtime variation. Name the principle that justifies the deletion (YAGNI / simplicity) and reconcile it in two sentences with OCP — how can "open for extension" and "don't build extension points" both be right? (Binding: OCP is a response to DEMONSTRATED change axes, not a mandate to pre-abstract every axis.)

---

*Working code goes wherever you put this pattern's project, diagrams on paper. Grading /10 per part, honest FAANG machine-coding bar: correctness, extensibility proven not claimed, narration of trade-offs unprompted.*
