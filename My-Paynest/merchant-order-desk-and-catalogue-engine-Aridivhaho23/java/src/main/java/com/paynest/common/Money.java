package com.paynest.common;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * A Rand amount that is safe to calculate with.
 *
 * <p>WHY NOT double? A double stores numbers in binary, so 0.1 + 0.2 gives
 * 0.30000000000000004. For money that is a bug. BigDecimal stores exact decimal digits.
 *
 * <p>This class wraps a BigDecimal and makes three rules impossible to forget:
 * <ol>
 *   <li>The scale is ALWAYS 2 decimals (cents): R5 is stored as R5.00.</li>
 *   <li>Rounding is ALWAYS HALF_UP (1.005 becomes 1.01), in one place only.</li>
 *   <li>The object is IMMUTABLE: add/subtract/multiply return a NEW Money.</li>
 * </ol>
 *
 * <p>Tip: build Money from Strings ({@code Money.of("12400.00")}), never from a double.
 * There is deliberately no Money.of(double).
 */
public final class Money implements Comparable<Money> {

    // NOTE: these two constants must be declared BEFORE ZERO, because ZERO's constructor uses them.
    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    /** R0.00, handy as the starting value when adding things up. */
    public static final Money ZERO = new Money(BigDecimal.ZERO);

    private final BigDecimal amount;

    // Private constructor: the only way in is through the of(...) factory methods below.
    private Money(BigDecimal amount) {
        this.amount = amount.setScale(SCALE, ROUNDING);
    }

    /** Creates Money from text such as "12400" or "99.95". Throws NumberFormatException for junk like "abc". */
    public static Money of(String text) {
        Objects.requireNonNull(text, "Amount text must not be null");
        return new Money(new BigDecimal(text.trim()));
    }

    /** Creates Money from an existing BigDecimal (it is rounded to cents). */
    public static Money of(BigDecimal value) {
        Objects.requireNonNull(value, "Amount must not be null");
        return new Money(value);
    }

    /** Creates Money from whole rands, e.g. ofRands(12400) is R12400.00. */
    public static Money ofRands(long rands) {
        return new Money(BigDecimal.valueOf(rands));
    }

    public Money add(Money other) {
        return new Money(amount.add(other.amount));
    }

    public Money subtract(Money other) {
        return new Money(amount.subtract(other.amount));
    }

    /** Multiplies by a whole number (used for unit price x quantity). */
    public Money multiply(int factor) {
        return new Money(amount.multiply(BigDecimal.valueOf(factor)));
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    public boolean isPositive() {
        return amount.signum() > 0;
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    public boolean isGreaterThan(Money other) {
        return compareTo(other) > 0;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    /**
     * Compares by numeric value. We use compareTo (not BigDecimal.equals) because
     * BigDecimal.equals treats 1.0 and 1.00 as DIFFERENT. Scale is fixed here, but this is safer.
     */
    @Override
    public int compareTo(Money other) {
        return amount.compareTo(other.amount);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Money other && compareTo(other) == 0;
    }

    @Override
    public int hashCode() {
        return amount.hashCode(); // Safe: the scale is always 2, so equal values hash the same.
    }

    /** Prints as Rand with cents, e.g. "R12400.00". toPlainString avoids scientific notation. */
    @Override
    public String toString() {
        return "R" + amount.toPlainString();
    }
}
