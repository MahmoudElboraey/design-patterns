package com.learn.stockpricealert.after;

import java.util.HashMap;
import java.util.Map;

public class DeliveryChannelFactory {
    private Map<String, DeliveryChannel> deliveryChannels;

    public DeliveryChannelFactory() {
        deliveryChannels = new HashMap<>();
        deliveryChannels.put("EMAIL", new EmailDeliveryChannel());
        deliveryChannels.put("SMS", new SmsDeliveryChannel());
        deliveryChannels.put("INAPP", new InAppDeliveryChannel());
    }
    public DeliveryChannel getDeliveryChannel(String symbol) {
        return deliveryChannels.get(symbol);
    }
}
