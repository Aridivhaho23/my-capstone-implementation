package com.paynest.checkout;

/** Where an order stands on payment. A closed set, so an enum. */
public enum PaymentStatus {
    /** Nothing has been paid yet. */
    PENDING,
    /** Something has been paid, but a balance is still due. */
    PARTIALLY_PAID,
    /** Paid in full. A completed order is never charged again. */
    COMPLETED
}
