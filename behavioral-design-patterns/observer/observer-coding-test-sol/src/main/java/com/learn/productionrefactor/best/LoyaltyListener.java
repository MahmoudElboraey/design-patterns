package com.learn.productionrefactor.best;

public class LoyaltyListener implements OrderListener {

    @Override
    public void onOrderPlaced(OrderPlacedEvent event) {
        System.out.println("[LOYALTY] +" + event.order().getTotal()
                + " points to user " + event.user().id());
    }
}
