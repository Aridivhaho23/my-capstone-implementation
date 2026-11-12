package com.paynest.service;

import com.paynest.domain.Customer;
import com.paynest.domain.Order;
import com.paynest.domain.Product;

public class OrderService {
    public Order createOrder(int id, Customer customer) {
        return new Order(id, customer);
    }
    // no duplicated stock check — Order already owns it
    public void addToOrder(Order order, Product product, int quantity) {
        order.addItem(product, quantity);
    }
}