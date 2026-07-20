package com.learn.stockpricealert.best;

public class ThresholdRule implements AlertRule {

    public enum Direction { ABOVE, BELOW }

    private final Direction direction;
    private final double threshold;

    public ThresholdRule(Direction direction, double threshold) {
        this.direction = direction;
        this.threshold = threshold;
    }

    @Override
    public boolean isTriggered(PriceUpdate u) {
        return direction == Direction.ABOVE
                ? u.newPrice() > threshold
                : u.newPrice() < threshold;
    }
}
