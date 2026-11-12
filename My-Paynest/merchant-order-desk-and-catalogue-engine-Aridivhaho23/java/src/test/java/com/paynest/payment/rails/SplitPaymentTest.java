package com.paynest.payment.rails;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.paynest.common.Money;

import java.time.YearMonth;

import org.junit.jupiter.api.Test;

/** Split payments: several rails, one amount, all-or-nothing. */
class SplitPaymentTest {

    private static CardPayment card() {
        return new CardPayment(new CardDetails("4111111111111111", YearMonth.now().plusYears(2), "123"));
    }

    @Test
    void chargesEachPartWhenTheyAddUpToTheTotal() {
        WalletPayment wallet = new WalletPayment(Money.of("5000.00"));
        SplitPayment split = new SplitPayment()
                .add(wallet, Money.of("5000.00"))
                .add(card(), Money.of("7400.00"));

        assertTrue(split.processPayment(Money.of("12400.00")));
        assertTrue(wallet.getBalance().isZero());
    }

    @Test
    void rejectsPartsThatDoNotAddUpAndChargesNothing() {
        WalletPayment wallet = new WalletPayment(Money.of("5000.00"));
        SplitPayment split = new SplitPayment()
                .add(wallet, Money.of("5000.00"))
                .add(card(), Money.of("7000.00")); // R12000, but R12400 is due.

        assertFalse(split.processPayment(Money.of("12400.00")));
        assertEquals(Money.of("5000.00"), wallet.getBalance());
    }

    @Test
    void rollsBackEarlierPartsWhenALaterPartFails() {
        WalletPayment first = new WalletPayment(Money.of("5000.00"));
        WalletPayment secondTooPoor = new WalletPayment(Money.of("100.00"));
        SplitPayment split = new SplitPayment()
                .add(first, Money.of("5000.00"))
                .add(secondTooPoor, Money.of("7400.00"));

        assertFalse(split.processPayment(Money.of("12400.00")));
        // The first wallet was debited, then given back.
        assertEquals(Money.of("5000.00"), first.getBalance());
        assertEquals(Money.of("100.00"), secondTooPoor.getBalance());
    }

    @Test
    void emptySplitFails() {
        assertFalse(new SplitPayment().processPayment(Money.of("100.00")));
    }

    @Test
    void rejectsInvalidParts() {
        SplitPayment split = new SplitPayment();

        assertThrows(IllegalArgumentException.class, () -> split.add(card(), Money.ZERO));
        assertThrows(IllegalArgumentException.class, () -> split.add(card(), Money.of("-5")));
        assertThrows(NullPointerException.class, () -> split.add(null, Money.of("5")));
    }

    @Test
    void reverseUndoesAllPartsButOnlyInFull() {
        WalletPayment wallet = new WalletPayment(Money.of("1000.00"));
        SplitPayment split = new SplitPayment().add(wallet, Money.of("400.00")).add(card(), Money.of("600.00"));
        split.processPayment(Money.of("1000.00"));

        assertThrows(IllegalArgumentException.class, () -> split.reverse(Money.of("100.00")));
        split.reverse(Money.of("1000.00"));
        assertEquals(Money.of("1000.00"), wallet.getBalance());
    }
}
