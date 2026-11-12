package com.paynest.payment.rails;

import java.math.BigDecimal;

/**
 * Simulated deferred payment. An approved purchase increases the customer's
 * outstanding BNPL balance; settlement happens outside the checkout flow.
 */
public class BuyNowPayLaterPayment extends AbstractPaymentMethod {
    private final BigDecimal creditLimit;
    private BigDecimal outstandingBalance = BigDecimal.ZERO;

    public BuyNowPayLaterPayment(double creditLimit) {
        super("BUY_NOW_PAY_LATER");
        if (Double.isNaN(creditLimit) || Double.isInfinite(creditLimit) || creditLimit < 0) {
            throw new IllegalArgumentException("Credit limit must be zero or more");
        }
        this.creditLimit = BigDecimal.valueOf(creditLimit);
    }

    public BuyNowPayLaterPayment() {
        this(50_000.00);
    }

    @Override
    protected boolean authorise(double amount) {
        BigDecimal charge = BigDecimal.valueOf(amount);
        if (outstandingBalance.add(charge).compareTo(creditLimit) > 0) {
            System.out.println("[BUY NOW PAY LATER] Declined: credit limit exceeded");
            return false;
        }
        outstandingBalance = outstandingBalance.add(charge);
        System.out.println("[BUY NOW PAY LATER] Purchase approved; amount due: "
                + RandFormat.format(outstandingBalance.doubleValue()));
        return true;
    }

    public double getOutstandingBalance() {
        return outstandingBalance.doubleValue();
    }

    /** Records repayment of an amount previously deferred. */
    public boolean recordRepayment(double amount) {
        if (Double.isNaN(amount) || Double.isInfinite(amount) || amount <= 0) {
            return false;
        }
        BigDecimal repayment = BigDecimal.valueOf(amount);
        if (repayment.compareTo(outstandingBalance) > 0) {
            return false;
        }
        outstandingBalance = outstandingBalance.subtract(repayment);
        return true;
    }
}
