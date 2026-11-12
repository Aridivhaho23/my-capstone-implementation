package com.paynest.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Tests for the BigDecimal-backed Money value object. */
class MoneyTest {

    @Test
    void doubleArithmeticIsInexactButMoneyIsExact() {
        // The reason Money exists: in double, 0.1 + 0.2 is NOT 0.3.
        assertTrue(0.1 + 0.2 != 0.3);
        assertEquals(Money.of("0.30"), Money.of("0.10").add(Money.of("0.20")));
    }

    @Test
    void scaleIsAlwaysTwoDecimals() {
        assertEquals("R5.00", Money.of("5").toString());
        assertEquals("R12400.00", Money.ofRands(12400).toString());
    }

    @Test
    void roundsHalfUpToCents() {
        assertEquals("R1.01", Money.of("1.005").toString());
        assertEquals("R1.00", Money.of("1.004").toString());
    }

    @Test
    void multiplyAddAndSubtractWork() {
        assertEquals(Money.of("0.30"), Money.of("0.10").multiply(3));
        assertEquals(Money.of("7.50"), Money.of("10.00").subtract(Money.of("2.50")));
    }

    @Test
    void comparisonsUseNumericValue() {
        assertTrue(Money.of("10.00").isGreaterThan(Money.of("9.99")));
        assertFalse(Money.of("10.00").isGreaterThan(Money.of("10")));
        assertEquals(Money.of("10"), Money.of("10.00"));
        assertTrue(Money.ZERO.isZero());
        assertTrue(Money.of("-1").isNegative());
        assertTrue(Money.of("1").isPositive());
    }

    @Test
    void rejectsNonNumericText() {
        assertThrows(NumberFormatException.class, () -> Money.of("abc"));
    }
}
