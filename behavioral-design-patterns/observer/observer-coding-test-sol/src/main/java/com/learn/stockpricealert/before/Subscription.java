package com.learn.stockpricealert.before;


public class Subscription {
    private Rule rule;
    private double rangeValue;
    private Channel channel;
    private String stockName;

    public Subscription(Rule rule, double rangeValue, Channel channel , String stockName) {
        this.rule = rule;
        this.rangeValue = rangeValue;
        this.channel = channel;
        this.stockName = stockName;
    }


    public Rule getRule() {
        return rule;
    }

    public void setRule(Rule rule) {
        this.rule = rule;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public double getRangeValue() {
        return rangeValue;
    }

    public void setRangeValue(double rangeValue) {
        this.rangeValue = rangeValue;
    }


    public String getStockName() {
        return stockName;
    }

    public void setStockName(String stockName) {
        this.stockName = stockName;
    }
}
