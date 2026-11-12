package com.paynest.checkout;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.paynest.common.Money;
import com.paynest.domain.Customer;
import com.paynest.domain.Order;
import com.paynest.domain.Product;
import com.paynest.payment.rails.EftPayment;

import java.math.BigDecimal;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class PaymentSummaryPrinterTest {

    private OrderCheckout checkoutWithDeposit() {
        Order order = new Order(1, new Customer(1, "Test Customer", "test@example.com"));
        order.addItem(new Product(1, "Item", new BigDecimal("100.00"), 5), 2); // R230.00 incl. VAT
        OrderCheckout checkout = new OrderCheckout(order);
        checkout.payInstalment(new EftPayment(), Money.of("80.00"));
        return checkout;
    }

    @Test
    void showsHistoryTotalsAndStatus() {
        String text = new PaymentSummaryPrinter().format(checkoutWithDeposit());

        assertTrue(text.contains("EFT  R80.00"));
        assertTrue(text.contains("Order total: R230.00"));
        assertTrue(text.contains("Amount paid: R80.00"));
        assertTrue(text.contains("Balance due: R150.00"));
        assertTrue(text.contains("PARTIALLY_PAID"));
    }

    @Test
    void outputDoesNotDependOnTheMachineLocale() {
        Locale original = Locale.getDefault();
        Locale.setDefault(Locale.forLanguageTag("en-ZA")); // uses a decimal comma by default
        try {
            String text = new PaymentSummaryPrinter().format(checkoutWithDeposit());
            assertTrue(text.contains("R150.00"));
        } finally {
            Locale.setDefault(original);
        }
    }

    @Test
    void saysSoWhenNothingHasBeenPaid() {
        Order order = new Order(1, new Customer(1, "Test Customer", "test@example.com"));
        String text = new PaymentSummaryPrinter().format(new OrderCheckout(order));

        assertTrue(text.contains("(no payments yet)"));
        assertTrue(text.contains("PENDING"));
    }
}
