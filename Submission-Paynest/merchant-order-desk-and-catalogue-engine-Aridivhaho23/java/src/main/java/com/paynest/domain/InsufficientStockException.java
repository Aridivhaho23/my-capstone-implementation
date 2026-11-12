package com.paynest.domain;

public class InsufficientStockException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public InsufficientStockException(Product product, int requested)
    {
        super("Insufficient stock for " + product.getName()
                + " (requested " + requested + ", available " + product.getStock() + ").");
    }
}
