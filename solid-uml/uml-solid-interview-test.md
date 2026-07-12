# Java Interview Test — UML & SOLID

> Answer all questions. Scope: UML class-diagram relationships + SOLID principles. No design patterns.

## Part A — Reading UML (Q1–Q2)

**Q1.** Explain the difference between these four UML arrows, and give a one-line Java code example for each (how the relationship appears in code):

```
a)  ClassA ──────▷ ClassB     (solid line, hollow triangle)
b)  ClassA ┄┄┄┄┄▷ ClassB     (dashed line, hollow triangle)
c)  ClassA ◆──── ClassB      (filled diamond on ClassA side)
d)  ClassA ┄┄┄┄> ClassB      (dashed line, open arrow)
```

**Q2.** Look at this diagram:

```
┌─────────────┐        ┌──────────────┐
│   Hospital  │◆───────│  Department  │
└─────────────┘        └──────────────┘
                              │
                              ◇
                              │
                       ┌──────────────┐
                       │    Doctor    │
                       └──────────────┘
```

A junior developer says: "If we delete the Hospital, the Departments are deleted too.
And if we delete a Department, all its Doctors are deleted too."
Is he right, wrong, or half right? Explain using the correct UML terms.

## Part B — Code to UML (Q3)

**Q3.** For this Java code, list EVERY UML relationship you would draw, with direction and arrow type (e.g. "PaymentService ──▷ X : inheritance"):

```java
public interface Notifier {
    void send(String message);
}

public class EmailNotifier implements Notifier {
    public void send(String message) { /* ... */ }
}

public class Order {
    private final List<OrderItem> items = new ArrayList<>();
    private Customer customer;
}

public class OrderService {
    private final Notifier notifier;

    public OrderService(Notifier notifier) {
        this.notifier = notifier;
    }

    public Invoice checkout(Order order) {
        Invoice invoice = new Invoice(order);
        notifier.send("Order placed");
        return invoice;
    }
}
```

## Part C — SOLID (Q4–Q5)

**Q4.** Each snippet below violates exactly ONE SOLID principle. Name the principle and explain the violation in 1–2 sentences. (Don't fix yet — just diagnose.)

**Snippet 1:**
```java
public class ReportManager {
    public ReportData collectData() { /* query DB */ }
    public String formatAsHtml(ReportData d) { /* build HTML */ }
    public void saveToDisk(String html) { /* file IO */ }
    public void emailToBoss(String html) { /* SMTP */ }
}
```

**Snippet 2:**
```java
public class Rectangle {
    protected int width, height;
    public void setWidth(int w)  { this.width = w; }
    public void setHeight(int h) { this.height = h; }
    public int area() { return width * height; }
}

public class Square extends Rectangle {
    @Override public void setWidth(int w)  { this.width = w; this.height = w; }
    @Override public void setHeight(int h) { this.width = h; this.height = h; }
}
```

**Snippet 3:**
```java
public class DiscountCalculator {
    public double calculate(Order order, String customerType) {
        if (customerType.equals("REGULAR")) return order.total() * 0.05;
        else if (customerType.equals("PREMIUM")) return order.total() * 0.10;
        else if (customerType.equals("VIP")) return order.total() * 0.20;
        return 0;
    }
}
```

**Snippet 4:**
```java
public interface Machine {
    void print(Document d);
    void scan(Document d);
    void fax(Document d);
    void staple(Document d);
}

public class SimplePrinter implements Machine {
    public void print(Document d) { /* works */ }
    public void scan(Document d)  { throw new UnsupportedOperationException(); }
    public void fax(Document d)   { throw new UnsupportedOperationException(); }
    public void staple(Document d){ throw new UnsupportedOperationException(); }
}
```

**Snippet 5:**
```java
public class UserService {
    private final MySqlUserRepository repository = new MySqlUserRepository();

    public User register(String email) {
        return repository.insert(new User(email));
    }
}
```

**Q5.** Pick Snippet 3 from Q4 and REWRITE it in Java so it no longer violates the principle. Then describe (in words) what the UML class diagram of your solution looks like — which boxes, which arrows.

## Part D — Design question (Q6)

**Q6.** Interview classic: You're designing a payment module. Requirements today: pay by credit card and PayPal. Next quarter: Apple Pay and crypto will be added. Marketing also wants a receipt sent after every successful payment — today by email, later maybe SMS.

Describe your design: what interfaces/classes you'd create, how they connect (UML arrow types), and name which SOLID principles your design satisfies and HOW. No full code needed — structure + reasoning.
