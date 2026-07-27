package com.learn.productionrefactor.best;

public class CreditCardFee implements FeeCalculator {

    private static final double VOLUME_THRESHOLD = 5000.0;

    @Override
    public double calculateFee(Order order) {
        double total = order.total();
        if (total > VOLUME_THRESHOLD) {
            return total * 0.025;
        }
        return total * 0.029 + 0.30;
    }
}
