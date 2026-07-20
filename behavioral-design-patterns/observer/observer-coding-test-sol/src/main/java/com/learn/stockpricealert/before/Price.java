package com.learn.stockpricealert.before;

public class Price {
    private double value;
    private String symbol;

    public Price(String symbol, double value) {
        this.symbol = symbol;
        this.value = value;
    }

    public double getValue() {
        return value;
    }
    public void setValue(double value) {
        this.value = value;
    }
    public String getSymbol() {
        return symbol;
    }
    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }
}
