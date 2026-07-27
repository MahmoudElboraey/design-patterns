package com.learn.ridefareengine.best;

public class EconomyFare implements RideTypeFare {
    @Override
    public double base(Trip trip) {
        return 10 + 2.5 * trip.distanceKm() + 0.5 * trip.durationMin();
    }
}
