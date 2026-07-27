package com.learn.productionrefactor.after;

public class CreditCardFee implements FeeCalculator {
    @Override
    public double calculateFee(Order order) {
        double total = order.getTotal();
        double fee = total * 0.029 + 0.30;
        if (total > 5000) {
            fee = total * 0.025;          // volume discount
        }
        return fee;
    }
}
