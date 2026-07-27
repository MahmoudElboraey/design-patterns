package com.learn.ridefareengine.sol;

public class FeeCalculatorService {
    private final TripTypeFareCalculator tripTypeFareCalculator;
    private final SurgeCalculator surgeCalculator;
    private final PromoCodeDiscountCalculator promoCodeDiscountCalculator;

    public FeeCalculatorService(TripTypeFareCalculator tripTypeFareCalculator , SurgeCalculator surgeCalculator, PromoCodeDiscountCalculator promoCodeDiscountCalculator) {
        this.tripTypeFareCalculator = tripTypeFareCalculator;
        this.surgeCalculator = surgeCalculator;
        this.promoCodeDiscountCalculator = promoCodeDiscountCalculator;
    }


    public double calculateRideFare(Trip trip){
        return promoCodeDiscountCalculator.calculateFareWithDiscount(surgeCalculator.calculateSurge(tripTypeFareCalculator.calculateTripTypeFare(trip)));
    }
}
