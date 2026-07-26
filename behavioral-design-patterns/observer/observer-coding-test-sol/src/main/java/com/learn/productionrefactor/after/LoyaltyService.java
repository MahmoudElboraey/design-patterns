package com.learn.productionrefactor.after;

public class LoyaltyService implements OrderCreation {
    @Override
    public void onOrderCreated(Order order, User user) {
        System.out.println("loyalty service work for user " + user.getEmail() + " and for order " + order.getId());
    }
}
