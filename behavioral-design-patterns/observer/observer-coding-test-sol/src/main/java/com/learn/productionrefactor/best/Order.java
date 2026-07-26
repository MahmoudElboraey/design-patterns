package com.learn.productionrefactor.best;

import java.util.List;

public class Order {

    private final String id;
    private final double total;
    private final List<Item> items;

    public Order(String id, double total, List<Item> items) {
        this.id = id;
        this.total = total;
        this.items = items;
    }

    public String getId()        { return id; }
    public double getTotal()     { return total; }
    public List<Item> getItems() { return items; }
}
