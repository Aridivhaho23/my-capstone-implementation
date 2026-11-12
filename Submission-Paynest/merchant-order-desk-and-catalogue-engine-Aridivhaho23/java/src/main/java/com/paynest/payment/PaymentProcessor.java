package com.paynest.payment;

import com.paynest.payment.rails.RandFormat;

/**
 * Runs a payment on ANY rail and prints the outcome.
 *
 * <p>It never mentions CardPayment, EftPayment or WalletPayment: it works with the
 * {@link PaymentMethod} interface only. {@code method.processPayment(amount)} automatically runs
 * the card, EFT or wallet version, depending on the object it was given (polymorphism). The
 * success and failure wording lives here ONCE, so a new rail does not repeat it.
 */
public class PaymentProcessor implements PaymentProcessing {

    /**
     * @param method the chosen payment type (any implementation of the interface)
     * @param amount the Rand amount to charge
     * @return true only if the payment succeeded
     */
    @Override
    public boolean processPayment(PaymentMethod method, double amount) {
        // Defensive: no payment type chosen. Fail cleanly instead of crashing.
        if (method == null) {
            System.out.println("Payment failed: no payment method selected");
            return false;
        }

        boolean success;
        try {
            success = method.processPayment(amount); // the polymorphic call
        } catch (RuntimeException e) {
            // A misbehaving rail must not crash checkout, and must never count as a success.
            System.out.println("Payment failed via " + method.getPaymentType() + ": " + e.getMessage());
            return false;
        }

        if (success) {
            System.out.println("Payment successful via " + method.getPaymentType());
            System.out.println("Amount: " + RandFormat.format(amount));
        } else {
            System.out.println("Payment failed via " + method.getPaymentType());
        }
        return success;
    }
}
