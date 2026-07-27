package com.learn.productionrefactor.best;

public class Main {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        CheckoutService checkout = new CheckoutService(new FeeCalculatorFactory());

        check("CreditCard base", checkout, new Order(100, "USD"), "CREDIT_CARD", 3.20);
        check("CreditCard volume discount", checkout, new Order(6000, "USD"), "CREDIT_CARD", 150.0);
        check("Paypal", checkout, new Order(100, "USD"), "PAYPAL", 3.75);
        check("Crypto", checkout, new Order(100, "USD"), "CRYPTO", 1.0);
        check("BankTransfer EUR (SEPA free)", checkout, new Order(100, "EUR"), "BANK_TRANSFER", 0.0);
        check("BankTransfer USD", checkout, new Order(100, "USD"), "BANK_TRANSFER", 5.0);

        System.out.println("\n--- unknown method: fail loud, no silent 0.0 leak ---");
        try {
            checkout.calculateProcessingFee(new Order(100, "USD"), "GIFT_CARD");
            System.out.println("FAIL: expected exception, got none");
            failed++;
        } catch (IllegalArgumentException e) {
            System.out.println("PASS: rejected unknown method -> " + e.getMessage());
            passed++;
        }

        System.out.println("\nPassed: " + passed + ", Failed: " + failed);
    }

    private static void check(String name, CheckoutService checkout, Order order,
                              String method, double expected) {
        double actual = checkout.calculateProcessingFee(order, method);
        boolean ok = Math.abs(actual - expected) < 1e-9;
        if (ok) {
            passed++;
        } else {
            failed++;
        }
        System.out.printf("[%s] %s -> expected %.2f, got %.2f%n",
                ok ? "PASS" : "FAIL", name, expected, actual);
    }
}
