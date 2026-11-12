package com.paynest.payment.rails;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

class SplitAndBuyNowPayLaterTest {

    @Test
    void splitPaymentChargesTwoMethodsForExactCombinedAmount() {
        WalletPayment wallet = new WalletPayment(100);
        VoucherPayment voucher = new VoucherPayment("SPLIT-1234", 200);
        SplitPayment split = new SplitPayment(List.of(
                new SplitPayment.Allocation(wallet, 100),
                new SplitPayment.Allocation(voucher, 130)));

        assertTrue(split.processPayment(230));
        assertEquals(0, wallet.getBalance(), 0.001);
        assertEquals(70, voucher.getBalance(), 0.001);
        assertEquals("SPLIT", split.getPaymentType());
    }

    @Test
    void splitPaymentRejectsAllocationsThatDoNotMatchCheckoutTotal() {
        SplitPayment split = new SplitPayment(List.of(
                new SplitPayment.Allocation(new WalletPayment(500), 100),
                new SplitPayment.Allocation(new CardPayment(), 100)));
        assertFalse(split.processPayment(230));
    }

    @Test
    void splitPaymentStopsWhenAnAllocationIsDeclined() {
        WalletPayment wallet = new WalletPayment(50);
        SplitPayment split = new SplitPayment(List.of(
                new SplitPayment.Allocation(wallet, 100),
                new SplitPayment.Allocation(new WalletPayment(500), 130)));
        assertFalse(split.processPayment(230));
        assertEquals(50, wallet.getBalance(), 0.001);
    }

    @Test
    void splitPaymentRefundsEarlierAllocationWhenLaterRailDeclines() {
        WalletPayment wallet = new WalletPayment(200);
        VoucherPayment voucher = new VoucherPayment("VOUCHER-1", 50);
        SplitPayment split = new SplitPayment(List.of(
                new SplitPayment.Allocation(wallet, 100),
                new SplitPayment.Allocation(voucher, 100)));

        assertFalse(split.processPayment(200));
        assertEquals(200, wallet.getBalance(), 0.001,
                "the earlier wallet debit must be compensated");
        assertEquals(50, voucher.getBalance(), 0.001,
                "the declined voucher must remain unchanged");
    }

    @Test
    void splitPaymentRejectsRailsThatCannotBeCompensated() {
        assertThrows(IllegalArgumentException.class, () -> new SplitPayment(List.of(
                new SplitPayment.Allocation(new WalletPayment(200), 100),
                new SplitPayment.Allocation(new EftPayment(), 100))));
    }

    @Test
    void bnplApprovesWithinLimitAndTracksRepayments() {
        BuyNowPayLaterPayment bnpl = new BuyNowPayLaterPayment(500);
        assertTrue(bnpl.processPayment(230));
        assertEquals(230, bnpl.getOutstandingBalance(), 0.001);
        assertTrue(bnpl.recordRepayment(100));
        assertEquals(130, bnpl.getOutstandingBalance(), 0.001);
        assertFalse(bnpl.recordRepayment(131));
    }

    @Test
    void bnplDeclinesWhenCreditLimitWouldBeExceeded() {
        BuyNowPayLaterPayment bnpl = new BuyNowPayLaterPayment(200);
        assertFalse(bnpl.processPayment(230));
        assertEquals(0, bnpl.getOutstandingBalance(), 0.001);
    }

    @Test
    void bnplRejectsInvalidCreditLimit() {
        assertThrows(IllegalArgumentException.class, () -> new BuyNowPayLaterPayment(-1));
    }
}
