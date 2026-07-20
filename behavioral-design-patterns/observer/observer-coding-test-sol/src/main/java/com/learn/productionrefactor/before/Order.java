package com.learn;

import java.util.List;

public class Order {
    private String id;
    private double total;
    private List<Item> items;


    public Order(String id, double total) {
        this.id = id;
        this.total = total;
    }

    public double getTotal() {
        return total;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public List<Item> getItems() {
        return this.items;
    }
}
