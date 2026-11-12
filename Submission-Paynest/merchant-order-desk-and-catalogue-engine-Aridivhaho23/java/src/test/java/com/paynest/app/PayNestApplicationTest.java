package com.paynest.app;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.Test;

class PayNestApplicationTest {
    @Test
    void applicationDemonstratesEveryPaymentMethod() {
        PrintStream original = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        System.setOut(new PrintStream(captured));
        try {
            PayNestApplication.demonstrateAllPaymentMethods();
        } finally {
            System.setOut(original);
        }
        String output = captured.toString();
        for (String rail : new String[] { "CARD", "EFT", "WALLET", "VOUCHER", "BITCOIN",
                "BUY_NOW_PAY_LATER", "SPLIT" }) {
            assertTrue(output.contains("--- Testing " + rail + " ---"), rail + " should be demonstrated");
            assertTrue(output.contains("completed via " + rail), rail + " should complete its test order");
        }
    }
}
