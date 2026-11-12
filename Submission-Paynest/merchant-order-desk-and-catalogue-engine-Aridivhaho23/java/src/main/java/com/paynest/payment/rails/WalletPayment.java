package com.paynest.payment.rails;

import java.math.BigDecimal;

/**
 * Rail 3: pay from an in-app wallet. A wallet holds a balance, so unlike card and EFT it can
 * genuinely FAIL (insufficient funds).
 */
public class WalletPayment extends AbstractPaymentMethod implements Refundable {

    /** Balance used by the no-argument constructor (simulation convenience, big enough for the demo). */
    private static final double DEFAULT_BALANCE = 100_000.0;

    private BigDecimal balance;
    // Tracks wallet debits that have not yet been refunded.
    private BigDecimal refundableBalance = BigDecimal.ZERO;

    public WalletPayment() {
        this(DEFAULT_BALANCE);
    }

    /** @param openingBalance the money in the wallet, in Rand (must not be negative) */
    public WalletPayment(double openingBalance) {
        super("WALLET");
        if (Double.isNaN(openingBalance) || Double.isInfinite(openingBalance) || openingBalance < 0) {
            throw new IllegalArgumentException("Opening balance must be zero or more");
        }
        this.balance = BigDecimal.valueOf(openingBalance);
    }

    @Override
    protected boolean authorise(double amount) {
        System.out.println("[WALLET] Checking wallet balance for " + RandFormat.format(amount));

        // Rail-specific rule: you cannot spend more than the wallet holds.
        if (BigDecimal.valueOf(amount).compareTo(balance) > 0) {
            System.out.println("[WALLET] Insufficient funds. Balance is " + RandFormat.format(balance.doubleValue()));
            return false;
        }

        // Debit only AFTER the check passes, so a failed payment costs nothing.
        balance = balance.subtract(BigDecimal.valueOf(amount));
        refundableBalance = refundableBalance.add(BigDecimal.valueOf(amount));
        System.out.println("[WALLET] Debited wallet. Remaining balance " + RandFormat.format(balance.doubleValue()));
        return true;
    }

    /**
     * Returns funds to the same wallet, but only up to successful payments
     * made through this wallet that have not already been refunded.
     */
    @Override
    public boolean refund(double amount) {
        if ((Double.isNaN(amount) || Double.isInfinite(amount)) || amount <= 0) {
            System.out.println("[WALLET] Refund rejected: amount must be positive and finite");
            return false;
        }

        BigDecimal requested = BigDecimal.valueOf(amount);
        if (requested.compareTo(refundableBalance) > 0) {
            System.out.println("[WALLET] Refund rejected: amount exceeds refundable wallet payments");
            return false;
        }

        refundableBalance = refundableBalance.subtract(requested);
        balance = balance.add(requested);
        System.out.println("[WALLET] Refund credited to wallet: " + RandFormat.format(amount));
        return true;
    }

    public double getBalance() {
        return balance.doubleValue();
    }

    public double getRefundableBalance() {
        return refundableBalance.doubleValue();
    }
}
