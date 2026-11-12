package com.paynest.payment;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/** The shared processor treats every rail the same and survives bad input. */
class PaymentProcessorTest {

    /** A payment type written ONLY for tests. It records what it was asked to do. */
    private static class StubPayment implements PaymentMethod {
        private final boolean result;
        private final boolean throwsError;
        int calls = 0;
        double lastAmount = -1;

        StubPayment(boolean result, boolean throwsError) {
            this.result = result;
            this.throwsError = throwsError;
        }

        @Override
        public boolean processPayment(double amount) {
            calls++;
            lastAmount = amount;
            if (throwsError) {
                throw new IllegalStateException("gateway down");
            }
            return result;
        }

        @Override
        public String getPaymentType() {
            return "STUB";
        }
    }

    private final PaymentProcessor processor = new PaymentProcessor();

    /** Runs the action while capturing what it prints. */
    private static String captureOutput(Runnable action) {
        PrintStream original = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        System.setOut(new PrintStream(captured));
        try {
            action.run();
        } finally {
            System.setOut(original);
        }
        return captured.toString();
    }

    @Test
    void passesTheExactAmountToTheRailAndReportsSuccess() {
        StubPayment stub = new StubPayment(true, false);

        assertTrue(processor.processPayment(stub, 12400));
        assertEquals(1, stub.calls);
        assertEquals(12400.0, stub.lastAmount, 0.001);
    }

    @Test
    void successPrintsTheRailAndTheAmount() {
        String output = captureOutput(() -> processor.processPayment(new StubPayment(true, false), 12400));

        assertTrue(output.contains("Payment successful via STUB"));
        assertTrue(output.contains("Amount: R12400.00"));
    }

    @Test
    void failurePrintsTheRailAndReturnsFalse() {
        StubPayment stub = new StubPayment(false, false);
        boolean[] result = new boolean[1];

        String output = captureOutput(() -> result[0] = processor.processPayment(stub, 500));

        assertFalse(result[0]);
        assertTrue(output.contains("Payment failed via STUB"));
    }

    @Test
    void aThrowingRailCountsAsAFailedPaymentNotACrash() {
        assertFalse(processor.processPayment(new StubPayment(true, true), 500));
    }

    @Test
    void aMissingPaymentMethodFailsCleanly() {
        assertFalse(processor.processPayment(null, 500));
    }
}
