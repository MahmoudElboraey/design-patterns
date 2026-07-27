package com.learn.ridefareengine.sol;

public class FullDiscountWithCapacity implements PromoCodeDiscountCalculator {

    private double capacity;
    public FullDiscountWithCapacity(double capacity) {
        this.capacity = capacity;
    }
    @Override
    public double calculateFareWithDiscount(double amount) {
        return amount > capacity ? amount : 0;
    }
}
