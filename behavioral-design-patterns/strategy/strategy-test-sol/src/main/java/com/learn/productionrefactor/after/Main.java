package com.learn.productionrefactor.after;

public class Main {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        FeeCalculatorFactory factory = new FeeCalculatorFactory();

        // CREDIT_CARD: 100 * 0.029 + 0.30 = 3.20
        check("CreditCard base", factory.getFeeCalculator("CREDIT_CARD"), order(100, "USD"), 3.20);
        // CREDIT_CARD volume discount: total > 5000 -> 6000 * 0.025 = 150
        check("CreditCard discount", factory.getFeeCalculator("CREDIT_CARD"), order(6000, "USD"), 150.0);

        // PAYPAL: 100 * 0.034 + 0.35 = 3.75
        check("Paypal", factory.getFeeCalculator("PAYPAL"), order(100, "USD"), 3.75);

        // CRYPTO: 100 * 0.01 = 1.0
        check("Crypto", factory.getFeeCalculator("CRYPTO"), order(100, "USD"), 1.0);

        // BANK_TRANSFER EUR -> SEPA free = 0.0
        check("BankTransfer EUR", factory.getFeeCalculator("BANK_TRANSFER"), order(100, "EUR"), 0.0);
        // BANK_TRANSFER non-EUR -> 5.0
        check("BankTransfer USD", factory.getFeeCalculator("BANK_TRANSFER"), order(100, "USD"), 5.0);

        // Unknown type -> default lambda returns 0.0
        check("Unknown default", factory.getFeeCalculator("GIFT_CARD"), order(100, "USD"), 0.0);

        System.out.println("\nPassed: " + passed + ", Failed: " + failed);
    }

    private static Order order(double total, String currency) {
        Order o = new Order();
        o.setTotal(total);
        o.setCurrency(currency);
        return o;
    }

    private static void check(String name, FeeCalculator calc, Order order, double expected) {
        double actual = calc.calculateFee(order);
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
