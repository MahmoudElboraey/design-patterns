package com.learn.stockpricealert.best;

public record PriceUpdate(String symbol, double oldPrice, double newPrice) {
}
