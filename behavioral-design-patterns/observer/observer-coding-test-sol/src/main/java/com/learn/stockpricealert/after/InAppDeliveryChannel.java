package com.learn.stockpricealert.after;

public class InAppDeliveryChannel implements DeliveryChannel {
    @Override
    public void deliver() {
        System.out.println("delivering notification through IN-APP");
    }
}
