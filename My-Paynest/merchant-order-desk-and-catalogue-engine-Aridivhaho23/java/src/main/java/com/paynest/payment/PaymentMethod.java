package com.paynest.payment;

import com.paynest.common.Money;

/**
 * THE PAYMENT CONTRACT: "how does the customer pay?"
 *
 * <p>This interface says WHAT every rail must do, never HOW. The checkout layer and
 * {@link PaymentProcessor} only know this interface, so a new rail never forces a change
 * to them (Open/Closed) and any rail can stand in for any other (Liskov).
 *
 * <p>The contract is deliberately tiny (Interface Segregation): charge, label, and an
 * optional undo.
 *
 * <p>Implementations must NOT print or log to the console. Report the result through the
 * return value; the processor turns it into a {@link PaymentOutcome}.
 *
 * <p>Design choice: the label is a String, not an enum. An enum would have to be edited
 * for every new rail, which breaks the "new class only" extension rule.
 */
public interface PaymentMethod {

    /**
     * Attempts to charge the amount on this rail.
     *
     * @param amount a positive amount (the processor guarantees this before calling)
     * @return true if the payment succeeded, false if it was declined. Declines are signalled
     *         by the return value, not by an exception.
     */
    boolean processPayment(Money amount);

    /** A stable, human-readable label such as "CARD", used in receipts and payment history. */
    String getPaymentType();

    /**
     * Undoes a payment that succeeded earlier (a refund / rollback).
     *
     * <p>Needed for split payments: if the second part fails, the first must be given back.
     * It is a DEFAULT method that does nothing, so a simple rail compiles without writing it.
     * Rails that hold real state (like a wallet balance) should override it. A rail may refuse
     * an amount it never charged by throwing IllegalArgumentException.
     */
    default void reverse(Money amount) {
        // Intentionally empty: rails with nothing to undo can inherit this.
    }
}
