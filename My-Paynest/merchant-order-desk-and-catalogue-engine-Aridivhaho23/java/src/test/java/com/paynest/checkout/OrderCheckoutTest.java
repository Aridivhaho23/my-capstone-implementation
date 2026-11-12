package com.paynest.checkout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.paynest.common.Money;
import com.paynest.domain.Customer;
import com.paynest.domain.Order;
import com.paynest.domain.Product;
import com.paynest.payment.PaymentMethod;
import com.paynest.payment.PaymentOutcome;
import com.paynest.payment.PaymentProcessor;
import com.paynest.payment.rails.CardChannel;
import com.paynest.payment.rails.CardDetails;
import com.paynest.payment.rails.CardPayment;
import com.paynest.payment.rails.EftPayment;
import com.paynest.payment.rails.SplitPayment;
import com.paynest.payment.rails.WalletPayment;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.Test;

/** Integration tests: the real Capstone 1 Order -> OrderCheckout -> PaymentProcessor -> rails. */
class OrderCheckoutTest {

    /**
     * A brand-new rail that exists ONLY in the test folder. Neither Order, OrderCheckout nor
     * PaymentProcessor is edited to support it: this is the "new rail = new class" rule.
     */
    private static class VoucherPayment implements PaymentMethod {
        Money chargedAmount;
        int calls;

        @Override
        public boolean processPayment(Money amount) {
            calls++;
            chargedAmount = amount;
            return true;
        }

        @Override
        public String getPaymentType() {
            return "VOUCHER";
        }
    }

    private static CardPayment card() {
        return new CardPayment(new CardDetails("4111111111111111", YearMonth.now().plusYears(2), "123"),
                CardChannel.CHIP_AND_PIN);
    }

    /** R200.00 subtotal + R30.00 VAT = R230.00 payable. */
    private Order sampleOrder() {
        Order order = new Order(900, new Customer(900, "Test Customer", "test@example.com"));
        order.addItem(new Product(900, "Test product", new BigDecimal("100.00"), 10), 2);
        return order;
    }

    @Test
    void theChargedAmountIsTheVatInclusiveOrderTotal() {
        Order order = sampleOrder();
        OrderCheckout checkout = new OrderCheckout(order);
        VoucherPayment recorder = new VoucherPayment();

        assertEquals(Money.of("230.00"), checkout.getOrderTotal());
        assertTrue(checkout.checkout(recorder).isSuccess());

        // The rail received exactly what the order says is owed, with no second formula involved.
        assertEquals(Money.of(order.calculateTotalInclVat()), recorder.chargedAmount);
        assertEquals(PaymentStatus.COMPLETED, checkout.getStatus());
        assertEquals(Money.of("230.00"), checkout.getAmountPaid());
        assertTrue(checkout.getBalanceDue().isZero());
    }

    @Test
    void everyRailCanPayAnOrder() {
        List<PaymentMethod> rails = List.of(card(), new EftPayment(), new WalletPayment());

        for (PaymentMethod rail : rails) {
            OrderCheckout checkout = new OrderCheckout(sampleOrder());

            assertTrue(checkout.checkout(rail).isSuccess(), rail.getPaymentType() + " should succeed");
            assertEquals(PaymentStatus.COMPLETED, checkout.getStatus());
            assertEquals(rail.getPaymentType(), checkout.getPaymentHistory().get(0).paymentType());
        }
    }

    @Test
    void aNewRailWorksWithoutAnyChangeToOrderOrCheckout() {
        OrderCheckout checkout = new OrderCheckout(sampleOrder());

        assertTrue(checkout.checkout(new VoucherPayment()).isSuccess());
        assertEquals("VOUCHER", checkout.getPaymentHistory().get(0).paymentType());
    }

    @Test
    void splitPaymentPaysAnOrderThroughTheSameCheckout() {
        OrderCheckout checkout = new OrderCheckout(sampleOrder());
        WalletPayment wallet = new WalletPayment(Money.of("100.00"));

        PaymentOutcome outcome = checkout.checkout(new SplitPayment()
                .add(wallet, Money.of("100.00"))
                .add(new EftPayment(), Money.of("130.00")));

        assertTrue(outcome.isSuccess());
        assertEquals(PaymentStatus.COMPLETED, checkout.getStatus());
        assertEquals("SPLIT", checkout.getPaymentHistory().get(0).paymentType());
        assertTrue(wallet.getBalance().isZero());
    }

    @Test
    void aFailedSplitRollsBackAndLeavesTheOrderUntouched() {
        Order order = sampleOrder();
        OrderCheckout checkout = new OrderCheckout(order);
        WalletPayment first = new WalletPayment(Money.of("100.00"));

        PaymentOutcome outcome = checkout.checkout(new SplitPayment()
                .add(first, Money.of("100.00"))
                .add(new WalletPayment(Money.of("5.00")), Money.of("130.00"))); // second part is too poor

        assertEquals(PaymentOutcome.Status.DECLINED, outcome.status());
        assertEquals(Money.of("100.00"), first.getBalance()); // first part was given back
        assertEquals(PaymentStatus.PENDING, checkout.getStatus());
        assertFalse(order.isLocked());
    }

    @Test
    void instalmentThenBalanceCompletesTheOrder() {
        OrderCheckout checkout = new OrderCheckout(sampleOrder());

        assertTrue(checkout.payInstalment(new WalletPayment(Money.of("100.00")), Money.of("80.00")).isSuccess());
        assertEquals(PaymentStatus.PARTIALLY_PAID, checkout.getStatus());
        assertEquals(Money.of("150.00"), checkout.getBalanceDue());

        VoucherPayment recorder = new VoucherPayment();
        assertTrue(checkout.checkout(recorder).isSuccess());
        assertEquals(Money.of("150.00"), recorder.chargedAmount); // pays only what is still owed
        assertEquals(PaymentStatus.COMPLETED, checkout.getStatus());
        assertEquals(2, checkout.getPaymentHistory().size());
    }

    @Test
    void failedPaymentChangesNothing() {
        Order order = sampleOrder();
        OrderCheckout checkout = new OrderCheckout(order);

        PaymentOutcome outcome = checkout.checkout(new WalletPayment(Money.of("10.00")));

        assertEquals(PaymentOutcome.Status.DECLINED, outcome.status());
        assertEquals(PaymentStatus.PENDING, checkout.getStatus());
        assertEquals(Money.ZERO, checkout.getAmountPaid());
        assertTrue(checkout.getPaymentHistory().isEmpty());
        assertFalse(order.isLocked());
    }

    @Test
    void aDeclinedPaymentCanBeRetriedOnAnotherRail() {
        OrderCheckout checkout = new OrderCheckout(sampleOrder());

        assertFalse(checkout.checkout(new WalletPayment(Money.of("10.00"))).isSuccess());
        assertTrue(checkout.checkout(card()).isSuccess());
        assertEquals(PaymentStatus.COMPLETED, checkout.getStatus());
    }

    @Test
    void overpaymentAndInvalidInstalmentsAreRejectedWithoutCallingTheRail() {
        OrderCheckout checkout = new OrderCheckout(sampleOrder());
        VoucherPayment recorder = new VoucherPayment();

        assertEquals(PaymentOutcome.Status.REJECTED, checkout.payInstalment(recorder, Money.of("230.01")).status());
        assertEquals(PaymentOutcome.Status.REJECTED, checkout.payInstalment(recorder, Money.ZERO).status());
        assertEquals(PaymentOutcome.Status.REJECTED, checkout.payInstalment(recorder, null).status());
        assertEquals(0, recorder.calls);
        assertEquals(PaymentStatus.PENDING, checkout.getStatus());
    }

    @Test
    void aCompletedOrderIsNeverChargedAgain() {
        OrderCheckout checkout = new OrderCheckout(sampleOrder());
        VoucherPayment second = new VoucherPayment();
        assertTrue(checkout.checkout(new VoucherPayment()).isSuccess());

        PaymentOutcome again = checkout.checkout(second);

        assertEquals(PaymentOutcome.Status.REJECTED, again.status());
        assertEquals(0, second.calls);
        assertEquals(1, checkout.getPaymentHistory().size());
    }

    @Test
    void anEmptyOrderIsNeverPaid() {
        Order empty = new Order(1, new Customer(1, "Test Customer", "test@example.com"));
        OrderCheckout checkout = new OrderCheckout(empty);

        assertEquals(PaymentOutcome.Status.REJECTED, checkout.checkout(card()).status());
        assertEquals(PaymentStatus.PENDING, checkout.getStatus());
    }

    @Test
    void aMissingPaymentMethodIsRejected() {
        OrderCheckout checkout = new OrderCheckout(sampleOrder());

        assertEquals(PaymentOutcome.Status.REJECTED, checkout.checkout(null).status());
        assertEquals(PaymentStatus.PENDING, checkout.getStatus());
    }

    @Test
    void theFirstSuccessfulPaymentLocksTheOrderSoStockIsNeverLeaked() {
        Order order = sampleOrder();
        OrderCheckout checkout = new OrderCheckout(order);
        checkout.payInstalment(new EftPayment(), Money.of("50.00"));
        Product extra = new Product(901, "Extra product", new BigDecimal("10.00"), 2);

        assertTrue(order.isLocked());
        assertThrows(IllegalStateException.class, () -> order.addItem(extra, 1));
        assertEquals(2, extra.getStock()); // the refused add did not touch stock
        assertEquals(Money.of("230.00"), checkout.getOrderTotal()); // and the total did not move
    }

    @Test
    void paymentHistoryIsReadOnly() {
        OrderCheckout checkout = new OrderCheckout(sampleOrder());
        checkout.checkout(new EftPayment());

        assertThrows(UnsupportedOperationException.class, () -> checkout.getPaymentHistory().clear());
    }

    @Test
    void aRailThatThrowsNeverMarksAnOrderPaid() {
        PaymentMethod exploding = new PaymentMethod() {
            @Override
            public boolean processPayment(Money amount) {
                throw new IllegalStateException("gateway down");
            }

            @Override
            public String getPaymentType() {
                return "EXPLODING";
            }
        };
        OrderCheckout checkout = new OrderCheckout(sampleOrder(), new PaymentProcessor());

        assertEquals(PaymentOutcome.Status.ERROR, checkout.checkout(exploding).status());
        assertEquals(PaymentStatus.PENDING, checkout.getStatus());
    }

    @Test
    void constructorsRequireTheirDependencies() {
        assertThrows(NullPointerException.class, () -> new OrderCheckout(null));
        assertThrows(NullPointerException.class, () -> new OrderCheckout(sampleOrder(), null));
    }
}
