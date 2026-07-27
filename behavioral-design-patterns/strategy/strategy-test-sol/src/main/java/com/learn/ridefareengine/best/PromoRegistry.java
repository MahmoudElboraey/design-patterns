package com.learn.ridefareengine.best;

import java.util.Map;

public class PromoRegistry {

    private static final PromoStrategy NO_PROMO = new NoPromo();

    private final Map<String, PromoStrategy> strategies = Map.of(
            "PCT10", new PercentOffPromo(10),
            "FLAT20", new FlatOffPromo(20)
    );

    public PromoStrategy get(String promoCode) {
        if (promoCode == null) {
            return NO_PROMO;
        }
        PromoStrategy promo = strategies.get(promoCode);
        if (promo == null) {
            throw new IllegalArgumentException("Unknown promo code: " + promoCode);
        }
        return promo;
    }
}
