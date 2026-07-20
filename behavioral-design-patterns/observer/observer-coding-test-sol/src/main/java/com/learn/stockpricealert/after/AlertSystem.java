package com.learn.stockpricealert.after;

import java.util.HashMap;
import java.util.Map;

public class AlertSystem {

    private final Map<String, Stock> stocks = new HashMap<>();

    public void addStock(Stock stock) {
        stocks.put(stock.getSymbol(), stock);
    }

    public void updatePrice(String symbol, double newPrice) {
        Stock stock = stocks.get(symbol);
        if (stock == null) {
            throw new IllegalArgumentException("unknown stock symbol: " + symbol);
        }
        stock.updatePrice(newPrice);
    }

    public static void main(String[] args) {

        DeliveryChannelFactory channelFactory = new DeliveryChannelFactory();
        AlertSystem alertSystem = new AlertSystem();


        Stock apple = new Stock("Apple", 195.0, "AAPL");
        Stock tesla = new Stock("Tesla", 160.0, "TSLA");
        alertSystem.addStock(apple);
        alertSystem.addStock(tesla);


        User alice = new User(channelFactory.getDeliveryChannel("EMAIL"), "Alice", "0100000001", "alice@mail.com");
        User bob = new User(channelFactory.getDeliveryChannel("SMS"), "Bob", "0100000002", "bob@mail.com");


        System.out.println("--- Alice subscribes: AAPL goes above 200 (EMAIL) ---");
        apple.subscribe(EventType.ABOVE_200, alice);

        System.out.println("--- Bob subscribes: TSLA goes below 150 (SMS) ---");
        tesla.subscribe(EventType.BELOW_150, bob);

        System.out.println("\n--- market feed: AAPL -> 205 (Alice notified by EMAIL) ---");
        alertSystem.updatePrice("AAPL", 205.0);

        System.out.println("\n--- market feed: TSLA -> 140 (Bob notified by SMS) ---");
        alertSystem.updatePrice("TSLA", 140.0);


        System.out.println("\n--- Bob cancels TSLA BELOW_150 subscription ---");
        tesla.removeSubscriber(EventType.BELOW_150, bob);

        System.out.println("\n--- market feed: TSLA -> 130 (nobody notified, Bob cancelled) ---");
        alertSystem.updatePrice("TSLA", 130.0);
    }
}
