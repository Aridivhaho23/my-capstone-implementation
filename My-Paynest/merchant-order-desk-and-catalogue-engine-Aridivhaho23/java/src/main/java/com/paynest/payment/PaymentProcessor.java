package com.paynest.payment;

import com.paynest.common.Money;

/**
 * Runs ONE payment on ANY rail and reports what happened as a {@link PaymentOutcome}.
 *
 * <p>It imports no concrete rail: it treats every {@link PaymentMethod} the same way
 * (polymorphism). Validation and error handling live here ONCE for every rail, so a new
 * rail gets them for free. It never prints; the app layer decides what to show.
 */
public class PaymentProcessor {

    private static final String NO_METHOD = "NONE";

    public PaymentOutcome processPayment(PaymentMethod method, Money amount) {
        // Guard 1: no rail chosen.
        if (method == null) {
            return PaymentOutcome.rejected(NO_METHOD, amount, "No payment method selected");
        }
        String type = method.getPaymentType();

        // Guard 2: the amount must be a real, positive Money value.
        if (amount == null || !amount.isPositive()) {
            return PaymentOutcome.rejected(type, amount, "Invalid payment amount: " + amount);
        }

        try {
            // THE polymorphic call: the object we were given decides which code runs.
            return method.processPayment(amount)
                    ? PaymentOutcome.success(type, amount)
                    : PaymentOutcome.declined(type, amount);
        } catch (RuntimeException e) {
            // A misbehaving rail must not crash checkout; treat it as a failed payment.
            return PaymentOutcome.error(type, amount, e.getMessage());
        }
    }
}
