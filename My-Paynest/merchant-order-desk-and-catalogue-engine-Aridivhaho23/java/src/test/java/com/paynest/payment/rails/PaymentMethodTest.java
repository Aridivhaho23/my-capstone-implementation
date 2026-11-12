package com.paynest.payment.rails;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.paynest.common.Money;
import com.paynest.payment.PaymentMethod;

import java.time.YearMonth;

import org.junit.jupiter.api.Test;

/** Rails used through the INTERFACE type, which proves polymorphism is really used. */
class PaymentMethodTest {

    private static CardDetails validCard() {
        return new CardDetails("4111111111111111", YearMonth.now().plusYears(2), "123");
    }

    @Test
    void differentRailsWorkThroughTheSameInterface() {
        PaymentMethod card = new CardPayment(validCard());
        PaymentMethod wallet = new WalletPayment();
        PaymentMethod eft = new EftPayment(EftMode.INSTANT);

        assertTrue(card.processPayment(Money.of("12400.00")));
        assertTrue(wallet.processPayment(Money.of("12400.00")));
        assertTrue(eft.processPayment(Money.of("12400.00")));
        assertEquals("CARD", card.getPaymentType());
        assertEquals("WALLET", wallet.getPaymentType());
        assertEquals("EFT", eft.getPaymentType());
    }

    @Test
    void walletFailsWhenBalanceIsTooLowAndKeepsItsBalance() {
        WalletPayment wallet = new WalletPayment(Money.of("1000.00"));

        assertFalse(wallet.processPayment(Money.of("1500.00")));
        assertEquals(Money.of("1000.00"), wallet.getBalance());
    }

    @Test
    void walletDebitsOnSuccessAndCreditsBackOnReverse() {
        WalletPayment wallet = new WalletPayment(Money.of("2000.00"));

        assertTrue(wallet.processPayment(Money.of("500.00")));
        assertEquals(Money.of("1500.00"), wallet.getBalance());

        wallet.reverse(Money.of("500.00"));
        assertEquals(Money.of("2000.00"), wallet.getBalance());
    }

    @Test
    void walletCanPayItsExactBalance() {
        WalletPayment wallet = new WalletPayment(Money.of("100.00"));

        assertTrue(wallet.processPayment(Money.of("100.00")));
        assertTrue(wallet.getBalance().isZero());
    }
}
