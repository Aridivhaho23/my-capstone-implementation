package com.paynest.architecture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * Executable architecture rules. They read the production source files and fail if a layer
 * starts depending on something it must not know about. This is what keeps the SOLID design
 * from rotting: the Dependency Inversion and Open/Closed claims are CHECKED, not just written down.
 *
 * <pre>
 *   app  ->  checkout, service, domain, payment, payment.rails, common     (composition root)
 *   checkout  ->  domain, payment, common
 *   service   ->  domain
 *   payment (contract + processor)  ->  common
 *   payment.rails (implementations) ->  payment, common
 *   domain  ->  (nothing)           common -> (nothing)
 * </pre>
 *
 * Maven runs tests from the project directory, so the relative path below resolves.
 */
class ArchitectureRulesTest {

    private static final Path SRC = Path.of("src/main/java/com/paynest");
    private static final String BASE = "com.paynest.";

    /** Java files directly in one package folder (subfolders are separate packages). */
    private static List<Path> filesIn(String packageDir) throws IOException {
        try (Stream<Path> stream = Files.list(SRC.resolve(packageDir))) {
            return stream.filter(p -> p.toString().endsWith(".java")).sorted().toList();
        }
    }

    /** Returns one message per forbidden dependency found in the given package. */
    private static List<String> violations(String packageDir, String... forbiddenPackages) throws IOException {
        List<String> found = new ArrayList<>();
        for (Path file : filesIn(packageDir)) {
            for (String line : Files.readAllLines(file)) {
                if (line.startsWith("package ")) {
                    continue;
                }
                for (String forbidden : forbiddenPackages) {
                    if (line.contains(BASE + forbidden)) {
                        found.add(file.getFileName() + " depends on " + BASE + forbidden);
                    }
                }
            }
        }
        return found;
    }

    @Test
    void sourceTreeIsWhereTheRulesExpectIt() throws IOException {
        assertTrue(Files.isDirectory(SRC), "Run tests from the project directory (mvn test)");
        assertTrue(!filesIn("domain").isEmpty() && !filesIn("payment").isEmpty() && !filesIn("checkout").isEmpty());
    }

    @Test
    void domainAndCommonDependOnNothingElseInTheProject() throws IOException {
        assertEquals(List.of(), violations("domain",
                "service", "checkout", "payment", "app"));
        // common may only mention itself.
        assertEquals(List.of(), violations("common",
                "domain", "service", "checkout", "payment", "app"));
    }

    @Test
    void theCheckoutNeverDependsOnConcretePaymentRails() throws IOException {
        // Dependency Inversion + Open/Closed: checkout knows the PaymentMethod interface only,
        // so a new rail never forces an edit here.
        assertEquals(List.of(), violations("checkout", "payment.rails", "app", "service"));
    }

    @Test
    void thePaymentContractAndProcessorKnowNeitherRailsNorOrders() throws IOException {
        assertEquals(List.of(), violations("payment",
                "payment.rails", "domain", "service", "checkout", "app"));
    }

    @Test
    void railsOnlyDependOnTheContractAndMoney() throws IOException {
        assertEquals(List.of(), violations("payment/rails",
                "domain", "service", "checkout", "app"));
    }

    @Test
    void theCapstoneOneServiceLayerStaysFreeOfPayments() throws IOException {
        assertEquals(List.of(), violations("service", "payment", "checkout", "app"));
    }

    @Test
    void onlyTheAppLayerEverPrints() throws IOException {
        List<String> found = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(SRC)) {
            for (Path file : stream.filter(p -> p.toString().endsWith(".java")).toList()) {
                if (SRC.relativize(file).startsWith("app")) {
                    continue;
                }
                for (String line : Files.readAllLines(file)) {
                    if (line.contains("System.out") || line.contains("System.err")
                            || line.contains("printStackTrace")) {
                        found.add(file.getFileName() + " writes to the console: " + line.trim());
                    }
                }
            }
        }
        assertEquals(List.of(), found);
    }
}
