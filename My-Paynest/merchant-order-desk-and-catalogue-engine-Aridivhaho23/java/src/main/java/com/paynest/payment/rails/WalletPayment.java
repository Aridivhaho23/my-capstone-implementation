package com.paynest.payment.rails;

import com.paynest.common.Money;
import com.paynest.payment.PaymentMethod;

import java.util.Objects;

/**
 * Rail 3: pay from an in-app wallet. A wallet holds a balance, so it can genuinely FAIL
 * (insufficient funds) and it must give money back when a payment is reversed.
 */
public class WalletPayment implements PaymentMethod {

    /** Balance used by the no-argument constructor (simulation convenience). */
    private static final Money DEFAULT_BALANCE = Money.of("100000.00");

    private Money balance;

    public WalletPayment() {
        this(DEFAULT_BALANCE);
    }

    public WalletPayment(Money openingBalance) {
        this.balance = Objects.requireNonNull(openingBalance, "Opening balance is required");
    }

    @Override
    public boolean processPayment(Money amount) {
        if (amount.isGreaterThan(balance)) {
            return false; // Insufficient funds: decline without touching the balance.
        }
        // Debit only AFTER the check passes, so a failed payment costs nothing.
        balance = balance.subtract(amount);
        return true;
    }

    @Override
    public String getPaymentType() {
        return "WALLET";
    }

    /** Puts the money back, used when a split payment has to roll back. */
    @Override
    public void reverse(Money amount) {
        balance = balance.add(amount);
    }

    public Money getBalance() {
        return balance;
    }
}
