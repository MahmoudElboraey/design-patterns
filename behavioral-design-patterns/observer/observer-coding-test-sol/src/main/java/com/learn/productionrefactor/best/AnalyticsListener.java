package com.learn.productionrefactor.best;

public class AnalyticsListener implements OrderListener {

    @Override
    public void onOrderPlaced(OrderPlacedEvent event) {
        System.out.println("[ANALYTICS] track order_placed id=" + event.order().getId());
    }
}
