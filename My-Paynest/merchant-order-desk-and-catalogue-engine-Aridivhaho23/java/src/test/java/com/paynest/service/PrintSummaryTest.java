package com.paynest.service;

import java.math.BigDecimal;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.paynest.domain.Customer;
import com.paynest.domain.Order;
import com.paynest.domain.Product;

class PrintSummaryTest {

    private Order sampleOrder() {
        Order order = new Order(1, new Customer(1, "Ari", "ari@example.com"));
        order.addItem(new Product(1, "Laptop", new BigDecimal("39.99"), 10), 2); // line total 79.98
        order.addItem(new Product(2, "Phone", new BigDecimal("29.99"), 10), 1);  // line total 29.99
        return order;
    }

    @Test
    void printSummaryContainsFormattedTotals() {
        Order order = sampleOrder();
        String output = new OrderSummaryPrinter().printSummary(order);

        // Expected values are built with Locale.US so the test says what the printer must do.
        assertTrue(output.contains(String.format(Locale.US, "R%11.2f", order.calculateAllTotal())),
                "Summary should contain the formatted subtotal");
        assertTrue(output.contains(String.format(Locale.US, "R%11.2f", order.calculateVat())),
                "Summary should contain the formatted VAT");
        assertTrue(output.contains(String.format(Locale.US, "R%11.2f", order.calculateTotalInclVat())),
                "Summary should contain the formatted total");
    }

    /** Regression: a South African default locale used to print decimal commas ("R   109,97"). */
    @Test
    void summaryUsesDecimalPointsWhateverTheMachineLocale() {
        Locale original = Locale.getDefault();
        Locale.setDefault(Locale.forLanguageTag("en-ZA"));
        try {
            String output = new OrderSummaryPrinter().printSummary(sampleOrder());

            assertTrue(output.contains("109.97"), "subtotal uses a decimal point");
            assertTrue(!output.contains("109,97"), "no decimal comma");
        } finally {
            Locale.setDefault(original);
        }
    }

    @Test
    void totalLineMatchesTheAmountTheCheckoutWillCharge() {
        Order order = sampleOrder();
        String output = new OrderSummaryPrinter().printSummary(order);

        assertEquals(new BigDecimal("126.47"), order.calculateTotalInclVat()); // 109.97 + 16.50
        assertTrue(output.contains("126.47"));
    }

    @Test
    void emptyOrderPrintsZeroedTotals() {
        Order order = new Order(1, new Customer(1, "Ari", "ari@example.com"));
        String output = new OrderSummaryPrinter().printSummary(order);

        assertTrue(output.contains("(no items)"));
        assertTrue(output.contains("0.00"));
    }
}
