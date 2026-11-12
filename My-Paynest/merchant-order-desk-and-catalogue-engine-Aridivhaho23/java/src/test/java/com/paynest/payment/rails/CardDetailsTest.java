package com.paynest.payment.rails;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.YearMonth;

import org.junit.jupiter.api.Test;

/** Validation rules for card number, expiry and CVV. */
class CardDetailsTest {

    private static final YearMonth FUTURE = YearMonth.now().plusYears(2);

    @Test
    void acceptsValidCardsAndDetectsScheme() {
        assertEquals(CardScheme.VISA, new CardDetails("4111111111111111", FUTURE, "123").getScheme());
        assertEquals(CardScheme.MASTERCARD, new CardDetails("5555555555554444", FUTURE, "123").getScheme());
        assertEquals(CardScheme.AMEX, new CardDetails("378282246310005", FUTURE, "1234").getScheme());
    }

    @Test
    void stripsSpacesAndDashesFromTheNumber() {
        assertEquals("**** **** **** 1111", new CardDetails("4111 1111-1111 1111", FUTURE, "123").getMaskedNumber());
    }

    @Test
    void rejectsBadChecksumNonDigitsAndWrongLength() {
        assertThrows(IllegalArgumentException.class, () -> new CardDetails("4111111111111112", FUTURE, "123"));
        assertThrows(IllegalArgumentException.class, () -> new CardDetails("4111abcd11111111", FUTURE, "123"));
        assertThrows(IllegalArgumentException.class, () -> new CardDetails("4111", FUTURE, "123"));
        assertThrows(IllegalArgumentException.class, () -> new CardDetails("", FUTURE, "123"));
        assertThrows(IllegalArgumentException.class, () -> new CardDetails(null, FUTURE, "123"));
    }

    @Test
    void rejectsUnsupportedCardType() {
        // Valid checksum, but a Discover number: not one of our supported schemes.
        assertThrows(IllegalArgumentException.class, () -> new CardDetails("6011111111111117", FUTURE, "123"));
    }

    @Test
    void expiryRules() {
        assertThrows(IllegalArgumentException.class,
                () -> new CardDetails("4111111111111111", YearMonth.now().minusMonths(1), "123"));
        // The current month is still valid: a card works through the end of its expiry month.
        assertFalse(new CardDetails("4111111111111111", YearMonth.now(), "123").isExpired());
        assertThrows(IllegalArgumentException.class, () -> new CardDetails("4111111111111111", (YearMonth) null, "123"));
    }

    @Test
    void parsesExpiryText() {
        assertEquals(YearMonth.of(2099, 12), new CardDetails("4111111111111111", "12/99", "123").getExpiry());
        assertThrows(IllegalArgumentException.class, () -> new CardDetails("4111111111111111", "13/99", "123"));
        assertThrows(IllegalArgumentException.class, () -> new CardDetails("4111111111111111", "1299", "123"));
        assertThrows(IllegalArgumentException.class, () -> new CardDetails("4111111111111111", "", "123"));
    }

    @Test
    void cvvLengthDependsOnScheme() {
        assertThrows(IllegalArgumentException.class, () -> new CardDetails("4111111111111111", FUTURE, "1234"));
        assertThrows(IllegalArgumentException.class, () -> new CardDetails("378282246310005", FUTURE, "123"));
        assertThrows(IllegalArgumentException.class, () -> new CardDetails("4111111111111111", FUTURE, "12a"));
        assertThrows(IllegalArgumentException.class, () -> new CardDetails("4111111111111111", FUTURE, null));
    }

    @Test
    void toStringNeverRevealsFullNumber() {
        String text = new CardDetails("4111111111111111", FUTURE, "123").toString();

        assertFalse(text.contains("4111111111111111"));
        assertFalse(text.contains("4111 1111 1111"));
        assertTrue(text.contains("**** **** **** 1111"));
    }
}
