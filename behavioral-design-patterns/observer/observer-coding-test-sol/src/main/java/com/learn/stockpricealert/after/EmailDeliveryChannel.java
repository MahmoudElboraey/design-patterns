package com.learn.stockpricealert.after;

public class EmailDeliveryChannel implements DeliveryChannel {
    @Override
    public void deliver() {
        System.out.println("delivering notification through EMAIL");
    }
}
