package com.learn.productionrefactor.after;

import java.util.HashMap;
import java.util.Map;

public class FeeCalculatorFactory {

    private Map<String, FeeCalculator> factory = new HashMap<>();

    public FeeCalculatorFactory() {
        factory.put("CREDIT_CARD", new CreditCardFee());
        factory.put("PAYPAL", new PaypalFee());
        factory.put("BANK_TRANSFER", new BankTransferFee());
        factory.put("CRYPTO", new CryptoFee());

    }

    public FeeCalculator getFeeCalculator(String type) {
        return factory.getOrDefault(type, order -> 0.0);
    }
}
