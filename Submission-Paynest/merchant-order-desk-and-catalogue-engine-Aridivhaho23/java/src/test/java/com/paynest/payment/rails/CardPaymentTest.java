package com.paynest.payment.rails;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class CardPaymentTest {

    private static CardDetails validCard() {
        return new CardDetails("4111111111111111", YearMonth.now().plusYears(2), "123");
    }

    @Test
    void aValidCardPaysAndOnlyTheMaskedNumberIsPrinted() {
        PrintStream original = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        System.setOut(new PrintStream(captured));
        boolean paid;
        try {
            paid = new CardPayment(validCard()).processPayment(12400);
        } finally {
            System.setOut(original);
        }

        String output = captured.toString();
        assertTrue(paid);
        assertTrue(output.contains("[CARD]"));
        assertTrue(output.contains("R12400.00"));
        assertTrue(output.contains("**** **** **** 1111"));
        assertFalse(output.contains("4111111111111111"));
    }

    @Test
    void aCardPaymentCannotBeCreatedWithoutAValidCard() {
        assertThrows(NullPointerException.class, () -> new CardPayment(null));
        // Invalid card data never even becomes a CardDetails, so it can never reach a CardPayment.
        assertThrows(IllegalArgumentException.class,
                () -> new CardPayment(new CardDetails("4111111111111112", YearMonth.now().plusYears(2), "123")));
        assertThrows(IllegalArgumentException.class,
                () -> new CardPayment(new CardDetails("4111111111111111", YearMonth.now().plusYears(2), "12")));
    }
}
