package com.learn.productionrefactor.best;

public class EmailListener implements OrderListener {

    @Override
    public void onOrderPlaced(OrderPlacedEvent event) {
        System.out.println("[EMAIL] confirmation to " + event.user().email()
                + " for order " + event.order().getId());
    }
}
