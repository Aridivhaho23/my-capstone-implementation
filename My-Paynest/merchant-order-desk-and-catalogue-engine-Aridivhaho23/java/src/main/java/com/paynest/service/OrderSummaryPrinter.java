package com.paynest.service;

import java.util.Locale;

import com.paynest.domain.Customer;
import com.paynest.domain.Order;
import com.paynest.domain.OrderItem;

/**
 * Builds the human-readable order summary as a String (it never prints itself).
 *
 * <p>Every String.format call passes Locale.US on purpose. Without it, a South African machine
 * (en_ZA) prints a decimal COMMA ("R 12000,00"), so the same order would print differently
 * depending on where the program runs.
 */
public class OrderSummaryPrinter {

    public String printSummary(Order order) {

        Customer customer = order.getCustomer();
        StringBuilder sb = new StringBuilder();

        sb.append("Order #").append(order.getId()).append(" — ").append(customer.getName()).append('\n');
        sb.append("Order Items:\n");
        sb.append(String.format(Locale.US, "  %-20s %-10s %12s%n", "Product", "Quantity", "Total Price"));

        if (order.getItems().isEmpty()) {
            sb.append("  (no items)\n");
        } else {
            for (OrderItem item : order.getItems()) {
                sb.append(String.format(Locale.US,
                        "  %-20s %-10d R%11.2f%n",
                        item.getProduct().getName(),
                        item.getQuantity(),
                        item.calculateTotal()));
            }
        }

        sb.append(String.format(Locale.US, "  %-20s R%11.2f%n", "Subtotal:", order.calculateAllTotal()));
        sb.append(String.format(Locale.US, "  %-20s R%11.2f%n", "VAT (15%):", order.calculateVat()));
        // Same method the payment checkout uses: one total, two readers.
        sb.append(String.format(Locale.US, "  %-20s R%11.2f%n", "Total:", order.calculateTotalInclVat()));

        return sb.toString();
    }
}
