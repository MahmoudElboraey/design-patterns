package com.learn.stockpricealert.best;

public interface AlertRule {

    boolean isTriggered(PriceUpdate update);
}
