package com.paynest.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class OrderItem {
    private final Product product;
    private final int quantity;

/**
 * @param product the product being ordered
 * @param quantity the number of units ordered
 */
    // package-private: only Order constructs these
    OrderItem(Product product, int quantity) {
        if (product == null) {
            throw new IllegalArgumentException("product cannot be null.");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be > 0.");
        }
        this.product = product;
        this.quantity = quantity;
    }

    public Product getProduct() {
        return product;
    }
    public int getQuantity() {
        return quantity;
    }

    public BigDecimal calculateTotal() {
        return product.getPrice().multiply(BigDecimal.valueOf(quantity))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
