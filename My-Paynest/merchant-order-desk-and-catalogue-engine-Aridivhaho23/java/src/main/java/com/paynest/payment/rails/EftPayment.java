package com.paynest.payment.rails;

import com.paynest.common.Money;
import com.paynest.payment.PaymentMethod;

import java.util.Objects;

/** Rail 2: pay by EFT (bank transfer), either standard or instant. Simulated: always succeeds. */
public class EftPayment implements PaymentMethod {

    private final EftMode mode;

    /** Default: a standard EFT. */
    public EftPayment() {
        this(EftMode.STANDARD);
    }

    public EftPayment(EftMode mode) {
        this.mode = Objects.requireNonNull(mode, "EFT mode is required");
    }

    @Override
    public boolean processPayment(Money amount) {
        return true; // Simulated: the transfer always succeeds in this capstone.
    }

    @Override
    public String getPaymentType() {
        return "EFT";
    }

    public EftMode getMode() {
        return mode;
    }

    @Override
    public void reverse(Money amount) {
        // Simulated reversal: nothing held locally.
    }
}
