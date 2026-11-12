package com.paynest.payment;

import com.paynest.common.Money;

import java.util.Objects;

/**
 * The result of ONE payment attempt, returned as data instead of being printed.
 *
 * <p>Why a value instead of console output: the payment and checkout layers must stay
 * free of I/O (only the app layer prints, the same rule the Capstone 1 code follows).
 * Returning an outcome also lets callers decide what to do (show a message, log, retry)
 * without parsing text.
 *
 * @param status      what happened (see {@link Status})
 * @param paymentType the rail label, e.g. "CARD", or "NONE" if no rail was chosen
 * @param amount      the amount that was requested (may be null if none was supplied)
 * @param message     a short human-readable explanation, safe to show to a customer
 */
public record PaymentOutcome(Status status, String paymentType, Money amount, String message) {

    /** The four things that can happen to a payment attempt. A closed set, so an enum. */
    public enum Status {
        /** The rail accepted the payment. */
        SUCCESS,
        /** The rail ran and said no (declined, insufficient funds, over a limit...). */
        DECLINED,
        /** The request was invalid, so no rail was charged (no method, bad amount, already paid...). */
        REJECTED,
        /** The rail threw an unexpected error. Treated as a failure, never as a success. */
        ERROR
    }

    public PaymentOutcome {
        Objects.requireNonNull(status, "status is required");
        Objects.requireNonNull(paymentType, "paymentType is required");
        Objects.requireNonNull(message, "message is required");
    }

    public static PaymentOutcome success(String paymentType, Money amount) {
        return new PaymentOutcome(Status.SUCCESS, paymentType, amount, "Payment successful via " + paymentType);
    }

    public static PaymentOutcome declined(String paymentType, Money amount) {
        return new PaymentOutcome(Status.DECLINED, paymentType, amount, "Payment declined by " + paymentType);
    }

    public static PaymentOutcome rejected(String paymentType, Money amount, String reason) {
        return new PaymentOutcome(Status.REJECTED, paymentType, amount, reason);
    }

    public static PaymentOutcome error(String paymentType, Money amount, String reason) {
        return new PaymentOutcome(Status.ERROR, paymentType, amount, "Payment failed via " + paymentType + ": " + reason);
    }

    /** True only when the money was actually taken. */
    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }
}
