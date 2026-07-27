package com.learn.ridefareengine.sol;

public class NoDiscount implements PromoCodeDiscountCalculator {
    @Override
    public double calculateFareWithDiscount(double amount) {
        return amount ;
    }
}
