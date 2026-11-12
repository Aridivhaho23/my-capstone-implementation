package com.paynest.checkout;

import com.paynest.common.Money;
import com.paynest.domain.Order;
import com.paynest.payment.PaymentMethod;
import com.paynest.payment.PaymentOutcome;
import com.paynest.payment.PaymentProcessor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Collects payment for ONE Capstone 1 {@link Order}: full checkout or instalments.
 *
 * <p>This is the bridge between the order world (domain) and the payment world (payment).
 * It depends only on abstractions: the domain Order, the {@link PaymentMethod} interface and
 * the {@link PaymentProcessor}. It never names CardPayment, WalletPayment, etc.
 *
 * <p>One instance owns the payment state of one order (amount paid, status, history), so
 * there is no hidden shared map and nothing to leak. The payable amount comes from
 * {@link Order#calculateTotalInclVat()}; this class has no total formula of its own.
 *
 * <p>When the first payment succeeds the order is {@linkplain Order#lock() locked}, so the
 * total cannot change under a part-paid order.
 */
public class OrderCheckout {

    private final Order order;
    private final PaymentProcessor processor;
    private final List<PaymentRecord> history = new ArrayList<>();
    private Money amountPaid = Money.ZERO;
    private PaymentStatus status = PaymentStatus.PENDING;

    public OrderCheckout(Order order) {
        this(order, new PaymentProcessor());
    }

    public OrderCheckout(Order order, PaymentProcessor processor) {
        this.order = Objects.requireNonNull(order, "Order is required");
        this.processor = Objects.requireNonNull(processor, "Payment processor is required");
    }

    /** What the customer owes in total: Capstone 1 subtotal plus VAT. */
    public Money getOrderTotal() {
        return Money.of(order.calculateTotalInclVat());
    }

    /** Total minus everything already paid. */
    public Money getBalanceDue() {
        return getOrderTotal().subtract(amountPaid);
    }

    public Money getAmountPaid() {
        return amountPaid;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    /** Read-only: callers can look at the history but cannot edit it. */
    public List<PaymentRecord> getPaymentHistory() {
        return Collections.unmodifiableList(history);
    }

    /** Pays the FULL balance due with the chosen rail (the whole total on a fresh order). */
    public PaymentOutcome checkout(PaymentMethod method) {
        return pay(method, getBalanceDue());
    }

    /**
     * Pays PART of the balance (a deposit or instalment). The amount cannot exceed the balance
     * due, so the customer is never over-charged.
     */
    public PaymentOutcome payInstalment(PaymentMethod method, Money amount) {
        if (amount != null && amount.isGreaterThan(getBalanceDue())) {
            return PaymentOutcome.rejected(typeOf(method), amount,
                    "Amount " + amount + " is more than the balance due of " + getBalanceDue());
        }
        return pay(method, amount);
    }

    /** Shared core for both public paths, so the rules exist once. */
    private PaymentOutcome pay(PaymentMethod method, Money amount) {
        // A fully paid order must never be charged again.
        if (status == PaymentStatus.COMPLETED) {
            return PaymentOutcome.rejected(typeOf(method), amount, "Order is already fully paid");
        }

        // The processor validates the request and runs the rail. It never throws for a failed payment.
        PaymentOutcome outcome = processor.processPayment(method, amount);
        if (!outcome.isSuccess()) {
            return outcome; // Failures change nothing: no history, no status change, no lock.
        }

        // Only a successful payment updates state. Lock first, so the total is frozen from here on.
        order.lock();
        amountPaid = amountPaid.add(amount);
        history.add(new PaymentRecord(method.getPaymentType(), amount));
        status = getBalanceDue().isZero() ? PaymentStatus.COMPLETED : PaymentStatus.PARTIALLY_PAID;
        return outcome;
    }

    private static String typeOf(PaymentMethod method) {
        return method == null ? "NONE" : method.getPaymentType();
    }
}
