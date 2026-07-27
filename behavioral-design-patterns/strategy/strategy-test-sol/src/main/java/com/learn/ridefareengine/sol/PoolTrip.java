package com.learn.ridefareengine.sol;

public class PoolTrip implements TripTypeFareCalculator {
    @Override
    public double calculateTripTypeFare(Trip trip) {
        return Math.max(15.0 , 0.7 * (10 + 2.5 * trip.distance() + .5 * trip.duration()));
    }
}
