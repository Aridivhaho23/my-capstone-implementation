# PayNest: Merchant Order Desk, Catalogue Engine and Payment Rails

## Overview

A small Java commerce kernel for PayNest, an early-stage South African fintech.

* **Capstone 1:** products, customers, orders with line items, and a trustworthy order total
  (subtotal, 15% VAT, grand total) printed as a readable summary.
* **Capstone 3:** one checkout that pays that same order through interchangeable payment rails
  (card, EFT, wallet, split), without the order code knowing which rail is used.

Capstone 3 extends the Capstone 1 `Order`; it does not replace it. See
[CAPSTONE3_INTEGRATION.md](CAPSTONE3_INTEGRATION.md) for the design and
[CAPSTONE3_RUBRIC_ASSESSMENT.md](CAPSTONE3_RUBRIC_ASSESSMENT.md) for the rubric check.

## Project Structure

```
java/
├── pom.xml
├── README.md
├── CAPSTONE3_INTEGRATION.md
├── CAPSTONE3_RUBRIC_ASSESSMENT.md
└── src/
    ├── main/java/com/paynest/
    │   ├── app/                 PayNestApplication            (entry point + composition root)
    │   ├── common/              Money
    │   ├── domain/              Product, Customer, OrderItem, Order, InsufficientStockException
    │   ├── service/             OrderService, OrderSummaryPrinter          (Capstone 1)
    │   ├── payment/             PaymentMethod, PaymentProcessor, PaymentOutcome   (the contract)
    │   │   └── rails/           CardPayment, EftPayment, WalletPayment, SplitPayment,
    │   │                        CardDetails, CardChannel, CardScheme, EftMode      (implementations)
    │   └── checkout/            OrderCheckout, PaymentStatus, PaymentRecord,
    │                            PaymentSummaryPrinter                     (order <-> payment bridge)
    └── test/java/com/paynest/   mirrors the layout above, plus architecture/ArchitectureRulesTest
```

Dependency rule (checked by `ArchitectureRulesTest`): `app` may use everything; `checkout` uses
`domain` + `payment` + `common`; `payment.rails` uses `payment` + `common`; `domain` and `common`
use nothing else. Only `app` prints.

## Features

### Capstone 1
* Products with id, name, price and stock (validated, never negative).
* Customers with a validated email address.
* Orders with multiple lines and quantities above one; stock checked and reduced when a line is added.
* Subtotal, 15% VAT and grand total using `BigDecimal` (`HALF_UP`, rounded once per line).
* A formatted order summary that prints identically on every machine (fixed `Locale.US`).

### Capstone 3
* One `PaymentMethod` contract with Card, EFT, Wallet and Split implementations.
* Card number, expiry and CVV validated on creation (Luhn checksum, scheme-specific CVV length,
  expiry); only the last four digits are ever kept.
* Card channels (chip and PIN, tap, online 3-D Secure) and EFT modes as enums.
* Full checkout or instalments; payment status `PENDING`, `PARTIALLY_PAID`, `COMPLETED`; read-only history.
* Split payments with rollback if any part fails.
* The order locks when the first payment succeeds, so its total can never change under a part-paid order.
* Every payment attempt returns a `PaymentOutcome` (`SUCCESS`, `DECLINED`, `REJECTED`, `ERROR`)
  instead of printing.

## Technologies Used

Java 21, Maven, JUnit 5 (Jupiter), Git and GitHub.

## How to Test

```bash
cd merchant-order-desk-and-catalogue-engine-Aridivhaho23
cd java
mvn clean test
```
## How to Run

Requires Java 21 and Maven. From the `java` directory:

```bash
mvn clean compile
mvn exec:java
```

The program first runs the Capstone 1 demo (catalogue, order, over-stock rejection, summary with VAT),
then pays that same order with Capstone 3: a deposit by instant EFT, a declined wallet, a refused
late item, a split payment (wallet + card) for the balance, and a refused second charge. It ends with
a payment summary.

Covers Capstone 1 validation, totals, VAT rounding and the stock invariant; `Money`; card validation
and masking; each rail; the processor's outcomes; split rollback; checkout integration against the
real `Order` (exact amount charged, instalments, failure and edge paths, order lock, a brand-new rail
added with no edits to `Order` or `OrderCheckout`); locale-independent output; and the architecture rules.

## Business Rules

* Product price and stock cannot be negative; ids must be positive; quantities must be above zero.
* Customer email must match a basic `local@domain.tld` pattern.
* **Money:** the Capstone 1 domain uses `BigDecimal` (`HALF_UP`, two decimals); the payment layer uses
  the `Money` value object (same scale and rounding). The conversion happens in exactly one place,
  `OrderCheckout.getOrderTotal()`. Rounding happens once per line, so the grand total is always the
  exact sum of what is printed.
* **The payable amount is `Order.calculateTotalInclVat()`** (subtotal + VAT). It is the only place
  that sum is made; the printer and the checkout both call it.
* Stock cannot go negative; overselling is rejected with `InsufficientStockException`.
* An order locks on its first successful payment; adding items afterwards throws
  `IllegalStateException` before stock is touched.
* A failed payment (declined, rejected or errored) changes nothing: no history, no status change, no lock.
* A completed order is never charged again; an instalment can never exceed the balance due.
* All formatted output uses a fixed locale, so decimal separators never depend on the machine.

## Known Limitations

* Rails are simulations (no bank, card network or gateway). Never use real card data.
* Stock is reduced when an item is added to an order, and is not released if the order is abandoned.
  A reservation/cancellation flow is a future improvement.
* State is in memory only (no database).

## Object-Oriented Concepts Demonstrated

Encapsulation, composition, polymorphism (the `PaymentMethod` interface), the Composite pattern
(`SplitPayment`), enums with behaviour (`CardChannel`), immutable value objects (`Money`,
`CardDetails`, `PaymentOutcome`), custom exceptions, data validation, and separation of concerns.

## Future Improvements

* Restocking and order cancellation (release reserved stock).
* Real tokenisation through a payment gateway and idempotency keys for retries.
* Database persistence and a REST or JavaFX front end.
* Discounts and promotions.

## Author

**Name:** Aridivhaho Junior Nemadodzi

Capstone 1 and Capstone 3 · Java Programming

## License

Developed for educational purposes as part of a Java programming capstone assessment ~ Ari.
