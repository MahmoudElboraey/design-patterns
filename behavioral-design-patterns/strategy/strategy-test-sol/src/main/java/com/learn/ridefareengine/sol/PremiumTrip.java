package com.learn.ridefareengine.sol;

public class PremiumTrip implements TripTypeFareCalculator {
    @Override
    public double calculateTripTypeFare(Trip trip) {
        return 25 + 4 * trip.distance() + trip.duration();
    }
}
