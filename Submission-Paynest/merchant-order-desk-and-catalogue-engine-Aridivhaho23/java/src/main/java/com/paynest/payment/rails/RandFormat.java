package com.paynest.payment.rails;

import java.util.Locale;

/**
 * Prints a Rand amount the same way everywhere, e.g. 18457.5 becomes "R18457.50".
 *
 * <p>Locale.US is used on purpose: a South African locale prints a decimal COMMA
 * ("18457,50"), so the same program would print differently on different computers.
 * Two decimals are used so cents are never hidden.
 */
public final class RandFormat {

    // Utility class: only static methods, so nobody should create one.
    private RandFormat() {
    }

    public static String format(double amount) {
        return "R" + String.format(Locale.US, "%.2f", amount);
    }
}
