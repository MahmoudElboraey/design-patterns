package com.learn.ridefareengine.best;

public class FareService {

    private final RideTypeFareRegistry rideTypeRegistry;
    private final SurgeSelector surgeSelector;
    private final PromoRegistry promoRegistry;

    public FareService(RideTypeFareRegistry rideTypeRegistry,
                       SurgeSelector surgeSelector,
                       PromoRegistry promoRegistry) {
        this.rideTypeRegistry = rideTypeRegistry;
        this.surgeSelector = surgeSelector;
        this.promoRegistry = promoRegistry;
    }

    public double calculateFare(Trip trip) {
        RideTypeFare rideType = rideTypeRegistry.get(trip.rideType());
        SurgeStrategy surge = surgeSelector.select(trip);
        PromoStrategy promo = promoRegistry.get(trip.promoCode());

        double fare = rideType.base(trip);
        fare = surge.apply(fare, trip);
        fare = promo.apply(fare, trip);
        return fare;
    }
}
