package com.learn.productionrefactor.after;

public class AnalyticsService implements OrderCreation{
    @Override
    public void onOrderCreated(Order order, User user) {
        System.out.println("analytics service work for Order " + order.getId() + " created for user " + user.getEmail());
    }
}
