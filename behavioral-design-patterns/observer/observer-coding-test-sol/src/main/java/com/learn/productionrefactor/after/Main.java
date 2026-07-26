package com.learn.productionrefactor.after;

import com.learn.productionrefactor.before.Cart;

public class Main {

    public static void main(String[] args) {
        OrderService orderService = new OrderService();

        PaymentService paymentService = new PaymentService();
        AnalyticsService analyticsService = new AnalyticsService();
        LoyaltyService loyaltyService = new LoyaltyService();
        WarehouseService warehouseService = new WarehouseService();

        orderService.subscribeToOrderCreation(paymentService);
        orderService.subscribeToOrderCreation(analyticsService);
        orderService.subscribeToOrderCreation(loyaltyService);
        orderService.subscribeToOrderCreation(warehouseService);

        User user = new User();
        user.setId("u1");
        user.setEmail("mahmoud@test.com");

        System.out.println("--- placing order with all observers ---");
        Order order = orderService.placeOrder(new Cart(), user);
        System.out.println("order placed: id=" + order.getId() + " total=" + order.getTotal());

        System.out.println("\n--- unsubscribing analytics, placing second order ---");
        orderService.unsubscribeFromOrderCreation(analyticsService);
        orderService.placeOrder(new Cart(), user);
    }
}
