# Machine Coding Round — Observer Pattern in Production

> Format mirrors a real LLD / machine-coding interview (Flipkart/Uber/Amazon style):
> one build problem, one production refactor, one hardening round, UML + trade-offs.
> Scope: Observer + UML + SOLID. Working Java expected for Parts A–B (put it in `solid-test-sol`).
> Budget like a real round: ~90 min total.

---

## Part A — Machine Coding: Stock Price Alert System (the classic LLD problem)

**Q1.** You're building the alerting core of a brokerage app (think Robinhood/Thndr).

**Functional requirements:**

1. The system tracks many stocks (`AAPL`, `TSLA`, ...). Each stock's price is updated by a market-data feed calling `updatePrice(symbol, newPrice)`.
2. Users create **alert subscriptions** at runtime, e.g.:
   - "Notify me when AAPL goes **above** 200"
   - "Notify me when TSLA goes **below** 150"
   - "Notify me on **every** price change of AAPL" (no threshold)
3. A user chooses a **delivery channel** per subscription: email, SMS, or in-app push.
4. Users can **cancel** a subscription at any time. Cancelled = never notified again.
5. Next sprint (do NOT implement, but design must absorb it with zero changes to the stock/price core):
   - new rule type: "price moved more than X% in a day"
   - new channel: WhatsApp

**Non-functional expectations (state how your design meets them, no need to fully implement):**

- One slow/failing channel must not prevent other users from being notified.
- Same rule evaluated for thousands of users must not require thousands of copies of the rule logic.

**Deliverables:**
- Interfaces + classes, working Java
- A `main` demo: 2 users, 2 different rules, 2 different channels, price updates trigger correct notifications, one user cancels, next update skips them
- Say out loud (write as comments or a short paragraph): where is the Observer pattern here — who is subject, who is observer, what is the event? Where did you combine it with the strategy idea from your `CustomerDiscount` solution?

---

## Part B — Production Refactor: the God `placeOrder`

**Q2.** Real production smell — this is how it actually looks in codebases (compare your `FlightSearchService`: after search completes, results must go to Redis, metrics, and consumers). Refactor this e-commerce service:

```java
public class OrderService {
    private final EmailClient emailClient = new EmailClient();
    private final AnalyticsClient analytics = new AnalyticsClient();
    private final LoyaltyService loyalty = new LoyaltyService();
    private final WarehouseClient warehouse = new WarehouseClient();

    public Order placeOrder(Cart cart, User user) {
        Order order = createOrder(cart, user);          // core domain logic
        chargePayment(order);                           // core domain logic

        emailClient.sendConfirmation(user.getEmail(), order);   // side effect
        analytics.track("order_placed", order.getId());         // side effect
        loyalty.addPoints(user.getId(), order.getTotal());      // side effect
        warehouse.reserveStock(order.getItems());               // side effect

        return order;
    }
}
```

Every sprint someone adds another side effect and `OrderService` grows. Marketing wants to A/B-test removing the email; data team wants a second analytics event; none of them should touch `OrderService`.

**Tasks:**

a) Refactor using Observer (domain-event style): define the event, the listener interface, the registration mechanism, and the rewritten `placeOrder`. New side effect = new class + registration, `OrderService` never edited again.

b) Interview follow-up you MUST answer: `warehouse.reserveStock` is **not like the others** — if stock reservation fails, the order must fail. Email failing should NOT fail the order. Does `reserveStock` belong in the observer list at all? Justify.

c) Name every SOLID principle your refactor improves, one sentence each — unprompted narration, this is the habit you're drilling.

---

## Part C — Hardening Round: production pitfalls

**Q3.** Your Part A code goes to production. Three incidents come in. For each: name the classic pitfall (interviewers expect the proper term where one exists), point to the line(s) in YOUR Q1 code that are vulnerable, and show the fix:

a) **Memory climbs for weeks, OOM.** Heap dump: millions of dead subscription objects still referenced. Users closed the app long ago but never explicitly cancelled. (Proper term exists — this exact failure has a name.)

b) **`ConcurrentModificationException`** in the notification loop. Stack trace shows an observer calling `cancel()` on itself from inside its own `update()` while the subject iterates the observer list. Give TWO different fixes and the trade-off between them.

c) **One user's push token is invalid → the push channel throws → 4,000 users after them in the list get NO notification that day.** Fix so one failing observer can't break the chain, and say what you do with the failure (swallow? log? retry? dead-letter?).

**Q4.** Sync vs async notification:

a) Your Q1 `updatePrice` currently notifies observers on the market-data feed's thread. The feed pushes 500 updates/second. Email takes 300 ms. Do the math out loud — what happens?
b) Sketch (words or code) the async version. What NEW problems does async introduce that sync didn't have? Name two.

---

## Part D — UML + Trade-offs

**Q5.** Full UML class diagram of your Part A design (hand-drawn like before). Graded hard on:

- Subject → observer-interface: arrow type, which side owns, **multiplicity** (`1 → 0..*`)
- Concrete observers → interface (realization, hollow triangle)
- Rule/strategy objects: how do they hang off subscriptions — aggregation or composition? Defend the diamond you chose and its fill
- Factory/creation arrows if you used any — point them at the CONCRETE classes (your recurring bug, prove it's dead)

**Q6.** Judgment questions, 2–3 sentences each:

a) Observer vs just injecting a `List<NotificationChannel>` and looping — when is plain-list injection actually the BETTER choice? (Hint: your `PaymentService` receipt design.)
b) Observer vs a message broker (you use Azure Service Bus daily — `FlightSearchConsumer` IS this). What does in-process Observer give up vs Service Bus topics, and what does it gain? Two items each side.
c) A teammate says "let's use Observer everywhere instead of direct calls, loose coupling is always better." Push back with the concrete debugging/readability cost — what specifically becomes harder in a codebase where everything is events?

**Q7.** The evolution ladder (YAGNI judgment). Three stages of the receipt design from your payment module:

```java
// Stage 1 — single injected field
public class PaymentService {
    private final ReceiptSender receiptSender;
    // on success: receiptSender.send(receipt);
}

// Stage 2 — injected list, loop
public class PaymentService {
    private final List<ReceiptSender> receiptSenders;
    // on success: for (ReceiptSender s : receiptSenders) s.send(receipt);
}

// Stage 3 — full Observer: runtime subscribe/unsubscribe
public class PaymentEvents {
    private final List<PaymentListener> listeners = new CopyOnWriteArrayList<>();
    public void subscribe(PaymentListener l)   { listeners.add(l); }
    public void unsubscribe(PaymentListener l) { listeners.remove(l); }
    public void publish(PaymentSucceeded event) { for (PaymentListener l : listeners) l.on(event); }
}
```

For each requirement below, pick the CHEAPEST stage that satisfies it, and say what NEW capability each stage buys over the previous (that's the only justification for climbing):

a) "Send receipt by email after every successful payment."
b) "Send email AND SMS AND push, all three, always — the set is fixed at deployment."
c) "Ops must add/remove channels at RUNTIME via admin panel, without restart. Fraud team also wants to attach a temporary listener during investigations."
d) A teammate jumps straight to Stage 3 for requirement (a) "so we're future-proof." Name the principle he's violating and the concrete costs he's paying today for flexibility nobody asked for.

---

## Part E — Pattern-or-Not Gauntlet — /10

### Q8. When Observer fits, when it doesn't, and when it burns you

**A) Five scenarios — verdict (Observer / not Observer / Observer-but-different-tool) + one-line justification each. Three contain traps.**

1. Checkout flow: after order placed → decrement inventory, charge card, send confirmation email. The charge MUST succeed before the email goes out; a failed inventory decrement must ABORT the whole order.
2. Admin dashboard widget must refresh whenever an in-memory config object changes, same JVM, same process.
3. Your flight-search service must inform the reservation service (separate deployment) that a fare changed.
4. Analytics team wants fire-and-forget hooks on 12 domain events; teams attach/detach their listeners independently; nobody cares about ordering; a dead listener must never hurt the others.
5. Payment events must reach the ledger exactly once, with retry, surviving a JVM crash mid-delivery.

The traps to catch: (1) is a WORKFLOW — ordered steps with abort semantics; observers are by contract independent, order-agnostic, failure-isolated, so Observer is wrong even though "things react to an event" sounds right. (3) crosses a process boundary — in-process Observer can't; the same IDEA becomes a message broker (your day job's Service Bus topics ARE distributed Observer). (5) needs durability guarantees an in-memory list can never give.

**B) Misuse post-mortem — Observer applied where it didn't belong:**

A team rebuilt their entire checkout as CDI events: `OrderPlaced` fires, seven `@Observes` methods react, some of those fire further events, three levels deep. Six months later: checkout latency +900ms (all listeners synchronous, sequential); a bug hunt takes days because NO ONE can read the control flow — grep finds no caller, the "flow" exists only at runtime; one listener throwing rolled back the whole transaction including the payment that had already been captured externally; and investigation listeners attached by the fraud team were never detached — the classic **lapsed listener** leak, subscriber list growing forever, GC can't collect them.

1. Name each failure with its proper term: hidden control flow / event spaghetti; synchronous fan-out latency; transactional coupling of "independent" observers; lapsed listener memory leak.
2. State the design-time signal the team missed: their listeners were NOT independent — checkout steps depend on each other's success and order. Dependent steps = orchestration (explicit calls in a service method), not notification.
3. Prescribe the fix boundary: which parts stay events (analytics, email — genuinely independent, fire-and-forget, async) and which return to an explicit orchestrator (inventory, payment). One sentence on why "Observer for everything" and "Observer for nothing" are both wrong — the pattern earns its place exactly where the one-to-many is real and the many are truly independent.

---

*Working code goes wherever you put this pattern's project, diagrams on paper. Grading /10 per part, honest FAANG machine-coding bar: correctness, extensibility proven not claimed, narration of trade-offs unprompted.*
