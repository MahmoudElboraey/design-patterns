package com.learn.ridefareengine.best;

public class Main {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        FareService fareService = new FareService(
                new RideTypeFareRegistry(),
                new ThresholdSurgeSelector(),
                new PromoRegistry());

        Trip economyNormal = new Trip(RideType.ECONOMY, 10, 20, 1.0, null);
        check("ECONOMY normal (x1.0)", fareService, economyNormal, 45.0);

        Trip economySurge = new Trip(RideType.ECONOMY, 10, 20, 2.0, null);
        check("ECONOMY surge (x2.0)", fareService, economySurge, 90.0);

        Trip poolFlatOff = new Trip(RideType.POOL, 2, 4, 1.0, "FLAT20");
        check("POOL floor 15 + flat 20 off -> clamped 0", fareService, poolFlatOff, 0.0);

        System.out.println("\n--- selection fails loud: MOTO in enum, no strategy registered ---");
        Trip moto = new Trip(RideType.MOTO, 10, 20, 1.0, null);
        try {
            fareService.calculateFare(moto);
            System.out.println("FAIL: expected exception for unregistered MOTO");
            failed++;
        } catch (IllegalStateException e) {
            System.out.println("PASS: " + e.getMessage());
            passed++;
        }

        System.out.println("\nPassed: " + passed + ", Failed: " + failed);
    }

    private static void check(String name, FareService service, Trip trip, double expected) {
        double actual = service.calculateFare(trip);
        boolean ok = Math.abs(actual - expected) < 1e-9;
        if (ok) {
            passed++;
        } else {
            failed++;
        }
        System.out.printf("[%s] %s -> expected %.2f, got %.2f%n",
                ok ? "PASS" : "FAIL", name, expected, actual);
    }
}
