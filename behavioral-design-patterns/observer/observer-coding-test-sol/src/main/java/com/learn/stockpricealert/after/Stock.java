package com.learn.stockpricealert.after;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Stock {
    private Map<EventType , List<Subscriber>> subscribers;
    private String name;
    private double priceValue;
    private String symbol;


    public Stock(String name, double priceValue, String symbol) {
        this.name = name;
        this.priceValue = priceValue;
        this.symbol = symbol;
        initSubscriberEvents();
    }

    private void initSubscriberEvents() {
        subscribers = new HashMap<>();
        subscribers.put(EventType.ABOVE_200 , new ArrayList<>());
        subscribers.put(EventType.BELOW_150 , new ArrayList<>());
        subscribers.put(EventType.PRICE_CHANGED , new ArrayList<>());

    }

    public String getName() {
        return name;
    }
    public double getPriceValue() {
        return priceValue;
    }
    public String getSymbol() {
        return symbol;
    }

    public void updatePrice(double newPrice) {
        double oldPrice = this.priceValue;
        this.priceValue = newPrice;

        if (newPrice > 200) {
            notifySubscribers(EventType.ABOVE_200, oldPrice, newPrice);
        }
        if (newPrice < 150) {
            notifySubscribers(EventType.BELOW_150, oldPrice, newPrice);
        }
        if (oldPrice != newPrice) {
            notifySubscribers(EventType.PRICE_CHANGED, oldPrice, newPrice);
        }
    }

    private void notifySubscribers(EventType eventType, double oldPrice, double newPrice) {
        subscribers.get(eventType).forEach(subscriber ->
                subscriber.notify(symbol + " " + eventType.name() + ": price moved from " + oldPrice + " to " + newPrice));
    }

    public void subscribe(EventType eventType, Subscriber subscriber) {
        subscribers.get(eventType).add(subscriber);
    }

    public void removeSubscriber(EventType eventType, Subscriber subscriber) {
        subscribers.get(eventType).remove(subscriber);
    }



}
