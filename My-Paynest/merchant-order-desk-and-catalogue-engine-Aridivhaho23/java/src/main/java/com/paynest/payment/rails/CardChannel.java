package com.paynest.payment.rails;

import com.paynest.common.Money;

/**
 * The different WAYS a card can be used. Each constant carries its own rule (an "enum with
 * constant-specific methods"), so CardPayment never needs an if/else or switch on the channel.
 *
 * <p>The set of channels is small and closed, which is exactly when an enum is the right tool.
 */
public enum CardChannel {

    /** Card inserted in the terminal; the customer enters a PIN. */
    CHIP_AND_PIN("Chip and PIN") {
        @Override
        boolean authorise(CardDetails card, Money amount) {
            return true;
        }
    },

    /** Contactless tap. Only small amounts are allowed without a PIN. */
    TAP("Contactless tap") {
        @Override
        boolean authorise(CardDetails card, Money amount) {
            return !amount.isGreaterThan(TAP_LIMIT);
        }
    },

    /** Online payment with a 3-D Secure one-time-PIN step. */
    ONLINE_3DS("Online with 3-D Secure") {
        @Override
        boolean authorise(CardDetails card, Money amount) {
            return true;
        }
    };

    /** DEMO value for the capstone, not a real banking rule. */
    static final Money TAP_LIMIT = Money.of("500.00");

    private final String displayName;

    CardChannel(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Channel-specific authorisation. Abstract, so every constant MUST provide it (the compiler
     * enforces this). Package-private: only CardPayment calls it. Simulated: no gateway call.
     */
    abstract boolean authorise(CardDetails card, Money amount);
}
