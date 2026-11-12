package com.paynest.payment.rails;

import com.paynest.common.Money;
import com.paynest.payment.PaymentMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Pays ONE amount using SEVERAL rails, e.g. R5000 from the wallet and R7400 by card.
 * (The Composite pattern: a PaymentMethod made of PaymentMethods.)
 *
 * <p>Because SplitPayment itself implements PaymentMethod, checkout accepts it without any
 * change: the checkout cannot tell a split from a single rail.
 *
 * <p>ALL-OR-NOTHING: if any part fails, every part that already succeeded is reversed.
 */
public class SplitPayment implements PaymentMethod {

    /** One slice of the split: which rail, and how much it pays. */
    public record Part(PaymentMethod method, Money amount) {
    }

    private final List<Part> parts = new ArrayList<>();

    /** Adds a slice. Returns this so calls can be chained: new SplitPayment().add(...).add(...). */
    public SplitPayment add(PaymentMethod method, Money amount) {
        Objects.requireNonNull(method, "Payment method is required");
        if (amount == null || !amount.isPositive()) {
            throw new IllegalArgumentException("Each part must be a positive amount");
        }
        parts.add(new Part(method, amount));
        return this;
    }

    @Override
    public boolean processPayment(Money total) {
        if (parts.isEmpty()) {
            return false;
        }
        // Rule: the parts must add up to EXACTLY the amount due (no over- or under-charging).
        if (!sumOfParts().equals(total)) {
            return false;
        }

        // Charge each part in order, remembering the ones that worked in case we must undo them.
        List<Part> succeeded = new ArrayList<>();
        for (Part part : parts) {
            boolean ok;
            try {
                ok = part.method().processPayment(part.amount());
            } catch (RuntimeException e) {
                ok = false; // An erroring part counts as a failed part.
            }
            if (!ok) {
                rollback(succeeded);
                return false;
            }
            succeeded.add(part);
        }
        return true;
    }

    private Money sumOfParts() {
        Money sum = Money.ZERO;
        for (Part part : parts) {
            sum = sum.add(part.amount());
        }
        return sum;
    }

    /** Reverses the parts that succeeded, newest first. */
    private void rollback(List<Part> succeeded) {
        for (int i = succeeded.size() - 1; i >= 0; i--) {
            Part part = succeeded.get(i);
            part.method().reverse(part.amount());
        }
    }

    @Override
    public String getPaymentType() {
        return "SPLIT";
    }

    /** Reverses every part, but only for the full amount (a split is all-or-nothing). */
    @Override
    public void reverse(Money amount) {
        if (!sumOfParts().equals(amount)) {
            throw new IllegalArgumentException("A split payment can only be reversed in full");
        }
        rollback(parts);
    }
}
