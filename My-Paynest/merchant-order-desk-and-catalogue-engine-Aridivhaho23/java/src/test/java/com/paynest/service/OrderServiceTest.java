package com.paynest.service;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import com.paynest.domain.Customer;
import com.paynest.domain.InsufficientStockException;
import com.paynest.domain.Order;
import com.paynest.domain.Product;

class OrderServiceTest {

    @Test
    void addToOrderDelegatesToOrder() {
        OrderService service = new OrderService();
        Order order = service.createOrder(1, new Customer(1, "Thandiwe", "t@example.com"));
        Product laptop = new Product(1, "Laptop", new BigDecimal("100.00"), 5);

        service.addToOrder(order, laptop, 2);

        assertEquals(1, order.getItems().size());
        assertEquals(3, laptop.getStock());
    }
    @Test
    void addToOrderPropagatesInsufficientStockException() {
        OrderService service = new OrderService();
        Order order = service.createOrder(1, new Customer(1, "Thandiwe", "t@example.com"));
        Product laptop = new Product(1, "Laptop", new BigDecimal("100.00"), 2);

        assertThrows(InsufficientStockException.class,
                () -> service.addToOrder(order, laptop, 5));

        assertEquals(2, laptop.getStock());
        assertEquals(0, order.getItems().size());
    }
}
