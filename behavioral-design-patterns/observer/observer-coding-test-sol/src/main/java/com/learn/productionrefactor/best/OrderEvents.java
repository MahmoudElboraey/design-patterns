package com.learn.productionrefactor.best;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class OrderEvents {

    private final List<OrderListener> listeners = new CopyOnWriteArrayList<>();

    public void register(OrderListener listener) {
        listeners.add(listener);
    }

    public void unregister(OrderListener listener) {
        listeners.remove(listener);
    }

    public void publish(OrderPlacedEvent event) {
        for (OrderListener listener : listeners) {
            try {
                listener.onOrderPlaced(event);
            } catch (Exception e) {
                System.err.println("listener " + listener.getClass().getSimpleName()
                        + " failed for order " + event.order().getId() + ": " + e.getMessage());
            }
        }
    }
}
