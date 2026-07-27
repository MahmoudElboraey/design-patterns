package com.learn.ridefareengine.best;

public class PoolFare implements RideTypeFare {

    private static final double DISCOUNT = 0.70;
    private static final double FLOOR = 15.0;

    private final EconomyFare economyFare = new EconomyFare();

    @Override
    public double base(Trip trip) {
        double discounted = DISCOUNT * economyFare.base(trip);
        return Math.max(FLOOR, discounted);
    }
}
