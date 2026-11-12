package com.paynest.checkout;

/** Lifecycle state of an order's payment. */
public enum PaymentStatus {
    UNPAID,
    PAYMENT_FAILED,
    PAID
}
