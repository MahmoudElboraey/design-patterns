package com.learn.productionrefactor.best;

public class CheckoutService {

    private final FeeCalculatorFactory factory;

    public CheckoutService(FeeCalculatorFactory factory) {
        this.factory = factory;
    }

    public double calculateProcessingFee(Order order, String paymentMethod) {
        return factory.getFeeCalculator(paymentMethod).calculateFee(order);
    }
}
