package com.learn.productionrefactor.after;

public class PaymentService implements OrderCreation{
    @Override
    public void onOrderCreated(Order order, User user) {
        System.out.println("User with name " + user.getEmail() + " has been charged for order " + order.getId());

    }
}
