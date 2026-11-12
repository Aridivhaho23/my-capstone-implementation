package com.paynest.app;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import com.paynest.checkout.CheckoutService;
import com.paynest.domain.Customer;
import com.paynest.domain.Order;
import com.paynest.domain.Product;
import com.paynest.payment.PaymentMethod;
import com.paynest.payment.PaymentProcessor;
import com.paynest.payment.rails.BitcoinPayment;
import com.paynest.payment.rails.BuyNowPayLaterPayment;
import com.paynest.payment.rails.CardDetails;
import com.paynest.payment.rails.CardPayment;
import com.paynest.payment.rails.EftPayment;
import com.paynest.payment.rails.SplitPayment;
import com.paynest.payment.rails.VoucherPayment;
import com.paynest.payment.rails.WalletPayment;
import com.paynest.service.OrderService;
import com.paynest.service.OrderSummaryPrinter;

/** Application entry point and executable demonstration of every supported payment method. */
public final class PayNestApplication {
    private PayNestApplication() { }

    public static void main(String[] args) {
        CheckoutService checkoutService = new CheckoutService(new PaymentProcessor());
        Order order = buildOrder(1);
        System.out.println(new OrderSummaryPrinter().printSummary(order));
        demonstrateAllPaymentMethods(checkoutService);
        demonstrateCardValidation();
    }

    private static Order buildOrder(int orderId) {

        Product laptop = new Product(orderId * 10 + 1, "Laptop", new BigDecimal("12000.00"), 5);
        Product mouse = new Product(orderId * 10 + 2, "Wireless Mouse", new BigDecimal("350.00"), 20);
        Product keyboard = new Product(orderId * 10 + 3, "Mechanical Keyboard", new BigDecimal("1500.00"), 10);

        Customer customer = new Customer(orderId, "Ari Nemadodzi", "ari@example.com");

        OrderService service = new OrderService();
        Order order = service.createOrder(orderId, customer);

        service.addToOrder(order, laptop, 1);
        service.addToOrder(order, mouse, 3);
        service.addToOrder(order, keyboard, 2);
        
        /*try {
            service.addToOrder(order, mouse, 100);
        } catch (InsufficientStockException ex) {
            System.out.println("Rejected as expected: " + ex.getMessage());
        }*/
        return order;
    }

    /** Convenience entry point for the payment-rail demonstration. */
    public static void demonstrateAllPaymentMethods()
    {
        demonstrateAllPaymentMethods(new CheckoutService(new PaymentProcessor()));
    }

    /** Runs all payment rails in one launch, each with a fresh order (no double charging). */
    public static void demonstrateAllPaymentMethods(CheckoutService checkoutService) {
        List<PaymentMethod> methods = List.of(
                new CardPayment(new CardDetails("4111 1111 1111 1111", YearMonth.now().plusYears(2), "123")),
                new EftPayment(),
                new WalletPayment(100_000.00),
                new VoucherPayment("DEMO-VOUCHER-1234", 100_000.00),
                new BitcoinPayment("bc1qdemodestinationaddress", () -> new BigDecimal("1000000.00")),
                new BuyNowPayLaterPayment(100_000.00),
                new SplitPayment(List.of(
                        new SplitPayment.Allocation(new WalletPayment(10_000.00), 5_000.00),
                        new SplitPayment.Allocation(new VoucherPayment("SPLIT-DEMO-1234", 100_000.00), 13_457.50)))
        );
        int id = 2;
        for (PaymentMethod method : methods) {
            System.out.println("\n--- Testing " + method.getPaymentType() + " ---");
            checkoutService.checkout(buildOrder(id++), method);
        }
    }

    private static void demonstrateCardValidation()
    {
        try {
            new CardDetails("4111 1111 1111 1111", YearMonth.now().plusYears(2), "1234");
        } catch (IllegalArgumentException ex) {
            System.out.println("Card rejected as expected: " + ex.getMessage());
        }
    }
}