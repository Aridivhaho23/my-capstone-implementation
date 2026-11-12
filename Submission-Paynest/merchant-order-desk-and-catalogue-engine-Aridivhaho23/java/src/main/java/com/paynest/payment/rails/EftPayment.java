package com.paynest.payment.rails;

/** Rail 2: pay by EFT (bank transfer). Simulated: the transfer always succeeds. */
public class EftPayment extends AbstractPaymentMethod {

    public EftPayment() {
        super("EFT");
    }

    @Override
    protected boolean authorise(double amount) {
        System.out.println("[EFT] Initiating bank transfer of " + RandFormat.format(amount));
        return true; // Simulated payment.
    }
}
