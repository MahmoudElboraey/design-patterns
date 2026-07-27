package com.learn.productionrefactor.best;

public class PaypalFee implements FeeCalculator {

    @Override
    public double calculateFee(Order order) {
        return order.total() * 0.034 + 0.35;
    }
}
