package com.learn.productionrefactor.best;

public class BankTransferFee implements FeeCalculator {

    @Override
    public double calculateFee(Order order) {
        if ("EUR".equals(order.currency())) {
            return 0.0;
        }
        return 5.0;
    }
}
