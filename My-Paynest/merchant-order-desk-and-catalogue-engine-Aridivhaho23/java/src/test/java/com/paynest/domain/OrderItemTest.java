package com.paynest.domain;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

class OrderItemTest {

    @Test
    void calculateTotalMultipliesPriceByQuantity() {
        Product p = new Product(1, "Laptop", new BigDecimal("100.00"), 10);
        OrderItem item = new OrderItem(p, 3);
        assertEquals(0, new BigDecimal("300.00").compareTo(item.calculateTotal()));
    }
    @Test
    void testNullProductThrowsException() {
        assertThrows(IllegalArgumentException.class,
        () -> new OrderItem(null, 2),
        "Null product should throw exception");
    }
    @Test
    void testZeroQuantityThrowsException() {
        Product p = new Product(1, "Laptop", new BigDecimal("100.00"), 10);
        assertThrows(IllegalArgumentException.class,
        () -> new OrderItem(p, 0),
        "Zero quantity should throw exception");
    }
    @Test
    void testNegativeQuantityThrowsException() {
        Product p = new Product(1, "Laptop", new BigDecimal("100.00"), 10);
        assertThrows(IllegalArgumentException.class,
        () -> new OrderItem(p, -1),
        "Negative quantity should throw exception");
    }

}