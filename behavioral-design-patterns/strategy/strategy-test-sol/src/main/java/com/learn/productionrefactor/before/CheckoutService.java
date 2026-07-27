package com.learn.productionrefactor.before;

public class CheckoutService {

    public double calculateProcessingFee(Order order, String paymentMethod) {
        double fee;
        if (paymentMethod.equals("CREDIT_CARD")) {
            fee = order.getTotal() * 0.029 + 0.30;
            if (order.getTotal() > 5000) {
                fee = order.getTotal() * 0.025;          // volume discount
            }
        } else if (paymentMethod.equals("PAYPAL")) {
            fee = order.getTotal() * 0.034 + 0.35;
        } else if (paymentMethod.equals("BANK_TRANSFER")) {
            fee = 5.0;
            if (order.getCurrency().equals("EUR")) {
                fee = 0.0;                                // SEPA is free
            }
        } else if (paymentMethod.equals("CRYPTO")) {
            fee = order.getTotal() * 0.01;
        } else {
            fee = 0.0;                                    // ← ticking bomb, see Part C
        }
        return fee;
    }
}