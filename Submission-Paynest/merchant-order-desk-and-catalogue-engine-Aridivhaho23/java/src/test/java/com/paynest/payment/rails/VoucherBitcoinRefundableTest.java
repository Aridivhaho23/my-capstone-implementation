package com.paynest.payment.rails;

import com.paynest.payment.PaymentMethod;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class VoucherBitcoinRefundableTest {

    @Test
    void voucherPaysWhenBalanceCoversAmountAndDeductsCorrectly() {
        VoucherPayment voucher = new VoucherPayment("VOUCHER-123456", 500.00);

        assertTrue(voucher.processPayment(230.00));
        assertEquals(270.00, voucher.getBalance(), 0.001);
    }

    @Test
    void voucherRejectsInsufficientBalanceWithoutChangingIt() {
        VoucherPayment voucher = new VoucherPayment("VOUCHER-123456", 100.00);

        assertFalse(voucher.processPayment(230.00));
        assertEquals(100.00, voucher.getBalance(), 0.001);
    }

    @Test
    void voucherRejectsMissingCodeAndNegativeBalance() {
        assertThrows(IllegalArgumentException.class, () -> new VoucherPayment(" ", 100));
        assertThrows(IllegalArgumentException.class, () -> new VoucherPayment("ABC123", -1));
    }

    @Test
    void bitcoinConvertsZarUsingInjectedQuoteWithoutHardCodingMarketPrice() {
        BitcoinPayment bitcoin = new BitcoinPayment(
                "bc1qexampledestinationaddress000000000000",
                () -> new BigDecimal("1000000.00"));

        java.io.ByteArrayOutputStream captured = new java.io.ByteArrayOutputStream();
        java.io.PrintStream original = System.out;
        System.setOut(new java.io.PrintStream(captured));
        try {
            assertTrue(bitcoin.processPayment(250.00));
        } finally {
            System.setOut(original);
        }
        assertEquals("BITCOIN", bitcoin.getPaymentType());
        assertTrue(captured.toString().contains("0.00025000 BTC"));
    }

    @Test
    void bitcoinRejectsMissingAddressOrRateProvider() {
        assertThrows(IllegalArgumentException.class,
                () -> new BitcoinPayment(" ", () -> new BigDecimal("1000000")));
        assertThrows(NullPointerException.class,
                () -> new BitcoinPayment("bc1qexample", null));
    }

    @Test
    void bitcoinRejectsInvalidExchangeRateQuote() {
        BitcoinPayment bitcoin = new BitcoinPayment("bc1qexample", () -> BigDecimal.ZERO);
        assertFalse(bitcoin.processPayment(100));
    }

    @Test
    void cardRefundsOnlyPreviouslyChargedAmountAndSupportsPartialRefunds() {
        CardPayment card = new CardPayment();

        assertFalse(card.refund(10)); // no successful charge exists yet
        assertTrue(card.processPayment(200));
        assertTrue(card.refund(75));
        assertEquals(125.00, card.getRefundableBalance(), 0.001);
        assertFalse(card.refund(126));
        assertTrue(card.refund(125));
        assertEquals(0.00, card.getRefundableBalance(), 0.001);
        assertFalse(card.refund(1)); // cannot refund the same payment twice
    }

    @Test
    void walletRefundReturnsFundsToWalletAndCannotExceedOriginalDebits() {
        WalletPayment wallet = new WalletPayment(500);

        assertTrue(wallet.processPayment(200));
        assertEquals(300.00, wallet.getBalance(), 0.001);
        assertTrue(wallet.refund(50));
        assertEquals(350.00, wallet.getBalance(), 0.001);
        assertFalse(wallet.refund(151));
        assertTrue(wallet.refund(150));
        assertEquals(500.00, wallet.getBalance(), 0.001);
    }

    @Test
    void refundRejectsZeroNegativeAndNonFiniteAmounts() {
        Refundable card = new CardPayment();
        assertFalse(card.refund(0));
        assertFalse(card.refund(-1));
        assertFalse(card.refund(Double.NaN));
        assertFalse(card.refund(Double.POSITIVE_INFINITY));
    }

    @Test
    void bitcoinIsNotDirectlyRefundableButVoucherCanRestoreItsBalance() {
        PaymentMethod bitcoin = new BitcoinPayment("bc1qexample", () -> new BigDecimal("1000000"));
        VoucherPayment voucher = new VoucherPayment("VOUCHER-1234", 500);

        assertFalse(bitcoin instanceof Refundable);
        assertTrue(voucher instanceof Refundable);
        assertTrue(voucher.processPayment(200));
        assertEquals(300, voucher.getBalance(), 0.001);
        assertTrue(((Refundable) voucher).refund(75));
        assertEquals(375, voucher.getBalance(), 0.001);
        assertFalse(((Refundable) voucher).refund(126));
    }
}
