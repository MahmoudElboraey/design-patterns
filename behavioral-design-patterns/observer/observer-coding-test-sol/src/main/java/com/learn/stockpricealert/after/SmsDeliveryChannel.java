package com.learn.stockpricealert.after;

public class SmsDeliveryChannel implements DeliveryChannel {

    @Override
    public void deliver() {
        System.out.println("delivering notification through SMS");
    }
}
