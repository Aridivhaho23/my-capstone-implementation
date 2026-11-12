package com.paynest.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.paynest.common.Money;

import org.junit.jupiter.api.Test;

/** The shared processor treats every rail the same, survives bad input, and reports outcomes. */
class PaymentProcessorTest {

    /** A rail written ONLY for tests; it records what it was asked to do. */
    private static class StubPayment implements PaymentMethod {
        private final boolean result;
        private final boolean throwsError;
        int calls = 0;
        Money lastAmount;

        StubPayment(boolean result, boolean throwsError) {
            this.result = result;
            this.throwsError = throwsError;
        }

        @Override
        public boolean processPayment(Money amount) {
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

    @Test
    void successPassesTheExactAmountToTheRail() {
        StubPayment stub = new StubPayment(true, false);

        PaymentOutcome outcome = processor.processPayment(stub, Money.of("12400.00"));

        assertTrue(outcome.isSuccess());
        assertEquals(PaymentOutcome.Status.SUCCESS, outcome.status());
        assertEquals("STUB", outcome.paymentType());
        assertEquals(Money.of("12400.00"), outcome.amount());
        assertEquals(1, stub.calls);
        assertEquals(Money.of("12400.00"), stub.lastAmount);
    }

    @Test
    void aDecliningRailGivesDeclined() {
        PaymentOutcome outcome = processor.processPayment(new StubPayment(false, false), Money.of("500.00"));

        assertFalse(outcome.isSuccess());
        assertEquals(PaymentOutcome.Status.DECLINED, outcome.status());
    }

    @Test
    void aThrowingRailGivesErrorNotACrashAndNeverSuccess() {
        PaymentOutcome outcome = processor.processPayment(new StubPayment(true, true), Money.of("500.00"));

        assertEquals(PaymentOutcome.Status.ERROR, outcome.status());
        assertTrue(outcome.message().contains("gateway down"));
    }

    @Test
    void invalidAmountsAreRejectedWithoutCallingTheRail() {
        StubPayment stub = new StubPayment(true, false);

        assertEquals(PaymentOutcome.Status.REJECTED, processor.processPayment(stub, Money.ZERO).status());
        assertEquals(PaymentOutcome.Status.REJECTED, processor.processPayment(stub, Money.of("-50")).status());
        assertEquals(PaymentOutcome.Status.REJECTED, processor.processPayment(stub, null).status());
        assertEquals(0, stub.calls);
    }

    @Test
    void missingPaymentMethodIsRejected() {
        PaymentOutcome outcome = processor.processPayment(null, Money.of("500.00"));

        assertEquals(PaymentOutcome.Status.REJECTED, outcome.status());
        assertEquals("NONE", outcome.paymentType());
    }

    @Test
    void outcomeRequiresItsMandatoryFields() {
        org.junit.jupiter.api.Assertions.assertThrows(NullPointerException.class,
                () -> new PaymentOutcome(null, "X", null, "m"));
        assertNull(PaymentOutcome.declined("X", null).amount());
    }
}
