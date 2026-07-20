package com.learn;

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

    private Order createOrder(Cart cart , User user){
        return null;
    }

    private void chargePayment(Order order){

    }
}
