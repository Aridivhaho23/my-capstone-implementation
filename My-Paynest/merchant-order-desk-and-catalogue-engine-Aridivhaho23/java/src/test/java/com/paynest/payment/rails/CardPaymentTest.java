package com.paynest.payment.rails;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.paynest.common.Money;
import com.paynest.payment.PaymentMethod;

import java.time.YearMonth;

import org.junit.jupiter.api.Test;

/** One CardPayment class, several ways of using a card (the channel enum). */
class CardPaymentTest {

    private static CardDetails validCard() {
        return new CardDetails("4111111111111111", YearMonth.now().plusYears(2), "123");
    }

    @Test
    void chipAndOnlineAcceptLargeAmounts() {
        assertTrue(new CardPayment(validCard(), CardChannel.CHIP_AND_PIN).processPayment(Money.of("12400.00")));
        assertTrue(new CardPayment(validCard(), CardChannel.ONLINE_3DS).processPayment(Money.of("12400.00")));
    }

    @Test
    void tapIsDeclinedAboveTheLimitButAcceptedBelowIt() {
        PaymentMethod tap = new CardPayment(validCard(), CardChannel.TAP);

        assertTrue(tap.processPayment(Money.of("500.00")));
        assertFalse(tap.processPayment(Money.of("500.01")));
    }

    @Test
    void everyChannelKeepsTheSameStableLabel() {
        for (CardChannel channel : CardChannel.values()) {
            assertEquals("CARD", new CardPayment(validCard(), channel).getPaymentType());
        }
    }

    @Test
    void defaultConstructorUsesOnline3ds() {
        assertTrue(new CardPayment(validCard()).processPayment(Money.of("1000.00")));
    }

    @Test
    void reverseDoesNotThrow() {
        new CardPayment(validCard()).reverse(Money.of("100.00"));
    }
}
