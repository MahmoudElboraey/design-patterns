package com.learn.openclosed;

public class VIPCustomer implements CustomerDiscount {
    @Override
    public double calculate(Order order) {
        return order.total() * 0.20;
    }
}
