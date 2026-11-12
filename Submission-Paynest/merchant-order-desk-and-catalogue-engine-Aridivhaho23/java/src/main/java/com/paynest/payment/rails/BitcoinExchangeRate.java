package com.paynest.payment.rails;

import java.math.BigDecimal;

/**
 * Supplies the quoted ZAR value of one Bitcoin (BTC).
 *
 * <p>Production implementations should obtain a time-limited quote from a
 * trusted exchange/payment provider. The project injects this dependency so
 * the Bitcoin rail does not invent or hard-code a live exchange rate.
 */
@FunctionalInterface
public interface BitcoinExchangeRate {

    /** @return positive number of South African Rand for 1 BTC */
    BigDecimal zarPerBitcoin();
}
