package com.learn.ridefareengine.sol;

public class EconomyTrip implements TripTypeFareCalculator {
    @Override
    public double calculateTripTypeFare(Trip trip) {
       return 10 + 2.5 * trip.distance() + .5 * trip.duration();
    }
}
