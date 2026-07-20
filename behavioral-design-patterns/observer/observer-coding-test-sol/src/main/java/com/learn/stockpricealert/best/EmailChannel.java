package com.learn.stockpricealert.best;

public class EmailChannel implements DeliveryChannel {

    @Override
    public void deliver(User user, String message) {
        System.out.println("[EMAIL to " + user.email() + "] " + message);
    }
}
