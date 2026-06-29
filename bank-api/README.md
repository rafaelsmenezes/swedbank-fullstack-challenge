# Bank API — Swedbank Backend

**Stack**  
Java 21 · Spring Boot 4.1 · Gradle · H2 · MapStruct · Lombok · JUnit 5

## Running locally

```bash
./gradlew bootRun
```

API: http://localhost:8080  
H2 Console: http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:mem:bankdb`)

## Running tests

```bash
./gradlew test
```

## Key design decisions

- BigDecimal for all monetary values (no float/double)
- Fixed exchange rates with EUR as base currency
- External logging simulation via WebClient (non-blocking, failure-tolerant)
- MapStruct for entity→DTO mapping (compile-time, no reflection)
- @Transactional on all write operations
- Pageable for transaction history (supports infinite scroll)
- H2 in-memory (explicitly allowed by challenge spec)
- DataInitializer creates seed data with 25 transactions per account

## Package structure

```bash
com.swedbank.bankapi/
├── config/       CorsConfig, DataInitializer, WebClientConfig
├── controller/   AccountController, TransactionController, ExchangeController
├── domain/       User, Account, Transaction, Currency, TransactionType
├── dto/          AccountDto, TransactionDto, CreditRequest, DebitRequest, ExchangeResponse
├── exception/    GlobalExceptionHandler + custom exceptions
├── mapper/       AccountMapper, TransactionMapper (MapStruct)
├── repository/   UserRepository, AccountRepository, TransactionRepository
└── service/      AccountService, ExchangeService, ExternalLogService
```

## Future Improvements

### Domain & Persistence
- [ ] Add `@Version` (optimistic locking) or `@Lock` (pessimistic) on the Account entity — prevents lost updates on concurrent balance modifications.
- [ ] Move business rules (`balance >= 0`, currency rules) into the `Account` entity methods instead of the service.
- [ ] Replace manual `@PrePersist` for `createdAt` with Hibernate `@CreationTimestamp`.
- [ ] Consider using a proper `MonetaryAmount` / `javax.money` type instead of `BigDecimal + String currency`.

### Service Layer & Transactions
- [ ] Extract a dedicated `DebitUseCase` / `CreditUseCase` class — keeps `AccountService` from mixing concerns (balance mutation + external side effects + persistence).
- [ ] Perform the external log **before** or **after** the transaction (not inside it) — current placement couples I/O with data consistency.
- [ ] Add support for idempotency keys on credit/debit (e.g. via header + table of processed keys).

### Testing
- [ ] Add concurrent tests that actually exercise the race condition between two debit requests.
- [ ] Better coverage of the `ExchangeService` (negative amounts, unsupported currencies, rounding edge cases).
- [ ] Test the full `DataInitializer` behavior (currently only implicitly tested).
- [ ] Integration tests that verify the external logging call was attempted (with WireMock).

### API & Validation
- [ ] Add maximum amount validation and currency whitelist at the DTO level.
- [ ] Return more structured error responses (problem details / RFC 7807 style).
- [ ] Add ETag / If-Match support for optimistic updates on accounts.
```
