package com.learn.stockpricealert.best;

public class BrokenPushChannel implements DeliveryChannel {

    @Override
    public void deliver(User user, String message) {
        throw new IllegalStateException("invalid push token for " + user.name());
    }
}
