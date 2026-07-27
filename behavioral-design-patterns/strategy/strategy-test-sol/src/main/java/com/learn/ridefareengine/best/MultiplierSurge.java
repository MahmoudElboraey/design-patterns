package com.learn.ridefareengine.best;

public class MultiplierSurge implements SurgeStrategy {

    private final double factor;

    public MultiplierSurge(double factor) {
        this.factor = factor;
    }

    @Override
    public double apply(double fare, Trip trip) {
        return fare * factor;
    }
}
