package com.paynest.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A customer's order: its customer, its lines, and the ONE place its totals are calculated.
 *
 * <p>Invariants this class protects (no caller can bypass them):
 * <ul>
 *   <li>stock is checked and reduced in the same step a line is added;</li>
 *   <li>the line list is exposed read-only;</li>
 *   <li>once the order is {@linkplain #lock() locked} (payment has started), no line can be
 *       added, so the amount being paid can never drift away from the items.</li>
 * </ul>
 */
public class Order {

    private static final BigDecimal VAT_RATE = new BigDecimal("0.15");

    private final int id;
    private final Customer customer;
    private final List<OrderItem> items = new ArrayList<>();
    private boolean locked;

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

    /** The single path for adding a line. Validates, checks stock, reduces stock, atomically. */
    public void addItem(Product product, int quantity) {
        // Refuse changes once payment has started. Checked FIRST so stock is never touched.
        if (locked) {
            throw new IllegalStateException("Order " + id + " is locked: payment has started.");
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

    /**
     * Freezes the order contents. Called by checkout when the first payment succeeds.
     * Locking is one-way and harmless to call twice.
     */
    public void lock() {
        locked = true;
    }

    public boolean isLocked() {
        return locked;
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
     * The amount the customer actually owes: subtotal + VAT.
     *
     * <p>THE ONLY place this sum is made. The summary printer and the payment checkout both
     * call it, so what is printed and what is charged can never disagree.
     */
    public BigDecimal calculateTotalInclVat() {
        return calculateAllTotal().add(calculateVat());
    }
}
