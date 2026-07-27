package com.learn.productionrefactor.best;

import java.util.Map;

public class FeeCalculatorFactory {

    private final Map<String, FeeCalculator> strategies = Map.of(
            "CREDIT_CARD", new CreditCardFee(),
            "PAYPAL", new PaypalFee(),
            "BANK_TRANSFER", new BankTransferFee(),
            "CRYPTO", new CryptoFee()
    );

    public FeeCalculator getFeeCalculator(String paymentMethod) {
        FeeCalculator calculator = strategies.get(paymentMethod);
        if (calculator == null) {
            throw new IllegalArgumentException(
                    "No fee strategy registered for payment method: " + paymentMethod);
        }
        return calculator;
    }
}
