package com.learn.productionrefactor.best;

import java.util.List;

public class Main {

    public static void main(String[] args) {
        OrderEvents events = new OrderEvents();
        events.register(new EmailListener());
        events.register(new AnalyticsListener());
        events.register(new LoyaltyListener());
        events.register(event -> {
            throw new RuntimeException("push token invalid");
        });

        OrderService service = new OrderService(new PaymentGateway(), new WarehouseClient(), events);
        User user = new User("u1", "mahmoud@test.com");
        Cart cart = new Cart(List.of(new Item("BOOK-1", 2)));

        System.out.println("--- place order: core runs, all listeners fire, broken one isolated ---");
        Order order = service.placeOrder(cart, user);
        System.out.println("order placed: id=" + order.getId() + " total=" + order.getTotal());

        System.out.println("\n--- A/B test: drop analytics at runtime, core untouched ---");
        AnalyticsListener analytics = new AnalyticsListener();
        events.register(analytics);
        events.unregister(analytics);

        System.out.println("\n--- core abort: empty cart -> warehouse fails -> NO side effects fire ---");
        try {
            service.placeOrder(new Cart(List.of()), user);
        } catch (Exception e) {
            System.out.println("order failed as expected: " + e.getMessage());
        }
    }
}
