package com.learn.openclosed;

public class RegularCustomer implements CustomerDiscount {
    @Override
    public double calculate(Order order) {
        return order.total() * 0.05;
    }
}
