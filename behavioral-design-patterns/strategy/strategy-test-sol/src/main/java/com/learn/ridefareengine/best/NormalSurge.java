package com.learn.ridefareengine.best;

public class NormalSurge implements SurgeStrategy {
    @Override
    public double apply(double fare, Trip trip) {
        return fare;
    }
}
