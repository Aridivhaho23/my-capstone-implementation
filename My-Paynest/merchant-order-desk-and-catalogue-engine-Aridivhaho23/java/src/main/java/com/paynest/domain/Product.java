package com.paynest.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Something the merchant can sell. Always valid once constructed. */
public class Product {

    private final int id;
    private final String name;
    private final BigDecimal price;
    private int stock;
 /**
     * Creates a new product.
     *
     * @param id    unique identifier for the product
     * @param name  display name of the product
     * @param price price in the local currency (e.g. Rands)
     * @param stock initial stock quantity
     */

    public Product(int id, String name, BigDecimal price, int stock)
    {
        if (id <= 0) {
            throw new IllegalArgumentException("id must be positive.");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name cannot be blank.");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("price cannot be negative.");
        }
        if (stock < 0) {
            throw new IllegalArgumentException("stock cannot be negative.");
        }

        this.id = id;
        this.name = name.trim();
        this.price = price.setScale(2, RoundingMode.HALF_UP); // rounded once, up front
        this.stock = stock;
    }

    public int getId() {
        return id;
    }
    public String getName() {
         return name;
    }
    public BigDecimal getPrice() {
         return price;
    }
    public int getStock() {
        return stock;
    }

    //
    // package-private: only Order should call this
    void reduceStock(int quantity) {
        if (quantity <= 0 || quantity > stock)
            throw new IllegalArgumentException("Invalid stock reduction for " + name + ".");
        stock -= quantity;
    }
}


