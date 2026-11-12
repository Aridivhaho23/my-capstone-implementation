package com.paynest.payment;

/**
 * THE PAYMENT CONTRACT: "how does the customer pay?"
 *
 * <p>It says WHAT every payment type must do, never HOW. The checkout code only knows this
 * interface, so a new payment type (for example BuyNowPayLaterPayment) never forces a change
 * to the order code.
 */
public interface PaymentMethod {

    /**
     * Tries to charge the amount.
     *
     * @param amount the Rand amount to charge (the order grand total)
     * @return true if the payment succeeded, false if it failed
     */
    boolean processPayment(double amount);

    /** A short, stable label for receipts and logs, e.g. "CARD". */
    String getPaymentType();
}
