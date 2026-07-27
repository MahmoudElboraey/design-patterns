package com.learn.productionrefactor.after;

public class PaypalFee implements FeeCalculator {
    @Override
    public double calculateFee(Order order) {
        return order.getTotal() * 0.034 + 0.35;
    }
}
