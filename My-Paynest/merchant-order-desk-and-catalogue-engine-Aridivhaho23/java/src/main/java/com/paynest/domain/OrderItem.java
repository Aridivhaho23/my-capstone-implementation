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
    //Doesn't comply with SOLID principles because it violates the Single Responsibility Principle (SRP). 
    // The OrderItem class is responsible for representing an item in an order,
    // but it also contains logic for calculating the total price of the item.
    // This calculation could be considered a separate responsibility that should
    // be handled by another class or method, such as an Order class or a PriceCalculator class.
    // By including this logic in the OrderItem class, it becomes more difficult to maintain and
    // test the code, and it may lead to code duplication if other classes need to perform similar
    // calculations.
    public BigDecimal calculateTotal() {
        return product.getPrice().multiply(BigDecimal.valueOf(quantity))
                .setScale(2, RoundingMode.HALF_UP);
    }
}

