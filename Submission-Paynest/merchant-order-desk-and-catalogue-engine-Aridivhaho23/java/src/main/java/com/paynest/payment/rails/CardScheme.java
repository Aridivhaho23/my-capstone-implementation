package com.paynest.payment.rails;


/**
 * The card network (Visa, Mastercard, Amex). An enum is perfect here: the list is
 * small and closed, and each constant carries its own rules (CVV length, card length).
 */
public enum CardScheme {

    VISA("Visa", 3, 13, 16, 19),
    MASTERCARD("Mastercard", 3, 16),
    AMEX("American Express", 4, 15); // Amex has a 4-digit security code.

    private final String displayName;
    private final int cvvLength;
    private final int[] validLengths;

    CardScheme(String displayName, int cvvLength, int... validLengths) {
        this.displayName = displayName;
        this.cvvLength = cvvLength;
        this.validLengths = validLengths;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getCvvLength() {
        return cvvLength;
    }

    /** True if a card number of this length is allowed for this scheme. */
    public boolean acceptsLength(int length) {
        for (int allowed : validLengths) {
            if (allowed == length) {
                return true;
            }
        }
        return false;
    }

    /**
     * Works out the scheme from the first digits of the card number (the "IIN range").
     * Simplified for the capstone; real systems use a maintained BIN table.
     *
     * @throws IllegalArgumentException if the number does not belong to a supported scheme
     */
    public static CardScheme detect(String digits) {
        if (digits.startsWith("4")) {
            return VISA;
        }
        if (digits.startsWith("34") || digits.startsWith("37")) {
            return AMEX;
        }
        if (digits.length() >= 4) {
            int firstTwo = Integer.parseInt(digits.substring(0, 2));
            int firstFour = Integer.parseInt(digits.substring(0, 4));
            if ((firstTwo >= 51 && firstTwo <= 55) || (firstFour >= 2221 && firstFour <= 2720)) {
                return MASTERCARD;
            }
        }
        throw new IllegalArgumentException("Unsupported card type");
    }
}
