package com.learn.openclosed;

public class PremiumCustomer implements CustomerDiscount {
    @Override
    public double calculate(Order order) {
        return order.total() * 0.10;
    }
}
