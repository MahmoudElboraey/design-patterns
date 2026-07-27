package com.learn.ridefareengine.best;

public class NoPromo implements PromoStrategy {
    @Override
    public double apply(double fare, Trip trip) {
        return fare;
    }
}
