package com.paynest.app;

import java.math.BigDecimal;
import java.time.YearMonth;

import com.paynest.checkout.OrderCheckout;
import com.paynest.checkout.PaymentSummaryPrinter;
import com.paynest.common.Money;
import com.paynest.domain.Customer;
import com.paynest.domain.InsufficientStockException;
import com.paynest.domain.Order;
import com.paynest.domain.Product;
import com.paynest.payment.PaymentOutcome;
import com.paynest.payment.rails.CardChannel;
import com.paynest.payment.rails.CardDetails;
import com.paynest.payment.rails.CardPayment;
import com.paynest.payment.rails.EftMode;
import com.paynest.payment.rails.EftPayment;
import com.paynest.payment.rails.SplitPayment;
import com.paynest.payment.rails.WalletPayment;
import com.paynest.service.OrderService;
import com.paynest.service.OrderSummaryPrinter;

/**
 * The single entry point and the composition root: the ONLY place concrete payment rails are
 * chosen, and the ONLY class that prints. Every other layer returns values.
 *
 * <p>Part 1 is the Capstone 1 demo (catalogue, order, summary). Part 2 pays that SAME order
 * with the Capstone 3 payment rails.
 */
public final class PayNestApplication {

    private PayNestApplication() { }

    public static void main(String[] args) {
        Order order = PlacingAnOrder();
        PayingAnOrder(order);
    }

    // -------------------------------------- Capstone 1 -----------------------------------

    private static Order PlacingAnOrder() {
        System.out.println("=== Capstone 1: order desk and catalogue engine");
        Product laptop = new Product(1, "Laptop", new BigDecimal("12000.00"), 5);
        Product mouse  = new Product(2, "Wireless Mouse", new BigDecimal("350.00"), 20);
        Product keyboard = new Product(3, "Mechanical Keyboard", new BigDecimal("1500.00"), 10);

        Customer customer = new Customer(1, "Ari Nemadodzi", "ari@example.com");

        OrderService orderService = new OrderService();
        Order order = orderService.createOrder(1, customer);

        // quantity > 1, per the demo requirement
        orderService.addToOrder(order, laptop, 1);
        orderService.addToOrder(order, mouse, 3);
        orderService.addToOrder(order, keyboard, 2);

        // deliberately over stock
        try {
            orderService.addToOrder(order, mouse, 100);
        } catch (InsufficientStockException ex) {
            System.out.println("Rejected as expected: " + ex.getMessage());
        }

        System.out.println(new OrderSummaryPrinter().printSummary(order));
        return order;
    }

    // ---------------------------------- Capstone 3 --------------------------------

    private static void PayingAnOrder(Order order) {
        System.out.println("Customer Checkout: paying that order ===");
        // The checkout takes its amount from Order.calculateTotalInclVat(): no second formula.
        OrderCheckout checkout = new OrderCheckout(order);
        System.out.println("Amount to pay (incl. VAT): " + checkout.getOrderTotal());

        // 1. A deposit by instant EFT. This also LOCKS the order: its items can no longer change.
        show(checkout.payInstalment(new EftPayment(EftMode.INSTANT), Money.of("5000.00")));

        // 2. A wallet with too little money is declined and changes nothing.
        show(checkout.checkout(new WalletPayment(Money.of("100.00"))));

        // 3. Items can no longer be added once payment has started.
        try {
            order.addItem(new Product(9, "Late Item", new BigDecimal("10.00"), 5), 1);
        } catch (IllegalStateException ex) {
            System.out.println("Rejected as expected: " + ex.getMessage());
        }

        // 4. Pay the rest with ONE split payment: wallet + card. The checkout cannot tell a
        //    split from a single rail, and the card details are validated when created.
        Money balance = checkout.getBalanceDue();
        Money fromWallet = Money.of("2000.00");
        CardDetails card = new CardDetails("4111 1111 1111 1111", YearMonth.now().plusYears(2), "123");
        show(checkout.checkout(new SplitPayment()
                .add(new WalletPayment(Money.of("5000.00")), fromWallet)
                .add(new CardPayment(card, CardChannel.CHIP_AND_PIN), balance.subtract(fromWallet))));

        // 5. A fully paid order is never charged again.
        show(checkout.checkout(new EftPayment()));

        System.out.println();
        System.out.print(new PaymentSummaryPrinter().format(checkout));
    }

    /** The only place payment results are turned into console output. */
    private static void show(PaymentOutcome outcome) {
        System.out.println("[" + outcome.status() + "] " + outcome.message());
    }
}




/**
 
public final class PayNestApplication {

    private PayNestApplication() { }

    public static void main(String[] args) {
        Product laptop = new Product(1, "Laptop", new BigDecimal("12000.00"), 5);
        Product mouse  = new Product(2, "Wireless Mouse", new BigDecimal("350.00"), 20);
        Product keyboard = new Product(3, "Mechanical Keyboard", new BigDecimal("1500.00"), 10);

        Customer customer = new Customer(1, "Ari Nemadodzi", "ari@example.com");

        OrderService orderService = new OrderService();
        Order order = orderService.createOrder(1, customer);

        // quantity > 1, per the demo requirement
        orderService.addToOrder(order, laptop, 1);
        orderService.addToOrder(order, mouse, 3);
        orderService.addToOrder(order, keyboard, 2);

        //deliberately over stock
        try {
            orderService.addToOrder(order, mouse, 100);
        } catch (InsufficientStockException ex) {
            System.out.println("Rejected as expected: " + ex.getMessage());
        }

        OrderSummaryPrinter printer = new OrderSummaryPrinter();
        System.out.println(printer.printSummary(order));
    }
}*/