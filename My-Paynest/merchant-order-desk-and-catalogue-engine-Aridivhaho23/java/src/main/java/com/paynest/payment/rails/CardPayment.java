package com.paynest.payment.rails;

import com.paynest.common.Money;
import com.paynest.payment.PaymentMethod;

import java.util.Objects;

/**
 * Rail 1: pay by bank card.
 *
 * <p>One class covers every way of using a card. It holds the validated {@link CardDetails}
 * and the {@link CardChannel} (chip, tap, online) and hands the channel-specific rule to the
 * channel enum. The rail label stays "CARD" for all channels so receipts stay stable.
 */
public class CardPayment implements PaymentMethod {

    private final CardDetails card;
    private final CardChannel channel;

    /** Pay with a specific card and channel. */
    public CardPayment(CardDetails card, CardChannel channel) {
        this.card = Objects.requireNonNull(card, "Card details are required");
        this.channel = Objects.requireNonNull(channel, "Card channel is required");
    }

    /** Shortcut: an online (3-D Secure) card payment, the most common case. */
    public CardPayment(CardDetails card) {
        this(card, CardChannel.ONLINE_3DS);
    }

    @Override
    public boolean processPayment(Money amount) {
        // The card may have been valid when created but expired since (e.g. a saved card).
        if (card.isExpired()) {
            return false;
        }
        // Channel-specific rules. No switch needed: the enum constant knows what to do.
        return channel.authorise(card, amount);
    }

    @Override
    public String getPaymentType() {
        return "CARD";
    }

    @Override
    public void reverse(Money amount) {
        // Simulated refund: a real gateway would be called here. Nothing to undo locally.
    }
}
