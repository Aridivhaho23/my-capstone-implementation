package com.paynest.checkout;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.paynest.payment.*;
import com.paynest.payment.rails.*;
import com.paynest.domain.*;


/** Checkout use-case integration tests: Order itself does not perform checkout. */
class OrderCheckoutTest {
    private CheckoutService checkoutService() {
        return new CheckoutService(new PaymentProcessor());
    }

    /** R200.00 subtotal + R30.00 VAT = R230.00 grand total. */
    private Order sampleOrder() {
        Order order = new Order(900, new Customer(900, "Test Customer", "test@example.com"));
        order.addItem(new Product(900, "Test product", new BigDecimal("100.00"), 10), 2);
        return order;
    }

    @Test
    void checkoutServiceUsesInjectedPaymentProcessingAbstraction() {
        final double[] capturedAmount = { -1 };
        PaymentProcessing fakeProcessor = (method, amount) -> {
            capturedAmount[0] = amount;
            return true;
        };
        Order order = sampleOrder();

        assertTrue(new CheckoutService(fakeProcessor).checkout(order, new EftPayment()));
        assertEquals(230.00, capturedAmount[0], 0.001);
        assertTrue(order.isPaid());
    }

    @Test
    void theAmountChargedIsExactlyTheOrderGrandTotal() {
        Order order = sampleOrder();
        BuyNowPayLaterPayment recorder = new BuyNowPayLaterPayment();

        assertTrue(checkoutService().checkout(order, recorder));
        assertEquals(230.00, recorder.getOutstandingBalance(), 0.001);
        assertEquals(order.calculateTotalInclVat().doubleValue(), recorder.getOutstandingBalance(), 0.001);
    }

    @Test
    void checkoutWorksWithEveryPaymentTypeAndCompletesTheOrder() {
        List<PaymentMethod> rails = List.of(new CardPayment(), new EftPayment(), new WalletPayment(),
                new VoucherPayment("VOUCHER-ORDER", 1000),
                new BitcoinPayment("bc1qorderexample", () -> new BigDecimal("1000000")));
        for (PaymentMethod rail : rails) {
            Order order = sampleOrder();
            assertTrue(checkoutService().checkout(order, rail), rail.getPaymentType() + " should succeed");
            assertTrue(order.isPaid());
            assertEquals(PaymentStatus.PAID, order.getPaymentStatus());
        }
    }

    @Test
    void splitPaymentWorksThroughCheckoutService() {
        Order order = sampleOrder();
        SplitPayment split = new SplitPayment(List.of(
                new SplitPayment.Allocation(new WalletPayment(100), 100),
                new SplitPayment.Allocation(new VoucherPayment("SPLIT-ORDER", 130), 130)));
        assertTrue(checkoutService().checkout(order, split));
        assertTrue(order.isPaid());
    }

    @Test
    void newPaymentTypeWorksWithoutChangingOrder() {
        Order order = sampleOrder();
        assertTrue(checkoutService().checkout(order, new BuyNowPayLaterPayment()));
        assertTrue(order.isPaid());
        assertEquals(PaymentStatus.PAID, order.getPaymentStatus());
    }

    @Test
    void failedPaymentLeavesOrderUnpaidAndAllowsRetry() {
        Order order = sampleOrder();
        CheckoutService checkout = checkoutService();
        assertFalse(checkout.checkout(order, new WalletPayment(10)));
        assertFalse(order.isPaid());
        assertEquals(PaymentStatus.PAYMENT_FAILED, order.getPaymentStatus());
        assertTrue(checkout.checkout(order, new EftPayment()));
        assertTrue(order.isPaid());
        assertEquals(PaymentStatus.PAID, order.getPaymentStatus());
    }

    @Test
    void paidOrderIsNeverChargedTwice() {
        Order order = sampleOrder();
        CheckoutService checkout = checkoutService();
        BuyNowPayLaterPayment second = new BuyNowPayLaterPayment();
        assertTrue(checkout.checkout(order, new BuyNowPayLaterPayment()));
        assertFalse(checkout.checkout(order, second));
        assertEquals(0.0, second.getOutstandingBalance(), 0.001);
    }

    @Test
    void paidOrderRefusesNewItemsAndLeavesStockUntouched() {
        Order order = sampleOrder();
        checkoutService().checkout(order, new EftPayment());
        Product extra = new Product(901, "Extra product", new BigDecimal("10.00"), 2);
        assertThrows(IllegalStateException.class, () -> order.addItem(extra, 1));
        assertEquals(2, extra.getStock());
        assertEquals(0, new BigDecimal("230.00").compareTo(order.calculateTotalInclVat()));
    }

    @Test
    void emptyOrderIsNeverPaid() {
        Order empty = new Order(1, new Customer(1, "Test Customer", "test@example.com"));
        assertFalse(checkoutService().checkout(empty, new CardPayment()));
        assertFalse(empty.isPaid());
    }

    @Test
    void missingPaymentMethodFailsCleanly() {
        Order order = sampleOrder();
        assertFalse(checkoutService().checkout(order, null));
        assertFalse(order.isPaid());
    }

    @Test
    void consoleOutputShowsRailAmountAndCompletion() {
        PrintStream original = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        System.setOut(new PrintStream(captured));
        try {
            checkoutService().checkout(sampleOrder(), new EftPayment());
        } finally {
            System.setOut(original);
        }
        String output = captured.toString();
        assertTrue(output.contains("[EFT]"));
        assertTrue(output.contains("Payment successful via EFT"));
        assertTrue(output.contains("Amount: R230.00"));
        assertTrue(output.contains("completed via EFT"));
    }

    @Test
    void orderEntityHasNoCheckoutOrPaymentProcessorDependency() throws Exception {
        assertTrue(java.util.Arrays.stream(Order.class.getDeclaredMethods())
                .noneMatch(method -> method.getName().equals("checkout")));
        assertTrue(java.util.Arrays.stream(Order.class.getDeclaredFields())
                .noneMatch(field -> PaymentProcessing.class.isAssignableFrom(field.getType())));
    }
}
