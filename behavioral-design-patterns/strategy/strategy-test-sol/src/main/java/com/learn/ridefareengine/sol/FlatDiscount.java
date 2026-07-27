package com.learn.ridefareengine.sol;

public class FlatDiscount implements PromoCodeDiscountCalculator {

    private final double flatOff;

    public FlatDiscount(double flatOff) {
        this.flatOff = flatOff;
    }

    @Override
    public double calculateFareWithDiscount(double amount) {
        return Math.max(0, amount - flatOff);
    }
}
