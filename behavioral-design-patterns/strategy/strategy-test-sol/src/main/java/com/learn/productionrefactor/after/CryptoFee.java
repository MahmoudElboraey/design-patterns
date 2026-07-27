package com.learn.productionrefactor.after;

public class CryptoFee implements FeeCalculator {
    @Override
    public double calculateFee(Order order) {
        return order.getTotal() * 0.01;
    }
}
