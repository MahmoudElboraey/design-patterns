package com.learn.ridefareengine.best;

import java.util.Map;

public class RideTypeFareRegistry {

    private final Map<RideType, RideTypeFare> strategies = Map.of(
            RideType.ECONOMY, new EconomyFare(),
            RideType.PREMIUM, new PremiumFare(),
            RideType.POOL, new PoolFare()
    );

    public RideTypeFare get(RideType rideType) {
        RideTypeFare fare = strategies.get(rideType);
        if (fare == null) {
            throw new IllegalStateException("No fare strategy registered for ride type: " + rideType);
        }
        return fare;
    }
}
