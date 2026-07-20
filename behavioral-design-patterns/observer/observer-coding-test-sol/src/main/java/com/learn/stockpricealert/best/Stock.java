package com.learn.stockpricealert.best;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Stock {

    private final String symbol;
    private volatile double price;
    private final List<PriceObserver> observers = new CopyOnWriteArrayList<>();

    public Stock(String symbol, double initialPrice) {
        this.symbol = symbol;
        this.price = initialPrice;
    }

    public String getSymbol() {
        return symbol;
    }

    public double getPrice() {
        return price;
    }

    public void subscribe(PriceObserver observer) {
        observers.add(observer);
    }

    public void unsubscribe(PriceObserver observer) {
        observers.remove(observer);
    }

    public void updatePrice(double newPrice) {
        PriceUpdate event = new PriceUpdate(symbol, price, newPrice);
        price = newPrice;

        for (PriceObserver observer : observers) {
            try {
                observer.onPriceUpdate(event);
            } catch (Exception e) {
                System.err.println("observer failed, skipped: " + e.getMessage());
            }
        }
    }
}
