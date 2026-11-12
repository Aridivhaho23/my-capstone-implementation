package com.paynest.payment.rails;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Simulated Bitcoin payment rail.
 *
 * <p>CheckoutService supplies the order amount in ZAR. This class converts that
 * amount using an injected quote and displays the BTC amount. It does not
 * broadcast a blockchain transaction or claim that a payment is confirmed.
 *
 * <p>Bitcoin on-chain payments are generally irreversible once confirmed.
 * Therefore this class deliberately does NOT implement Refundable. A refund
 * would be a separate outgoing Bitcoin transaction, not a reversal of the
 * original transaction.
 */
public class BitcoinPayment extends AbstractPaymentMethod {

    private final String destinationAddress;
    private final BitcoinExchangeRate exchangeRate;

    public BitcoinPayment(String destinationAddress, BitcoinExchangeRate exchangeRate) {
        super("BITCOIN");
        if (destinationAddress == null || destinationAddress.isBlank()) {
            throw new IllegalArgumentException("Bitcoin destination address is required");
        }
        this.destinationAddress = destinationAddress.trim();
        this.exchangeRate = Objects.requireNonNull(exchangeRate, "Bitcoin exchange rate provider is required");
    }

    @Override
    protected boolean authorise(double amount) {
        BigDecimal zarPerBitcoin = exchangeRate.zarPerBitcoin();
        if (zarPerBitcoin == null || zarPerBitcoin.signum() <= 0) {
            System.out.println("[BITCOIN] Payment rejected: invalid exchange-rate quote");
            return false;
        }

        BigDecimal zarAmount = BigDecimal.valueOf(amount);
        BigDecimal btcAmount = zarAmount.divide(zarPerBitcoin, 8, RoundingMode.HALF_UP);

        System.out.println("[BITCOIN] Payment request: " + btcAmount.toPlainString() + " BTC");
        System.out.println("[BITCOIN] Destination: " + maskAddress(destinationAddress));
        System.out.println("[BITCOIN] Quote: 1 BTC = " + RandFormat.format(zarPerBitcoin.doubleValue()));
        System.out.println("[BITCOIN] Simulated only: no blockchain transaction was broadcast.");
        return true;
    }

    private static String maskAddress(String address) {
        if (address.length() <= 12) {
            return address;
        }
        return address.substring(0, 6) + "..." + address.substring(address.length() - 6);
    }
}
