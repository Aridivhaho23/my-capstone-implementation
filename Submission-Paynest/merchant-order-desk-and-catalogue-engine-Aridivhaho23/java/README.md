# PayNest: Merchant Order Desk, Catalogue Engine and Payment Rails

## Overview

A small Java kernel for PayNest, an early-stage South African fintech.

* **Capstone 1:** products, customers, orders with line items, and a trustworthy order total
  (subtotal, 15% VAT, grand total) printed as a readable summary.
* **Capstone 3:** one checkout that pays that same order through interchangeable payment types
  (card, EFT, wallet, voucher, Bitcoin) without the order code knowing which one is used.

See [CAPSTONE3_INTEGRATION.md](CAPSTONE3_INTEGRATION.md) for the design and
[CAPSTONE3_RUBRIC_ASSESSMENT.md](CAPSTONE3_RUBRIC_ASSESSMENT.md) for the rubric check.

## Project Structure

```
java/
├── pom.xml
└── src/
    ├── main/java/com/paynest/
    │   ├── app/        PayNestApplication
    │   ├── domain/     Product, Customer, OrderItem, Order, InsufficientStockException
    │   ├── service/    OrderService, OrderSummaryPrinter
    │   └── payment/    PaymentMethod (interface), AbstractPaymentMethod (abstract class),
    │                   CardPayment, EftPayment, WalletPayment, VoucherPayment, BitcoinPayment,
    │                   Refundable, BitcoinExchangeRate, PaymentProcessing (interface), PaymentProcessor,
    │                   CardDetails, CardScheme, RandFormat
    └── test/java/com/paynest/   mirrors the layout above
```

## Features

### Capstone 1
* Products with id, name, price and stock (validated, never negative).
* Customers with a validated email address.
* Orders with multiple lines; stock is checked and reduced when a line is added.
* Subtotal, 15% VAT and grand total using `BigDecimal` (`HALF_UP`, two decimals).
* A formatted order summary that prints the same on every machine (fixed `Locale.US`).

### Capstone 3
* `PaymentMethod` interface: `boolean processPayment(double amount)` and `String getPaymentType()`.
* `AbstractPaymentMethod` abstract class: holds the label and the shared amount check once; each
  rail only writes its own message and rule.
* Seven payment options: `CardPayment`, `EftPayment`, `WalletPayment`, `VoucherPayment`, `BitcoinPayment`,
  `BuyNowPayLaterPayment`, and `SplitPayment` (which composes multiple instruments).
  Each implements the same `PaymentMethod` contract; most extend `AbstractPaymentMethod`, while
  `SplitPayment` implements the interface directly as a composite.
* Card validation: number (digits, 13 to 19 long, Luhn checksum, supported network), expiry (not in
  the past), and CVV (3 digits, or 4 for American Express). Only the last four digits are kept.
* `CheckoutService` and `PaymentProcessor`: one checkout use case that accepts any `PaymentMethod`; `Order` remains focused on order state and totals.
* Voucher balance is checked before redemption; Bitcoin converts the Rand order total using an
  injected ZAR-per-BTC quote provider (no fake live rate is hard-coded).
* Optional `Refundable` capability: card, wallet and voucher implement it. Refunds are limited
  to successful, not-yet-refunded charges. Bitcoin, EFT and BNPL do not claim direct refund support.
* `CheckoutService` receives the `PaymentProcessing` abstraction through its constructor. `Order` has no dependency on payment infrastructure; `OrderService` handles order creation and item management only.
* Split payment accepts only refundable rails and compensates earlier successful allocations in
  reverse order if a later allocation declines.
* Safe failure paths: wallet/voucher with too little balance, invalid amount, missing payment
  method, a rail that throws, an empty order, paying twice, split allocation mismatches, BNPL credit
  limits, and refund limit violations.
* `PaymentStatus` is an enum (`UNPAID`, `PAYMENT_FAILED`, `PAID`) exposed by `Order.getPaymentStatus()`;
  `isPaid()` remains as a convenient compatibility method.
* `PayNestApplication` demonstrates every payment option against its own fresh order.

## How to Test

```bash
cd Submission-Paynest
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

The program builds the Capstone 1 order, prints its summary, pays it by card (printing the rail,
the amount and the order completion), and finally shows an invalid card being rejected.

To demonstrate polymorphism, change only the selected `PaymentMethod` assignment in
`PayNestApplication.payForOrder`. Examples are included as comments for EFT, wallet, voucher and
Bitcoin. Bitcoin requires an injected quote provider; the commented fixed quote is only a demo
value, not a live market rate.

### Refund design and real-world behaviour

`Refundable` is a separate capability, not part of `PaymentMethod`. Card, wallet and voucher payments
implement it. The simulation tracks successful charges and prevents refunds above the remaining
refundable amount. In a real card system, the processor/acquirer must perform the refund against
the original transaction; the application must retain a transaction reference and follow scheme
and provider rules. A wallet refund is a credit back to that same wallet, subject to the wallet
ledger and applicable terms. Voucher refunds restore the redeemed value to the same simulated
voucher balance.

EFT returns are generally handled through bank/payment-provider processes and are not guaranteed
by a generic application-level reversal method. Voucher refunds depend on voucher terms and
applicable consumer law; this sample models voucher refunds by restoring the same voucher balance; real handling
depends on voucher terms and applicable consumer law. Bitcoin on-chain transfers are generally irreversible once confirmed. A Bitcoin
"refund" is a separate outgoing transaction, not a reversal, so `BitcoinPayment` does not
implement `Refundable`.

`BitcoinExchangeRate` supplies the ZAR value of one BTC. A production implementation must use a
trusted provider quote with timestamp/expiry, handle quote movement and network fees, and wait for
the required blockchain confirmations. This project only simulates the payment request; it does
not connect to a wallet, exchange, card processor or blockchain.


## Business Rules

* The amount charged at checkout is the order grand total (subtotal + VAT), calculated once by
  `Order.calculateTotalInclVat()`. The printed summary reads the same method.
* Payment amounts are `double` (as in the brief). The Capstone 1 total is `BigDecimal`, and it is
  converted to `double` in exactly one place, `CheckoutService.checkout`.
* Amounts print with two decimals and a fixed locale, e.g. `R18457.50`.
* A failed payment leaves the order unpaid, so the customer can try another rail.
* A paid order is never charged twice, and its items can no longer be changed.

## Known Limitations

* The rails are simulations (no bank, card network or gateway). Never use real card data.
* Stock is reduced when an item is added to an order and is not released if the order is abandoned.
* `double` is not exact for money. It is used here because the brief specifies it; a production
  system would use `BigDecimal`.

## Author

**Name:** Aridivhaho Junior Nemadodzi

Capstone 1 and Capstone 3 · Java Programming

## License

Developed for educational purposes as part of a Java programming capstone assessment ~ Ari.
