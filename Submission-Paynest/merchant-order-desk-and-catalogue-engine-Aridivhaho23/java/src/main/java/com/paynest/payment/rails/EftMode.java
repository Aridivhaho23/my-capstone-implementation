package com.paynest.payment.rails;


/** The two ways an EFT can run. Closed set, so an enum. */
public enum EftMode {
    /** Normal bank transfer, confirmed later. */
    STANDARD("Standard EFT"),
    /** Confirmed immediately. */
    INSTANT("Instant EFT");

    private final String displayName;

    EftMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
