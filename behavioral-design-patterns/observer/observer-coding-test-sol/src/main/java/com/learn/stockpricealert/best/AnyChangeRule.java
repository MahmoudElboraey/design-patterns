package com.learn.stockpricealert.best;

public class AnyChangeRule implements AlertRule {

    @Override
    public boolean isTriggered(PriceUpdate u) {
        return u.oldPrice() != u.newPrice();
    }
}
