package com.paynest.checkout;

import java.math.BigDecimal;
import java.util.Objects;

import com.paynest.domain.Order;
import com.paynest.payment.PaymentMethod;
import com.paynest.payment.PaymentProcessing;
import com.paynest.payment.rails.RandFormat;

/** Orchestrates checkout without making the Order entity responsible for payment processing. */
public class CheckoutService {
    private final PaymentProcessing paymentProcessing;

    public CheckoutService(PaymentProcessing paymentProcessing) {
        this.paymentProcessing = Objects.requireNonNull(paymentProcessing,
                "payment processing is required");
    }

    /** Attempts payment for the order's single authoritative grand total. */
    public boolean checkout(Order order, PaymentMethod paymentMethod) {
        Objects.requireNonNull(order, "order cannot be null");

        if (order.isPaid())
        {
            System.out.println("Order #" + order.getId() + " is already paid; no payment taken.");
            return false;
        }
        if (paymentMethod == null)
        {
            order.markPaymentFailed();
            System.out.println("Order #" + order.getId() + " was NOT completed: no payment method selected.");
            return false;
        }

        BigDecimal grandTotal = order.calculateTotalInclVat();
        if (grandTotal.signum() <= 0)
        {
            order.markPaymentFailed();
            System.out.println("Order #" + order.getId() + " was NOT completed: order total must be positive.");
            return false;
        }

        double amount = grandTotal.doubleValue();
        boolean success = paymentProcessing.processPayment(paymentMethod, amount);
        if (success)
        {
            order.markPaid();
            System.out.println("Order #" + order.getId() + " completed via " + paymentMethod.getPaymentType()
                    + " for " + RandFormat.format(amount));
        } else {
            order.markPaymentFailed();
            System.out.println("Order #" + order.getId() + " was NOT completed: payment unsuccessful.");
        }
        return success;
    }
}
