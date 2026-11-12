package com.paynest.service;

import com.paynest.domain.Customer;
import com.paynest.domain.Order;
import com.paynest.domain.Product;

/** Coordinates order creation and item management; checkout is handled separately. */
public class OrderService {

    public Order createOrder(int id, Customer customer) {
        return new Order(id, customer);
    }

    public void addToOrder(Order order, Product product, int quantity) {
        order.addItem(product, quantity);
    }
}
