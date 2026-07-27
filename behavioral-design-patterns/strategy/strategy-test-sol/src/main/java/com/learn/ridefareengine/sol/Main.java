package com.learn.ridefareengine.sol;

public class Main {
    public static void main(String[] args) {

        // ---- Scenario 1: ECONOMY, zone normal (x1.0), no promo ----
        // fare = 10 + 2.5*10 + 0.5*20 = 45
        Trip tripA = new Trip(10, 20);
        FeeCalculatorService economyNormal = new FeeCalculatorService(
                new EconomyTrip(),
                new NormalSurge(),
                new NoDiscount());
        System.out.printf("ECONOMY  normal (x1.0)   -> %.2f%n",
                economyNormal.calculateRideFare(tripA));

        // ---- Scenario 2: same trip, ECONOMY at x2.0 surge, no promo ----
        // fare = 45 * 2.0 = 90
        FeeCalculatorService economySurge = new FeeCalculatorService(
                new EconomyTrip(),
                new HigherSurging(),
                new NoDiscount());
        System.out.printf("ECONOMY  surge  (x2.0)   -> %.2f%n",
                economySurge.calculateRideFare(tripA));

        // ---- Scenario 3: POOL with "flat 20 off", proving min-fare floor holds ----
        // short trip so 0.7 * economy < 15  ->  floor kicks in at 15
        // economy(2,4) = 10 + 2.5*2 + 0.5*4 = 17 ; 0.7*17 = 11.9  -> floored to 15
        Trip tripB = new Trip(2, 4);

        double rawPoolFare = new PoolTrip().calculateTripTypeFare(tripB);
        System.out.printf("POOL ride-type fare      -> %.2f  (floor held at 15, not 11.90)%n",
                rawPoolFare);

        FeeCalculatorService poolFlatOff = new FeeCalculatorService(
                new PoolTrip(),
                new NormalSurge(),
                new FlatDiscount(20));
        System.out.printf("POOL  x1.0 + flat 20 off -> %.2f  (promo clamped at 0, no negative fare)%n",
                poolFlatOff.calculateRideFare(tripB));
    }
}
