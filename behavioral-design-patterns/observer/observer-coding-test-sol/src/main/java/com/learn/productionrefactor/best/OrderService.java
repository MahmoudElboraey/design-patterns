package com.learn.productionrefactor.best;

import java.util.List;
import java.util.UUID;

public class OrderService {

    private final PaymentGateway payment;
    private final WarehouseClient warehouse;
    private final OrderEvents events;

    public OrderService(PaymentGateway payment, WarehouseClient warehouse, OrderEvents events) {
        this.payment = payment;
        this.warehouse = warehouse;
        this.events = events;
    }

    public Order placeOrder(Cart cart, User user) {
        Order order = createOrder(cart, user);

        warehouse.reserveStock(order);
        payment.charge(order);

        events.publish(new OrderPlacedEvent(order, user));

        return order;
    }

    private Order createOrder(Cart cart, User user) {
        List<Item> items = cart.items();
        double total = items.size() * 100.0;
        return new Order(UUID.randomUUID().toString().substring(0, 8), total, items);
    }
}
