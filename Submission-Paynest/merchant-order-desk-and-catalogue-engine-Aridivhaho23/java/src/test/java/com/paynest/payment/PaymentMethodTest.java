package com.paynest.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.paynest.payment.rails.*;

import java.util.List;
import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

/**
 * Every variable below is typed as the INTERFACE (PaymentMethod). That is what proves
 * polymorphism is really used: the same calls work on every payment type.
 */
class PaymentMethodTest {

    @Test
    void checkoutCanUseDifferentPaymentMethods() {
        // The starter test shape from the brief.
        PaymentMethod card = new CardPayment();
        PaymentMethod wallet = new WalletPayment();

        assertTrue(card.processPayment(12400));
        assertTrue(wallet.processPayment(12400));
        assertEquals("CARD", card.getPaymentType());
        assertEquals("WALLET", wallet.getPaymentType());
    }

    @Test
    void eftWorksThroughTheSameInterface() {
        PaymentMethod eft = new EftPayment();

        assertTrue(eft.processPayment(12400));
        assertEquals("EFT", eft.getPaymentType());
    }

    @Test
    void voucherAndBitcoinUseTheSamePaymentMethodAbstraction() {
        PaymentMethod voucher = new VoucherPayment("VOUCHER-TEST", 500);
        PaymentMethod bitcoin = new BitcoinPayment("bc1qexample", () -> new BigDecimal("1000000"));

        assertTrue(voucher.processPayment(100));
        assertTrue(bitcoin.processPayment(100));
        assertEquals("VOUCHER", voucher.getPaymentType());
        assertEquals("BITCOIN", bitcoin.getPaymentType());
    }

    @Test
    void everyRailExtendsTheAbstractBaseClassAndKeepsAStableLabel() {
        List<PaymentMethod> rails = List.of(new CardPayment(), new EftPayment(), new WalletPayment(),
                new VoucherPayment("VOUCHER-TEST", 500),
                new BitcoinPayment("bc1qexample", () -> new BigDecimal("1000000")));

        for (PaymentMethod rail : rails) {
            assertTrue(rail instanceof AbstractPaymentMethod);
            assertEquals(rail.getPaymentType(), rail.getPaymentType()); // same label on every call
        }
    }

    @Test
    void theBaseClassRejectsInvalidAmountsForEveryRail() {
        List<PaymentMethod> rails = List.of(new CardPayment(), new EftPayment(), new WalletPayment(),
                new VoucherPayment("VOUCHER-TEST", 500),
                new BitcoinPayment("bc1qexample", () -> new BigDecimal("1000000")));

        for (PaymentMethod rail : rails) {
            assertFalse(rail.processPayment(0), rail.getPaymentType() + " must reject 0");
            assertFalse(rail.processPayment(-50), rail.getPaymentType() + " must reject a negative amount");
            assertFalse(rail.processPayment(Double.NaN), rail.getPaymentType() + " must reject NaN");
            assertFalse(rail.processPayment(Double.POSITIVE_INFINITY), rail.getPaymentType() + " must reject infinity");
        }
    }

    @Test
    void walletFailsWhenBalanceIsTooLowAndKeepsItsBalance() {
        WalletPayment wallet = new WalletPayment(1000);

        assertFalse(wallet.processPayment(1500));
        assertEquals(1000.0, wallet.getBalance(), 0.001);
    }

    @Test
    void walletDebitsOnSuccessAndCanPayItsExactBalance() {
        WalletPayment wallet = new WalletPayment(2000);

        assertTrue(wallet.processPayment(500));
        assertEquals(1500.0, wallet.getBalance(), 0.001);
        assertTrue(wallet.processPayment(1500));
        assertEquals(0.0, wallet.getBalance(), 0.001);
    }

    @Test
    void walletRejectsAnInvalidOpeningBalance() {
        assertThrows(IllegalArgumentException.class, () -> new WalletPayment(-1));
        assertThrows(IllegalArgumentException.class, () -> new WalletPayment(Double.NaN));
    }

    @Test
    void baseClassRejectsABlankLabel() {
        assertThrows(IllegalArgumentException.class, () -> new AbstractPaymentMethod(" ") {
            @Override
            protected boolean authorise(double amount) {
                return true;
            }
        });
    }
}
