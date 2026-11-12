package com.paynest.payment.rails;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Objects;

/**
 * Rail 1: pay by bank card.
 *
 * <p>The card is a validated {@link CardDetails} (number, expiry and CVV are checked when it is
 * created), so a CardPayment can only ever be built with a card that passed every rule.
 */
public class CardPayment extends AbstractPaymentMethod implements Refundable {

    private final CardDetails card;
    // Tracks successful charges not yet refunded (simulation ledger).
    private BigDecimal refundableBalance = BigDecimal.ZERO;

    /** Pays with the given validated card. */
    public CardPayment(CardDetails card) {
        super("CARD");
        this.card = Objects.requireNonNull(card, "Card details are required");
    }

    /**
     * Demo shortcut so the starter shape {@code new CardPayment()} works. It uses a built-in TEST
     * card number (valid checksum, no real account).
     */
    public CardPayment() {
        this(new CardDetails("4111 1111 1111 1111", YearMonth.now().plusYears(2), "123"));
    }

    @Override
    protected boolean authorise(double amount) {
        // The card may have been valid when created but expired since (e.g. a saved card).
        if (card.isExpired()) {
            System.out.println("[CARD] Declined: card has expired");
            return false;
        }
        // Rail-specific message. Only the masked number is shown, never the full number.
        System.out.println("[CARD] Authorising card payment of " + RandFormat.format(amount) + " on " + card);
        refundableBalance = refundableBalance.add(BigDecimal.valueOf(amount));
        return true; // Simulated payment: there is no real gateway.
    }

    /**
     * Simulates a refund to the original card. The refund cannot exceed the
     * total amount successfully charged and not already refunded.
     */
    @Override
    public boolean refund(double amount) {
        if ((Double.isNaN(amount) || Double.isInfinite(amount)) || amount <= 0) {
            System.out.println("[CARD] Refund rejected: amount must be positive and finite");
            return false;
        }

        BigDecimal requested = BigDecimal.valueOf(amount);
        if (requested.compareTo(refundableBalance) > 0) {
            System.out.println("[CARD] Refund rejected: amount exceeds refundable card payments");
            return false;
        }

        refundableBalance = refundableBalance.subtract(requested);
        System.out.println("[CARD] Refund accepted to original card: " + RandFormat.format(amount));
        return true;
    }

    /** Amount still eligible for refund in this simulation. */
    public double getRefundableBalance() {
        return refundableBalance.doubleValue();
    }
}
