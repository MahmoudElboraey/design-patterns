# Machine Coding Round — Memento Pattern in Production

> Format mirrors a real LLD / machine-coding interview (Google/Netflix/Figma style):
> one build problem, one production refactor, one hardening round, UML + trade-offs.
> Scope: Memento + UML + SOLID, with Observer/Strategy/Template cross-questions.
> The tell for this pattern: "we need undo/rollback/restore, but exposing the
> object's internals to save them would break encapsulation."
> Budget like a real round: ~90 min total.

---

## Part A — Machine Coding: Document Editor with Undo/Redo (the classic LLD problem)

**Q1.** You're building the editing core of a document tool (think Google Docs
single-user mode / Notion). This is THE canonical memento interview problem —
interviewers at every tier ask it.

The editor's state: text content, cursor position, and current formatting
(font size + bold flag). All three must save/restore TOGETHER — restoring text
without cursor position is a broken undo.

**Functional requirements:**

1. `type(text)`, `moveCursor(pos)`, `setFormatting(...)` mutate the editor.
2. **Undo** restores the editor to the state before the last mutation.
   **Redo** re-applies an undone mutation. Standard editor semantics.
3. **The timeline rule:** undo twice, then make a NEW edit → redo must become
   impossible (the old future is gone). State this rule in a comment and prove
   it in the demo.
4. **Encapsulation is the whole point of the pattern:** the history holder must
   NOT be able to read or modify the saved state's fields. If your history
   class can call `memento.getText()`, you've failed the core constraint.
   Java gives you two idiomatic ways to enforce this — use one, name both.
5. Next sprint (do NOT implement, design must absorb with zero changes to the
   editor or history core):
   - **named checkpoints**: "save this state as 'before-client-review', jump
     back to it anytime" (Photoshop's Snapshots feature — Part C tells you why)
   - **persist history to disk** so undo survives a restart

**Non-functional expectations (state how design meets them):**

- **Memory is bounded.** History must cap at N states and drop the oldest —
  unbounded undo history is a real OOM (Part C incident a). Name the data
  structure that makes drop-oldest O(1).
- Saved states must be **immune to later mutation** — if the editor mutates a
  field after a save, the saved state must not change with it. Say which Java
  feature guarantees this for free.
- One-sentence thread-safety statement: single-threaded editor is fine, but SAY
  it, and say what breaks first if two threads mutate concurrently.

**Deliverables:**
- Working Java: Originator (editor), Memento, Caretaker (history) — name the
  roles in comments
- A `main` demo: several edits → undo → undo → redo → verify state correct at
  each step → new edit after undo → prove redo is dead → fill history past cap
  N and show oldest dropped
- Short paragraph: who creates the memento and WHY it must be that role and not
  the other one; where the pattern's encapsulation boundary sits in your code

---

## Part B — Production Refactor: the getter-scraping history manager

**Q2.** Real production smell — state saving WITHOUT memento. A teammate built
draft-recovery for a booking flow (your day job's shape: multi-step flight
booking form) like this:

```java
public class BookingDraft {
    private List<Passenger> passengers = new ArrayList<>();
    private Map<String, String> selectedSeats = new HashMap<>();
    private String contactEmail;
    private int currentStep;

    public List<Passenger> getPassengers() { return passengers; }
    public Map<String, String> getSelectedSeats() { return selectedSeats; }
    public String getContactEmail() { return contactEmail; }
    public int getCurrentStep() { return currentStep; }

    public void setPassengers(List<Passenger> p) { this.passengers = p; }
    public void setSelectedSeats(Map<String, String> s) { this.selectedSeats = s; }
    public void setContactEmail(String e) { this.contactEmail = e; }
    public void setCurrentStep(int s) { this.currentStep = s; }
}

public class DraftHistoryManager {
    private final Deque<Object[]> history = new ArrayDeque<>();

    public void saveState(BookingDraft draft) {
        history.push(new Object[] {
                draft.getPassengers(),          // stores the LIVE list reference
                draft.getSelectedSeats(),       // stores the LIVE map reference
                draft.getContactEmail(),
                draft.getCurrentStep()
        });
    }

    public void restoreLast(BookingDraft draft) {
        Object[] state = history.pop();
        draft.setPassengers((List<Passenger>) state[0]);
        draft.setSelectedSeats((Map<String, String>) state[1]);
        draft.setContactEmail((String) state[2]);
        draft.setCurrentStep((Integer) state[3]);
    }
}
```

Users report: "I clicked restore and NOTHING changed." Also, adding a new field
to `BookingDraft` last sprint required editing `DraftHistoryManager` in two
places, and someone forgot one — seats silently stopped being restored for a week.

**Tasks:**

a) First, the bug: explain EXACTLY why restore appears to do nothing for
passengers and seats (trace the references — what do the draft AND the saved
state point at after `saveState` + one more mutation?). Name the underlying
mistake with its proper term.

b) Refactor to Memento: the draft creates its own memento, the memento is
immutable and opaque to the history manager, restore goes through the draft.
Show all three classes. The `Object[]` with index-casting must die — say what
you replaced it with and which compile-time guarantee you gained.

c) Guaranteed interviewer follow-ups — answer before asked:
   - Why must the ORIGINATOR create the memento instead of the caretaker
     reading getters? (Two reasons: one about encapsulation, one about the
     "forgot one field" incident.)
   - Your memento stores `List<Passenger>` — shallow reference, unmodifiable
     wrapper, or deep copy? Pick, and say what each level does and does not
     protect against (hint: `Collections.unmodifiableList` over a live list
     protects nothing here; a copied list of MUTABLE Passenger objects is still
     leaky if someone mutates a Passenger).
   - Those public setters for every field — what do they do to the class's
     invariants, and does your refactor still need them?

d) Name every SOLID principle your refactor improves, one sentence each —
unprompted narration, still drilling the habit.

---

## Part C — Hardening Round: production pitfalls

**Q3.** Your Part A editor ships. Three incidents. For each: name the root
cause (proper term where one exists), point at the vulnerable line(s) in YOUR
code, show the fix:

a) **OOM after long sessions.** Power users edit for hours; heap dump shows
tens of thousands of memento objects, each holding the FULL document text —
a 2 MB document snapshotted on every keystroke. Give THREE independent fixes
and the trade-off of each:
   1. the bound you (hopefully) already built — what number, and what's the UX
      cost of choosing it too low?
   2. **coalescing** — don't snapshot every keystroke; define the boundary you
      snapshot on instead (editors really do this)
   3. **incremental/delta mementos** — store only what changed; what new
      complexity does restore gain, and why is "full snapshot with a bound"
      still the recommended STARTING point before profiling proves you need
      deltas?

b) **"Undo restored a corrupted state."** A memento was built like
`new Memento(this.paragraphs)` where `paragraphs` is a mutable `List` — the
editor kept appending to the SAME list after the save. Undo "restores" a state
that contains edits made AFTER the save point. Name the bug (same family as
Part B-a), state the iron rule for memento fields, and show the fix — then
connect it to the concurrency doc rule you know: what property makes a memento
safe to share across threads for free?

c) **The fork in the timeline.** QA files: "undo 3 times, type a character,
then redo — the editor mixed old and new content." Your redo stack survived a
new edit. Explain why redo-after-new-edit is semantically meaningless (what
timeline would you even be redoing INTO?), show the one-line fix and where it
lives, and name which editor you know of that gets this right (all of them —
it's the universal convention).

**Q4.** Snapshots vs deltas vs command-log — the design-space question:

a) Three ways to build undo: (1) memento snapshots of state, (2) Command
pattern with `undo()` inverse operations, (3) event-sourcing style — persist
every change event, rebuild state by replay (your Service Bus world: this is
what a Kafka/EventStore aggregate does; even your search flow rebuilds view
state from stored responses). For each: what does it cost in memory, what does
it cost in code complexity, and what does it uniquely enable? Fill the 3×3 honestly.

b) Git question (interviewers love it): is a git commit a diff or a snapshot?
Answer precisely: git's object model stores each commit as a full SNAPSHOT of
the tree (memento-style), and separately compresses storage with pack-file
deltas underneath. Why is "snapshot semantics, delta storage" the best of both —
and which of your Q3-a fixes is the same idea?

**Q5.** Real post-mortems — real companies, documented engineering write-ups.
Analyze through the memento lens:

a) **Figma — undo in multiplayer.** Figma's engineering blog: "undo in a
multiplayer environment is inherently confusing. If other people have edited
the same objects that you edited and then undo, what should happen?" A naive
global memento stack would mean YOUR undo restores a snapshot that wipes out
OTHER people's edits made since. Figma's solution: undo/redo are PER-AUTHOR
operations expressed as new deltas (not time-travel on shared history), an
undo actively REWRITES the redo history at undo time, and their guiding
invariant is: "if you undo a lot, copy something, and redo back to the present,
the document should not change."

   Answer:
   - Precisely why does the classic memento (restore full past state) break
     down with 2+ concurrent editors? Name what gets destroyed.
   - Figma's per-author deltas are closer to which of Q4-a's three approaches?
     What does that tell you about memento's honest boundary — what property
     must the system have for state-snapshot undo to be the right tool?
   - Your Part A editor is single-user. Next year PM says "make it
     collaborative." One paragraph: what survives of your design, what must be
     rebuilt, and why saying THIS in an interview (knowing your pattern's
     breaking point) scores more than the implementation itself.

b) **Adobe Photoshop — history states as a memory budget.** Real numbers from
Adobe's own performance docs: every history state consumes RAM/scratch disk; on
a large multi-layer composite ONE state can be hundreds of megabytes; the
default cap is 50 states (configurable up to 1,000); when RAM runs out,
Photoshop spills to scratch disk and performance falls off a cliff. Adobe also
ships a separate "Snapshots" feature — named checkpoints you create manually at
milestones, outside the rolling history cap.

   Answer:
   - Map every mechanism to your Part A design: rolling cap = ?, Snapshots = ?
     (you designed this as the "next sprint" trap), scratch-disk spill = which
     tier of storage for which mementos?
   - Photoshop chose 50 as default, not 1,000. Write the two-line memory math
     an interviewer wants: states × size-per-state = budget; then state your
     editor's equivalent policy and what you'd measure to tune it.
   - Why does the "named snapshot" feature exist SEPARATELY from the undo
     history instead of being "just a history state you keep"? (Think: what
     does the rolling cap do to anything living inside it?)

---

## Part D — UML + Trade-offs

**Q6.** Full UML class diagram of your Part A design (hand-drawn like before).
Graded hard on:

- The THREE roles boxed and labeled: Originator, Memento, Caretaker
- **Creation arrow:** who creates the memento — dashed dependency arrow from
  Originator to Memento labeled «create». Caretaker must have NO arrow into
  Memento's internals — show it holds mementos without seeing inside (this is
  the diagram's way of stating the narrow interface)
- Caretaker → Memento: association with diamond — which diamond, defended, and
  multiplicity (`1 → 0..*` — the history holds many states)
- Caretaker → Originator: how does undo flow back? Show the call direction
- Nested-class notation if your Memento is an inner class of the Originator
  (the +-in-circle anchor, or a note — either accepted, absence not)
- SOLID narration written next to the diagram, unprompted — and one line on
  which SOLID principle the PATTERN ITSELF protects (hint: what would N
  external classes reading getters do to the editor's freedom to change its
  fields?)

**Q7.** Judgment questions, 2–3 sentences each:

a) **Memento vs Command-with-undo():** both give you undo. When do inverse
operations beat snapshots, and when are snapshots the only correct choice
(hint: some operations have no computable inverse — name one from a real editor).
b) **Memento vs Observer:** a teammate says "just publish a StateChanged event
on every edit and keep the events — that's our history." He's reinvented which
Q4-a approach? What does his version cost on RESTORE that yours doesn't, and
when is his genuinely better?
c) **Memento vs Prototype:** both copy objects. One sentence each on intent —
what is the copy FOR in each pattern — and why memento's copy must be opaque
while prototype's must not be.
d) **When to refuse memento entirely:** give two concrete signals that
undo/restore should NOT be built with snapshots (think: state size vs change
size, state that's trivially recomputable, state owned by someone else — pick two).

**Q8.** The evolution ladder (YAGNI judgment). Draft recovery for a form, three stages:

```java
// Stage 1 — single last-good state
public class DraftService {
    private DraftMemento lastGood;
    // on risky action: lastGood = draft.save();  on failure: draft.restore(lastGood);
}

// Stage 2 — undo/redo stacks, bounded
public class DraftHistory {
    private final Deque<DraftMemento> undo = new ArrayDeque<>();
    private final Deque<DraftMemento> redo = new ArrayDeque<>();
    private static final int MAX = 50;
    // push on edit (evict oldest past MAX), pop to undo, timeline rule enforced
}

// Stage 3 — persisted, delta-compressed, named checkpoints
// mementos serialized to Redis/disk with schema version, deltas between
// snapshots, named checkpoint registry, survives restart
```

For each requirement, pick the CHEAPEST stage that satisfies it, and say what
NEW capability each stage buys over the previous:

a) "If payment fails mid-booking, roll the form back to before payment started.
That's it."
b) "Users demand full undo/redo while filling the form, standard editor feel,
sessions are short-lived."
c) "Drafts must survive app restarts and deploys; support named 'saved
versions' a user returns to days later; some drafts are huge."
d) A teammate builds Stage 3 for requirement (a) "so we're future-proof." Name
the principle violated and the concrete costs paid today — including the ONE
cost unique to Stage 3 here that neither the Observer nor Strategy Stage-3
overbuilds had (hint: serialized state + next deploy changes the class = what
production incident?).

---

## Part E — Pattern-or-Not Gauntlet — /10

### Q9. When Memento fits, when it doesn't, and when it burns you

**A) Five scenarios — verdict (Memento / not Memento / different tool) + one-line justification each. Three contain traps.**

1. In-session undo/redo for a document editor; history dies with the session.
2. Compliance requires a permanent audit trail: who changed what, when, queryable for seven years.
3. Roll a single config value back to its last known-good on failed health check.
4. Restore full service state after a crash, across deploys and node replacements.
5. Collaborative editing (multiple users, one document) needs per-user undo.

The traps to catch: (2) is event sourcing / audit log territory — mementos are OPAQUE (caretaker can't query inside them) and in-memory-bounded; an audit trail needs queryable, attributed, durable events — opacity is Memento's feature and audit's dealbreaker. (4) is persistence/recovery — snapshots that outlive the process must be schema-versioned, serialized DTOs in durable storage; an in-memory Memento dies with the JVM by design. (5) global snapshots undo OTHER users' work — Figma's documented answer is per-author operation deltas, not whole-state mementos. And (3) is the opposite trap: a full Memento stack for one field is overbuild — a `lastGood` field is Stage 1 of your own ladder.

**B) Misuse post-mortem — Memento applied where it didn't belong:**

A team added undo to a booking-draft editor by snapshotting the whole JPA ENTITY per keystroke: serialized the entity graph to Redis. Three incidents followed. (1) The entity carried lazy-loading proxies and a reference to its persistence session — serialization dragged half the ORM into Redis, payloads were megabytes, and deserialization on another node exploded on missing proxy classes. (2) A deploy renamed one field; every stored snapshot became undeserializable — all users lost undo simultaneously, some restore attempts threw and wiped the CURRENT draft. (3) A user restored a 10-minute-old snapshot while a payment webhook had meanwhile updated the same row — the restore silently overwrote the webhook's write: a lost update.

1. Name each failure: snapshotting LIVE infrastructure-coupled objects instead of self-contained value state (the aliasing sin from Part C, escalated — you didn't just alias a list, you aliased the ORM); unversioned serialized state hitting a schema change (your Stage-3 ladder cost, realized); restore-as-blind-write ignoring concurrency (lost update — name the race shape from your concurrency doc).
2. State the design-time signals missed: a memento must capture a CLOSED, immutable, self-contained value snapshot — if you can't deep-copy it into plain data, it isn't memento material yet; and any snapshot that leaves the process boundary is no longer the GoF pattern, it's a persistence format that needs an owned schema and versioning.
3. Prescribe: dedicated snapshot DTO (plain fields, no entity, schema you own and version); snapshot the FORM state not the persisted row; restore goes through the same optimistic-locking write path as any edit (version check, reject stale). One sentence on the boundary rule: Memento inside the process for UX undo; anything durable or cross-node graduates to explicitly designed persistence.

---

*Working code goes wherever you put this pattern's project, diagrams on paper.
Grading /10 per part, honest FAANG machine-coding bar: correctness,
extensibility proven not claimed, narration of trade-offs unprompted.*
