package com.paynest.payment.rails;


import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Card number, expiry date and CVV, validated the moment the object is created.
 * If the data is bad, construction throws IllegalArgumentException, so an INVALID
 * CardDetails can never exist anywhere else in the program.
 *
 * <p>SECURITY BY DESIGN: this class validates the full card number and CVV, then
 * keeps ONLY what is safe to keep: the scheme, expiry and the last four digits.
 * The full number and the CVV are NOT stored in any field, so they cannot leak
 * through toString(), logs or a debugger. (CVV must never be stored at all; in
 * production a gateway would turn the card into a token and your server would
 * never see the real number.)
 */
public final class CardDetails {

    private static final DateTimeFormatter EXPIRY_FORMAT = DateTimeFormatter.ofPattern("MM/yy");

    private final CardScheme scheme;
    private final YearMonth expiry;
    private final String lastFour;

    /** Convenience constructor taking expiry as text in "MM/yy" format, e.g. "12/28". */
    public CardDetails(String cardNumber, String expiry, String cvv) {
        this(cardNumber, parseExpiry(expiry), cvv);
    }

    /** Main constructor: runs every validation rule, in a sensible order. */
    public CardDetails(String cardNumber, YearMonth expiry, String cvv) {
        if (cardNumber == null || cardNumber.isBlank()) {
            throw new IllegalArgumentException("Card number is required");
        }
        // Customers type spaces or dashes ("4111 1111 1111 1111"); strip them before checking.
        String digits = cardNumber.replaceAll("[\\s-]", "");

        if (!digits.matches("\\d+")) {
            throw new IllegalArgumentException("Card number must contain digits only");
        }
        if (digits.length() < 13 || digits.length() > 19) {
            throw new IllegalArgumentException("Card number must be 13 to 19 digits");
        }
        if (!passesLuhnCheck(digits)) {
            throw new IllegalArgumentException("Card number is not valid (failed checksum)");
        }

        CardScheme detected = CardScheme.detect(digits);
        if (!detected.acceptsLength(digits.length())) {
            throw new IllegalArgumentException(detected.getDisplayName() + " card has the wrong length");
        }

        if (expiry == null) {
            throw new IllegalArgumentException("Expiry date is required");
        }
        // A card is valid THROUGH the last day of its expiry month, so the current month is still OK.
        if (expiry.isBefore(YearMonth.now())) {
            throw new IllegalArgumentException("Card has expired");
        }

        // The CVV length depends on the scheme (Amex = 4, others = 3), which is why we detect the scheme first.
        if (cvv == null || !cvv.matches("\\d+") || cvv.length() != detected.getCvvLength()) {
            throw new IllegalArgumentException(
                    "CVV must be " + detected.getCvvLength() + " digits for " + detected.getDisplayName());
        }

        // Keep only the safe parts. The full number and CVV go out of scope here and are discarded.
        this.scheme = detected;
        this.expiry = expiry;
        this.lastFour = digits.substring(digits.length() - 4);
    }

    /** Turns "MM/yy" text into a YearMonth, with a friendly error message on bad input. */
    private static YearMonth parseExpiry(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Expiry date is required");
        }
        try {
            return YearMonth.parse(text.trim(), EXPIRY_FORMAT);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Expiry date must be in MM/yy format");
        }
    }

    /**
     * The Luhn checksum, used by real card numbers to catch typing mistakes.
     * Starting from the right, double every second digit (subtract 9 if the result is above 9),
     * add everything up, and the total must be a multiple of 10.
     */
    private static boolean passesLuhnCheck(String digits) {
        int sum = 0;
        boolean doubleIt = false; // The right-most digit is NOT doubled.
        for (int i = digits.length() - 1; i >= 0; i--) {
            int digit = digits.charAt(i) - '0';
            if (doubleIt) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            sum += digit;
            doubleIt = !doubleIt;
        }
        return sum % 10 == 0;
    }

    public CardScheme getScheme() {
        return scheme;
    }

    public YearMonth getExpiry() {
        return expiry;
    }

    /** True if the card has expired as of now (checked again at charge time, not just at creation). */
    public boolean isExpired() {
        return expiry.isBefore(YearMonth.now());
    }

    /** Safe to print: all digits hidden except the last four. */
    public String getMaskedNumber() {
        return "**** **** **** " + lastFour;
    }

    /** toString shows only the masked number, so accidentally printing this object is harmless. */
    @Override
    public String toString() {
        return scheme.getDisplayName() + " " + getMaskedNumber() + " (exp " + expiry.format(EXPIRY_FORMAT) + ")";
    }
}
