package com.learn.ridefareengine.best;

public class ThresholdSurgeSelector implements SurgeSelector {

    private static final SurgeStrategy NORMAL = new NormalSurge();
    private static final SurgeStrategy SURGE_1_5 = new MultiplierSurge(1.5);
    private static final SurgeStrategy SURGE_2_0 = new MultiplierSurge(2.0);

    @Override
    public SurgeStrategy select(Trip trip) {
        double demand = trip.demandRatio();
        if (demand >= 2.0) {
            return SURGE_2_0;
        }
        if (demand >= 1.5) {
            return SURGE_1_5;
        }
        return NORMAL;
    }
}
