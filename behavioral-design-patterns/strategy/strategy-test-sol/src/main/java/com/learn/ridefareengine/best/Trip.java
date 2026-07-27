package com.learn.ridefareengine.best;

public record Trip(
        RideType rideType,
        double distanceKm,
        double durationMin,
        double demandRatio,
        String promoCode) {
}
