package com.learn.openclosed;

import java.util.Map;

public class DiscountCalculatorFactory {
    private static final Map<String, CustomerDiscount> STRATEGIES = Map.of(
            "REGULAR", new RegularCustomer(),
            "PREMIUM", new PremiumCustomer(),
            "VIP",     new VIPCustomer()
    );

    public static CustomerDiscount forType(String customerType) {
        return STRATEGIES.getOrDefault(customerType, order -> 0);
    }
}
