package com.paynest.payment;

/** Abstraction for coordinating a payment attempt. */
@FunctionalInterface
public interface PaymentProcessing {
    boolean processPayment(PaymentMethod method, double amount);
}
