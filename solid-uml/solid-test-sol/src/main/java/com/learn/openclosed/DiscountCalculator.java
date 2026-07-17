package com.learn.openclosed;

public class DiscountCalculator {
    private final CustomerDiscount customerDiscount;

    public DiscountCalculator(CustomerDiscount customerDiscount) {
        this.customerDiscount = customerDiscount;
    }


    public double calculate(Order order) {
        return customerDiscount.calculate(order);
    }
}