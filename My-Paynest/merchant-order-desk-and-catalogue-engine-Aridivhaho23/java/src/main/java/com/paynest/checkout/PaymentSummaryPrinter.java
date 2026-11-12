package com.paynest.checkout;

/**
 * Builds the payment part of the receipt as a String (it never prints itself), mirroring how
 * OrderSummaryPrinter works for the order part. Money.toString is already locale-independent.
 */
public class PaymentSummaryPrinter {

    public String format(OrderCheckout checkout) {
        StringBuilder sb = new StringBuilder();
        sb.append("Payments:\n");
        if (checkout.getPaymentHistory().isEmpty()) {
            sb.append("  (no payments yet)\n");
        }
        for (PaymentRecord record : checkout.getPaymentHistory()) {
            sb.append("  ").append(record.paymentType()).append("  ").append(record.amount()).append('\n');
        }
        sb.append("Order total: ").append(checkout.getOrderTotal()).append('\n');
        sb.append("Amount paid: ").append(checkout.getAmountPaid()).append('\n');
        sb.append("Balance due: ").append(checkout.getBalanceDue()).append('\n');
        sb.append("Status:      ").append(checkout.getStatus()).append('\n');
        return sb.toString();
    }
}
