package com.learn.productionrefactor.best;

public class WarehouseClient {

    public void reserveStock(Order order) {
        if (order.getItems().isEmpty()) {
            throw new IllegalStateException("no items to reserve for order " + order.getId());
        }
        System.out.println("[WAREHOUSE] reserved stock for order " + order.getId());
    }
}
