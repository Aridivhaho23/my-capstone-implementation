package com.paynest.domain;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class OrderTest {

    private Customer customer() {
        return new Customer(1, "Ari", "ari@example.com");
    }
    
    @Test
    void constructorRejectsNullCustomer() {
        assertThrows(IllegalArgumentException.class,() -> new Order(6, null));
    }

    @Test
    void emptyOrderHasZeroTotal() {
        Order order = new Order(1, customer());
        assertEquals(0, new BigDecimal("0.00").compareTo(order.calculateAllTotal()));
    }

    @Test
    void grandTotalEqualsSumOfLineTotals() {
        Order order = new Order(1, customer());
        Product laptop = new Product(1, "Laptop", new BigDecimal("100.00"), 10);
        Product mouse  = new Product(2, "Mouse", new BigDecimal("50.00"), 10);

        order.addItem(laptop, 2);  // 200.00
        order.addItem(mouse, 3);   // 150.00

        assertEquals(0, new BigDecimal("350.00").compareTo(order.calculateAllTotal()));
    }

    @Test
    void vatIsFifteenPercentOfSubtotal() {
        Order order = new Order(1, customer());
        order.addItem(new Product(1, "Laptop", new BigDecimal("100.00"), 10), 1);

        assertEquals(0, new BigDecimal("15.00").compareTo(order.calculateVat()));
    }

    /**
     * A true half-cent boundary: 0.30 * 0.15 = 0.045 exactly. This is the
     * one value that distinguishes HALF_UP (-> 0.05) from HALF_EVEN
     * (-> 0.04, since 4 is even). Proves the documented rounding policy is
     * actually the one in effect, not just "some" rounding.
     */
    @Test
    void vatRoundsExactHalfUpBoundary() {
        Order order = new Order(1, customer());
        order.addItem(new Product(1, "Cheap Item", new BigDecimal("0.30"), 1), 1);

        assertEquals(0, new BigDecimal("0.05").compareTo(order.calculateVat()));
    }

    /**
     * Previous VAT tests only used "clean" numbers (100.00 -> 15.00) that
     * never actually require rounding, so HALF_UP was never exercised.
     * 33.37 * 3 = 100.11 exactly (no line-level rounding needed), but
     * 100.11 * 0.15 = 15.0165, which HALF_UP rounds to 15.02.
     */
    @Test
    void vatRoundsHalfUpWhenNotAlreadyClean() {
        Order order = new Order(1, customer());
        order.addItem(new Product(1, "Widget", new BigDecimal("33.37"), 10), 3);

        assertEquals(0, new BigDecimal("15.02").compareTo(order.calculateVat()));
    }

    @Test
    void addItemRejectsNonPositiveQuantity() {
        Order order = new Order(1, customer());
        Product laptop = new Product(1, "Laptop", new BigDecimal("100.00"), 10);
        assertThrows(IllegalArgumentException.class, () -> order.addItem(laptop, 0));
    }

    @Test
    void addItemRejectsNullProduct() {
        Order order = new Order(1, customer());
        assertThrows(NullPointerException.class, () -> order.addItem(null, 1));
    }

    @Test
    void addItemReducesProductStock() {
        Order order = new Order(1, customer());
        Product laptop = new Product(1, "Laptop", new BigDecimal("100.00"), 10);
        order.addItem(laptop, 4);
        assertEquals(6, laptop.getStock());
    }

    @Test
    void constructorRejectsNonPositiveOrderId() {
        assertThrows(IllegalArgumentException.class, () -> new Order(0, customer()));
    }

    /** Boundary: quantity exactly equal to stock must succeed, leaving stock at zero. */
    @Test
    void addItemAllowsQuantityEqualToAvailableStock() {
        Order order = new Order(1, customer());
        Product laptop = new Product(1, "Laptop", new BigDecimal("100.00"), 5);

        order.addItem(laptop, 5);

        assertEquals(0, laptop.getStock());
        assertEquals(1, order.getItems().size());
    }

    @Test
    void getItemsReturnsUnmodifiableList() {
        Order order = new Order(1, customer());
        Product product = new Product(7,"Tablet",new BigDecimal("2000.00"), 5);
        order.addItem(product, 2);

        assertThrows(UnsupportedOperationException.class, () -> order.getItems().add(null));
    }

    /**
     * This is the invariant test: it calls Order.addItem directly, with no
     * OrderService involved at all, and proves the stock check still fires.
     * The rule lives in Order, not in whichever caller happens to check
     * first — this is what "extensibility without breaking callers" means
     * in practice.
     */
    @Test
    void addItemEnforcesStockEvenWithoutOrderService() {
        Order order = new Order(1, customer());
        Product laptop = new Product(1, "Laptop", new BigDecimal("19999.00"), 2);

        assertThrows(InsufficientStockException.class, () -> order.addItem(laptop, 5));
        assertEquals(2, laptop.getStock());       // failed attempt must not touch stock
        assertTrue(order.getItems().isEmpty());   // and must not add a line
    }

        /** Boundary: exactly one more than stock must still fail. */
    @Test
    void addItemRejectsQuantityOneMoreThanStock() {
        Order order = new Order(1, customer());
        Product laptop = new Product(1, "Laptop", new BigDecimal("100.00"), 5);

        assertThrows(InsufficientStockException.class, () -> order.addItem(laptop, 6));
        assertEquals(5, laptop.getStock()); // failed attempt must not touch stock
    }

    @Test
    void stockDepletesAcrossMultipleAddItemCalls() {
        Order order = new Order(1, customer());
        Product laptop = new Product(1, "Laptop", new BigDecimal("100.00"), 10);

        order.addItem(laptop, 3);
        order.addItem(laptop, 4);

        assertEquals(3, laptop.getStock());
        assertEquals(2, order.getItems().size());
        assertEquals(0, new BigDecimal("700.00").compareTo(order.calculateAllTotal()));
    }
}