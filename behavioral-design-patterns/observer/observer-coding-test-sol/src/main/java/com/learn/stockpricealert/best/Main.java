package com.learn.stockpricealert.best;

public class Main {

    public static void main(String[] args) {
        Stock apple = new Stock("AAPL", 195.0);
        Stock tesla = new Stock("TSLA", 160.0);

        User alice = new User("Alice", "alice@mail.com", "0100000001");
        User bob   = new User("Bob",   "bob@mail.com",   "0100000002");
        User carol = new User("Carol", "carol@mail.com", "0100000003");

        DeliveryChannel email = new EmailChannel();
        DeliveryChannel sms   = new SmsChannel();

        AlertRule above200 = new ThresholdRule(ThresholdRule.Direction.ABOVE, 200);
        AlertRule below150 = new ThresholdRule(ThresholdRule.Direction.BELOW, 150);

        Subscription aliceSub = new Subscription(alice, above200, email);
        Subscription bobSub   = new Subscription(bob,   below150, sms);
        Subscription carolSub = new Subscription(carol, above200, new BrokenPushChannel());

        apple.subscribe(carolSub);
        apple.subscribe(aliceSub);
        tesla.subscribe(bobSub);

        System.out.println("--- AAPL -> 205 (Carol's push THROWS, Alice still gets email) ---");
        apple.updatePrice(205.0);

        System.out.println("\n--- TSLA -> 140 (Bob gets SMS) ---");
        tesla.updatePrice(140.0);

        System.out.println("\n--- Bob cancels ---");
        tesla.unsubscribe(bobSub);

        System.out.println("\n--- TSLA -> 130 (silence, Bob cancelled) ---");
        tesla.updatePrice(130.0);

        System.out.println("\n--- 'above 210' request: no new class, no core edit, just new data ---");
        apple.subscribe(new Subscription(alice,
                new ThresholdRule(ThresholdRule.Direction.ABOVE, 210), email));
        apple.updatePrice(212.0);
    }
}
