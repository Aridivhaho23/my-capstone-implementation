package com.paynest.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.paynest.checkout.PaymentStatus;


/**
 * A customer's order: its customer, its lines, its totals, and its payment state.
 *
 * <p>Payment orchestration belongs to {@code CheckoutService}; this entity only owns
 * order data, calculations, and valid payment-state transitions.
 */
public class Order {

    private static final BigDecimal VAT_RATE = new BigDecimal("0.15");

    private final int id;
    private final Customer customer;
    private final List<OrderItem> items = new ArrayList<>();
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    /**
     * @param id       the unique identifier for this order
     * @param customer the person placing the order
     */
    public Order(int id, Customer customer) {
        if (customer == null) {
            throw new IllegalArgumentException("Customer cannot be null");
        }
        if (id <= 0) {
            throw new IllegalArgumentException("id must be positive.");
        }
        this.id = id;
        this.customer = customer;
    }

    public int getId() {
        return id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public boolean isPaid() {
        return paymentStatus == PaymentStatus.PAID;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    /** The single path for adding a line. Validates, checks stock, reduces stock, atomically. */
    public void addItem(Product product, int quantity) {
        // A paid order must not change. Checked FIRST so stock is never touched.
        if (isPaid()) {
            throw new IllegalStateException("Order " + id + " is already paid and cannot be changed.");
        }
        Objects.requireNonNull(product, "product cannot be null.");

        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be > 0.");
        }
        if (quantity > product.getStock()) {
            throw new InsufficientStockException(product, quantity);
        }
        // only Order can do this: reduceStock is package-private
        product.reduceStock(quantity);
        items.add(new OrderItem(product, quantity));
    }

    /** Subtotal: the sum of all line totals, excluding VAT. */
    public BigDecimal calculateAllTotal() {
        return items.stream()
                .map(OrderItem::calculateTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /** VAT at 15% of the subtotal, rounded HALF_UP to cents. */
    public BigDecimal calculateVat() {
        return calculateAllTotal().multiply(VAT_RATE).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * The grand total the customer owes: subtotal + VAT.
     *
     * <p>THE ONLY place this sum is made. The printed summary and the checkout both call it, so the
     * amount charged is always the amount shown.
     */
    public BigDecimal calculateTotalInclVat() {
        return calculateAllTotal().add(calculateVat());
    }

    /** Records a successful payment after CheckoutService confirms the transaction. */
    public void markPaid() {
        if (isPaid()) {
            throw new IllegalStateException("Order " + id + " is already paid.");
        }
        paymentStatus = PaymentStatus.PAID;
    }

    /** Records an unsuccessful payment attempt without changing an already-paid order. */
    public void markPaymentFailed() {
        if (!isPaid()) {
            paymentStatus = PaymentStatus.PAYMENT_FAILED;
        }
    }
}