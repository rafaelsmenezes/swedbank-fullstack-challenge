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
