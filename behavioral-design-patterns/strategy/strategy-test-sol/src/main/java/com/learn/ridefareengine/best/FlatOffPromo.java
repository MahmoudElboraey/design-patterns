package com.learn.ridefareengine.best;

public class FlatOffPromo implements PromoStrategy {

    private final double flatOff;

    public FlatOffPromo(double flatOff) {
        this.flatOff = flatOff;
    }

    @Override
    public double apply(double fare, Trip trip) {
        return Math.max(0, fare - flatOff);
    }
}
