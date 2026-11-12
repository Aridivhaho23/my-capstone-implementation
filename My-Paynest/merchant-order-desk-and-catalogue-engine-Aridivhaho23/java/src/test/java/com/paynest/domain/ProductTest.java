package com.paynest.domain;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

class ProductTest {

    @Test
    void constructorAcceptsValidProduct() {
        Product product = new Product(1, "Laptop", new BigDecimal("999.99"), 10);

        assertEquals("Laptop", product.getName());
        assertEquals(0, new BigDecimal("999.99").compareTo(product.getPrice()));
        assertEquals(10, product.getStock());
    }

    @Test
    void constructorRejectsNegativePrice() {
        assertThrows(IllegalArgumentException.class,
                () -> new Product(1, "Laptop", new BigDecimal("-5.00"), 10));
    }

    @Test
    void constructorRejectsBlankName() {
        assertThrows(IllegalArgumentException.class,
                () -> new Product(1, "   ", new BigDecimal("5.00"), 10));
    }

    @Test
    void constructorRejectsNonPositiveId() {
        assertThrows(IllegalArgumentException.class,
                () -> new Product(0, "Laptop", new BigDecimal("5.00"), 10));
    }

    @Test
    void constructorRejectsNegativeStock() {
        assertThrows(IllegalArgumentException.class,
                () -> new Product(1, "Laptop", new BigDecimal("5.00"), -1));
    }

    @Test
    void reduceStockDecreasesAvailableStock() {
        Product product = new Product(1, "Laptop", new BigDecimal("5.00"), 10);

        product.reduceStock(3);

        assertEquals(7, product.getStock());
    }

    @Test
    void reduceStockRejectsMoreThanAvailable() {
        Product product = new Product(1, "Laptop", new BigDecimal("5.00"), 2);

        assertThrows(IllegalArgumentException.class, () -> product.reduceStock(3));
    }

    @Test
    void reduceStockRejectsNonPositiveQuantity() {
        Product product = new Product(1, "Laptop", new BigDecimal("5.00"), 2);

        assertThrows(IllegalArgumentException.class, () -> product.reduceStock(0));
    }
    @Test
    void reduceStockAllowsQuantityEqualToStock() {
        Product product = new Product(1, "Laptop", new BigDecimal("5.00"), 3);
        product.reduceStock(3);
        assertEquals(0, product.getStock());
    }
}
