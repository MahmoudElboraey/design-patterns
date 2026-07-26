package com.learn.productionrefactor.best;

public class PaymentGateway {

    public void charge(Order order) {
        System.out.println("[PAYMENT] charged " + order.getTotal() + " for order " + order.getId());
    }
}
