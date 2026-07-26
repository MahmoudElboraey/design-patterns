package com.learn.productionrefactor.after;

public class WarehouseService implements OrderCreation {
    @Override
    public void onOrderCreated(Order order, User user) {
        System.out.println("order stock for " + order.getId() + " has been decreased");

    }
}
