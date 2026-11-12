package com.paynest.payment.rails;

/**
 * Optional capability for payment rails that can return funds.
 *
 * <p>Refunds are not universally available on every rail. Implementations must
 * enforce their own refund limits and return funds according to the original
 * payment instrument's rules. This educational interface uses a Rand amount.
 */
public interface Refundable {

    /**
     * Refunds a positive Rand amount against a previous successful payment.
     *
     * @param amount amount to refund in ZAR
     * @return true when the refund is accepted
     */
    boolean refund(double amount);
}
