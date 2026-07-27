package com.learn.productionrefactor.after;

public class BankTransferFee implements FeeCalculator {
    @Override
    public double calculateFee(Order order) {
        if ("EUR".equals(order.getCurrency())) {
            return 0.0;                // SEPA is free
        }
        return 5.0;
    }
}
