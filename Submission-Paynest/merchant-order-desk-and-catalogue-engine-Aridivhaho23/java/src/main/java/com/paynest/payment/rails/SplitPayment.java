package com.paynest.payment.rails;

import com.paynest.payment.PaymentMethod;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Combines multiple payment instruments for one checkout.
 *
 * <p>Each allocation is a fixed Rand amount. The allocations must add up to
 * the checkout total. Processing stops at the first declined allocation.
 * Earlier successful allocations remain captured, as real payment systems
 * require compensating refunds rather than pretending a transaction rolled back.
 */
public class SplitPayment implements PaymentMethod {

    public record Allocation(PaymentMethod method, BigDecimal amount) {
        public Allocation {
            Objects.requireNonNull(method, "Payment method is required");
            Objects.requireNonNull(amount, "Allocation amount is required");
            if (amount.signum() <= 0) {
                throw new IllegalArgumentException("Allocation amount must be positive");
            }
        }

        public Allocation(PaymentMethod method, double amount) {
            this(method, BigDecimal.valueOf(amount));
        }
    }

    private final List<Allocation> allocations;

    public SplitPayment(List<Allocation> allocations) {
        if (allocations == null || allocations.size() < 2) {
            throw new IllegalArgumentException("A split payment needs at least two allocations");
        }
        this.allocations = List.copyOf(allocations);
        for (Allocation allocation : this.allocations) {
            if (!(allocation.method() instanceof Refundable)) {
                throw new IllegalArgumentException("Split-payment rails must support refunds for compensation");
            }
        }
    }

    @Override
    public boolean processPayment(double amount) {
        if (Double.isNaN(amount) || Double.isInfinite(amount) || amount <= 0) {
            return false;
        }
        BigDecimal expected = BigDecimal.valueOf(amount);
        BigDecimal allocated = allocations.stream()
                .map(Allocation::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (allocated.compareTo(expected) != 0) {
            System.out.println("[SPLIT] Allocations do not equal the checkout total");
            return false;
        }

        List<Allocation> captured = new java.util.ArrayList<>();
        for (Allocation allocation : allocations) {
            if (!allocation.method().processPayment(allocation.amount().doubleValue())) {
                System.out.println("[SPLIT] Declined during " + allocation.method().getPaymentType()
                        + "; compensating previously approved allocations");
                boolean fullyCompensated = true;
                for (int i = captured.size() - 1; i >= 0; i--) {
                    Allocation prior = captured.get(i);
                    boolean refunded = ((Refundable) prior.method()).refund(prior.amount().doubleValue());
                    fullyCompensated &= refunded;
                }
                if (!fullyCompensated) {
                    System.out.println("[SPLIT] WARNING: compensation incomplete; manual reconciliation required");
                }
                return false;
            }
            captured.add(allocation);
        }
        System.out.println("[SPLIT] All allocations approved");
        return true;
    }

    @Override
    public String getPaymentType() {
        return "SPLIT";
    }

    public List<Allocation> getAllocations() {
        return allocations;
    }
}
