package com.paynest.payment.rails;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Payment using a stored-value voucher.
 *
 * <p>The voucher balance is held in this object for the simulation. A real
 * system would validate the voucher token against a secure voucher service,
 * check expiry/region/merchant restrictions, and atomically redeem it.
 */
public class VoucherPayment extends AbstractPaymentMethod implements Refundable {

    private final String voucherCode;
    private BigDecimal balance;
    private BigDecimal refundableBalance = BigDecimal.ZERO;

    public VoucherPayment(String voucherCode, double openingBalance) {
        super("VOUCHER");
        if (voucherCode == null || voucherCode.isBlank()) {
            throw new IllegalArgumentException("Voucher code is required");
        }
        if ((Double.isNaN(openingBalance) || Double.isInfinite(openingBalance)) || openingBalance < 0) {
            throw new IllegalArgumentException("Voucher balance must be zero or more");
        }
        this.voucherCode = voucherCode.trim();
        this.balance = BigDecimal.valueOf(openingBalance);
    }

    @Override
    protected boolean authorise(double amount) {
        BigDecimal charge = BigDecimal.valueOf(amount);
        System.out.println("[VOUCHER] Redeeming voucher ending " + maskedCode()
                + " for " + RandFormat.format(amount));

        if (charge.compareTo(balance) > 0) {
            System.out.println("[VOUCHER] Insufficient voucher balance: "
                    + RandFormat.format(balance.doubleValue()));
            return false;
        }

        balance = balance.subtract(charge);
        refundableBalance = refundableBalance.add(charge);
        System.out.println("[VOUCHER] Remaining balance: " + RandFormat.format(balance.doubleValue()));
        return true;
    }


    @Override
    public boolean refund(double amount) {
        if (!Double.isFinite(amount) || amount <= 0) {
            return false;
        }
        BigDecimal requested = BigDecimal.valueOf(amount);
        if (requested.compareTo(refundableBalance) > 0) {
            return false;
        }
        refundableBalance = refundableBalance.subtract(requested);
        balance = balance.add(requested);
        System.out.println("[VOUCHER] Refund restored to voucher: " + RandFormat.format(amount));
        return true;
    }

    public double getRefundableBalance() {
        return refundableBalance.doubleValue();
    }

    public double getBalance() {
        return balance.doubleValue();
    }

    private String maskedCode() {
        int visible = Math.min(4, voucherCode.length());
        return "*".repeat(Math.max(0, voucherCode.length() - visible))
                + voucherCode.substring(voucherCode.length() - visible);
    }
}
