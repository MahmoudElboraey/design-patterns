package com.learn.ridefareengine.best;

public class PremiumFare implements RideTypeFare {
    @Override
    public double base(Trip trip) {
        return 25 + 4 * trip.distanceKm() + 1 * trip.durationMin();
    }
}
