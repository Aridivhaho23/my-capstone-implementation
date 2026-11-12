package com.paynest.checkout;

import com.paynest.common.Money;

/**
 * One successful payment taken against an order, kept in its payment history.
 * A record is a compact, immutable data holder.
 */
public record PaymentRecord(String paymentType, Money amount) { }
