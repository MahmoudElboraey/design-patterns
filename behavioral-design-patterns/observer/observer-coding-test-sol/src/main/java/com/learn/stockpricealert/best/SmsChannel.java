package com.learn.stockpricealert.best;

public class SmsChannel implements DeliveryChannel {

    @Override
    public void deliver(User user, String message) {
        System.out.println("[SMS to " + user.phone() + "] " + message);
    }
}
