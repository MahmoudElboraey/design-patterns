package com.learn.stockpricealert.best;

public class Subscription implements PriceObserver {

    private final User user;
    private final AlertRule rule;
    private final DeliveryChannel channel;

    public Subscription(User user, AlertRule rule, DeliveryChannel channel) {
        this.user = user;
        this.rule = rule;
        this.channel = channel;
    }

    @Override
    public void onPriceUpdate(PriceUpdate u) {
        if (rule.isTriggered(u)) {
            channel.deliver(user, u.symbol() + " moved " + u.oldPrice() + " -> " + u.newPrice());
        }
    }
}
