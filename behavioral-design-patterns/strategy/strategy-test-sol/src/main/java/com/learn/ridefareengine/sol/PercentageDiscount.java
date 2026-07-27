package com.learn.ridefareengine.sol;

public class PercentageDiscount implements PromoCodeDiscountCalculator {

    private final double percentage;
    PercentageDiscount(double percentage) {
        this.percentage = percentage;
    }
    @Override
    public double calculateFareWithDiscount(double amount) {
        return (100 - percentage) / 100  * amount;
    }
}
