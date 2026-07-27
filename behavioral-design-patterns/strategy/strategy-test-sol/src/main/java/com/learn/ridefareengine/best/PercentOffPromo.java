package com.learn.ridefareengine.best;

public class PercentOffPromo implements PromoStrategy {

    private final double percent;

    public PercentOffPromo(double percent) {
        this.percent = percent;
    }

    @Override
    public double apply(double fare, Trip trip) {
        return fare * (100 - percent) / 100;
    }
}
