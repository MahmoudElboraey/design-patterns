package com.learn.productionrefactor.best;

public class CryptoFee implements FeeCalculator {

    @Override
    public double calculateFee(Order order) {
        return order.total() * 0.01;
    }
}
