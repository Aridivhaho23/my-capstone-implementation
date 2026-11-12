package com.paynest.payment.rails;

import com.paynest.payment.PaymentMethod;
/**
 * ABSTRACT BASE CLASS for the payment types that ship with the application.
 *
 * <p>It implements the {@link PaymentMethod} interface and holds everything the rails have in
 * common, so each rail only writes the part that is different:
 * <ul>
 *   <li>the stable label (set once through the constructor);</li>
 *   <li>the amount check, done ONCE here for every rail;</li>
 *   <li>the pattern "check the amount, then let the rail decide" (the Template Method pattern).</li>
 * </ul>
 *
 * <p>A rail extends this class and writes one method, {@link #authorise(double)}: its own
 * console message and its own rule. Nothing outside this class can skip the amount check,
 * because {@link #processPayment(double)} is final.
 *
 * <p>The checkout still depends on the {@link PaymentMethod} INTERFACE, not on this class, so a
 * payment type that does not want to extend it can implement the interface directly.
 */
public abstract class AbstractPaymentMethod implements PaymentMethod {

    private final String paymentType;

    /** @param paymentType the label for this rail, e.g. "CARD" (must not be blank) */
    protected AbstractPaymentMethod(String paymentType) {
        if (paymentType == null || paymentType.isBlank()) {
            throw new IllegalArgumentException("Payment type label must not be blank");
        }
        this.paymentType = paymentType;
    }

    /**
     * The shared steps. It is FINAL so no rail can accidentally skip the amount check.
     * Step 1 is the same for every rail; step 2 is the rail's own rule.
     */
    @Override
    public final boolean processPayment(double amount) {
        // Step 1 (shared): never send a nonsense amount (zero, negative, NaN, infinite) to a rail.
        if (!isValidAmount(amount)) {
            System.out.println("[" + paymentType + "] Rejected: amount must be a positive number but was "
                    + amount);
            return false;
        }
        // Step 2 (rail-specific): the concrete class prints its own message and decides.
        return authorise(amount);
    }

    /**
     * The part that differs per rail. Print a short message that shows which rail is used,
     * then return true (accepted) or false (declined).
     *
     * @param amount a valid, positive amount
     */
    protected abstract boolean authorise(double amount);

    @Override
    public final String getPaymentType() {
        return paymentType;
    }

    private static boolean isValidAmount(double amount) {
        return !Double.isNaN(amount) && !Double.isInfinite(amount) && amount > 0;
    }
}
