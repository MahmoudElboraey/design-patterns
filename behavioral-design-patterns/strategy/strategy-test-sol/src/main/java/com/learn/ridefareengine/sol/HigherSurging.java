package com.learn.ridefareengine.sol;

public class HigherSurging implements SurgeCalculator{
    @Override
    public double calculateSurge(double amount) {
        return 2 * amount;
    }
}
