# Swedbank Fullstack Challenge

**🔗 Live Demo:** [https://swedbank-fullstack-challenge-production.up.railway.app](https://swedbank-fullstack-challenge-production.up.railway.app)  
**🔗 API (Swagger UI):** [https://swedbank-bank-api-production.up.railway.app/swagger-ui.html](https://swedbank-bank-api-production.up.railway.app/swagger-ui.html)

![CI](https://github.com/rafaelmenezes/swedbank-fullstack-challenge/actions/workflows/ci.yml/badge.svg)

A fullstack banking application demonstrating multi-currency accounts, transactions with currency conversion, and a modern Angular frontend.

## Tech Stack

| Layer          | Technology |
|----------------|------------|
| **Backend**    | Java 21, Spring Boot 4.1, Gradle, MapStruct, Lombok |
| **Database**   | H2 (local dev) / PostgreSQL 16 (Docker/Railway) |
| **Frontend**   | Angular 21, Chart.js, jsPDF, standalone components |
| **Infrastructure** | Docker, Docker Compose, Nginx (reverse proxy + SPA), GitHub Actions CI |

## Features

- Multi-currency accounts (EUR, USD, SEK, GBP, VND)
- Credit with automatic currency conversion
- Debit with currency and balance validation
- Quick Transfer panel for cross-account deposits with currency conversion
- External logging simulation before debit (httpstat.us)
- Transaction history with infinite scroll pagination
- Balance timeline chart (Chart.js)
- PDF export for transactions (jsPDF)
- Fixed exchange rates (EUR-based)

## Architecture

```
Browser → Nginx (port 80) → Angular SPA
                    → /api/** proxy → Spring Boot (port 8080) → H2 / PostgreSQL
```

## Quick Start (Docker — Recommended)

```bash
git clone https://github.com/rafaelmenezes/swedbank-fullstack-challenge
cd swedbank-fullstack-challenge
docker compose up --build
```

Access the app at: **<http://localhost>**

## Quick Start (Development)

**Backend:**

```bash
cd bank-api
./gradlew bootRun
# API available at http://localhost:8080
# H2 Console: http://localhost:8080/h2-console
```

**Frontend:**

```bash
cd bank-frontend
npm install
ng serve
# App available at http://localhost:4200
```

## API Overview

| Method | Endpoint                                      | Description                              |
|--------|-----------------------------------------------|------------------------------------------|
| GET    | /api/v1/accounts?userId=1                     | List user accounts                       |
| GET    | /api/v1/accounts/{id}                         | Account details                          |
| POST   | /api/v1/accounts/{id}/credit                  | Credit (with currency conversion)        |
| POST   | /api/v1/accounts/{id}/debit                   | Debit (same currency only)               |
| GET    | /api/v1/accounts/{id}/transactions            | Paginated transaction history            |
| GET    | /api/v1/transactions/{id}                     | Transaction details                      |
| GET    | /api/v1/exchange?from=USD&to=EUR&amount=100   | Currency conversion                      |

## API Documentation

Interactive Swagger UI available at:

- Local: <http://localhost:8080/swagger-ui.html>
- Production: <https://swedbank-bank-api-production.up.railway.app/swagger-ui.html>

## Default Seed Data

User automatically created on startup:

- **Email**: <user@bank.com>
- **EUR Account**: ~1500 EUR
- **USD Account**: ~800 USD
- **SEK Account**: ~5000 SEK
- **GBP Account**: ~1200 GBP
- **VND Account**: ~25,000,000 VND
- 25 transactions per account (for infinite scroll demo)

## Running Tests

```bash
cd bank-api
./gradlew test
```

## Live Demo Script

See [DEMO.md](DEMO.md) for the presentation walkthrough.

## CI/CD

GitHub Actions pipeline (`.github/workflows/ci.yml`):

- `backend-tests`: Checkout → Setup JDK 21 → `./gradlew test --no-daemon` → Upload test results
- `frontend-build`: Checkout → Setup Node 22 → `npm ci` → `npm run build` → Upload dist artifact
- `docker-build`: Depends on both previous jobs → `docker compose build` → Start containers → Healthcheck both services (`/api/v1/accounts` + `/`) → `docker compose down`

## Project Structure

```bash
swedbank-fullstack-challenge/
├── bank-api/                    # Spring Boot backend
│   ├── src/main/java/com/swedbank/bankapi/
│   │   ├── config/              # CORS, DataInitializer, WebClient
│   │   ├── controller/          # REST controllers
│   │   ├── domain/              # JPA entities + enums
│   │   ├── dto/                 # Request/Response DTOs
│   │   ├── exception/           # Global exception handler
│   │   ├── mapper/              # MapStruct mappers
│   │   ├── repository/          # Spring Data repositories
│   │   └── service/             # Business logic
│   └── src/test/                # Unit tests (JUnit 5 + Mockito)
├── bank-frontend/               # Angular frontend
│   ├── src/app/
│   │   ├── core/                # Models + Services
│   │   ├── features/            # Pages (Home, AccountOverview, TransactionOverview)
│   │   └── shared/              # Pipes
│   └── nginx.conf               # Production nginx config
└── docker-compose.yml           # Orchestrates both services
```

## Future Improvements

This section documents known gaps and intentional trade-offs made during the rapid development of this technical challenge.

### Security

- [ ] Spring Security + JWT / OAuth2 authentication — currently any client can access any account by guessing IDs; real banking requires authenticated sessions with ownership validation.
- [ ] Account ownership enforcement — add checks so users can only access their own accounts (service + controller layer).
- [ ] Rate limiting on write endpoints (credit/debit) — protects against abuse and accidental double-spends.
- [ ] Secrets management — database credentials are currently in plain properties and docker-compose.

### Concurrency & Data Integrity

- [ ] Pessimistic or optimistic locking on Account balance updates — the current read-modify-write pattern in `AccountService` is vulnerable to lost updates under concurrent credit/debit.
- [ ] Idempotency keys for credit and debit operations — prevents duplicate side effects on retries.
- [ ] Move external logging outside the `@Transactional` boundary — side effects inside transactions are fragile.

### Performance & Scalability

- [ ] Redis (or simple cache) for exchange rates — rates are hardcoded in a static map; dynamic rates would require a cache layer.
- [ ] Proper database indexing strategy — especially on `transactions.account_id` + `created_at`.
- [ ] Virtualized or cursor-based loading for very large transaction histories on the frontend.

### Observability

- [ ] Structured logging with correlation IDs across the stack (frontend → API → external log).
- [ ] Metrics (Micrometer + Prometheus) for critical paths: credit/debit success rate, conversion latency, external log health.
- [ ] Better health indicators and readiness probes suitable for Railway / Kubernetes.

### Testing

- [ ] Concurrency / load tests specifically for the debit race condition.
- [ ] Contract tests between frontend and backend.
- [ ] End-to-end tests (Playwright/Cypress) covering chart rendering, PDF export, and infinite scroll.
- [ ] Property-based testing for the currency conversion logic.

### Architecture

- [ ] Move balance mutation logic into the `Account` domain entity (`account.debit(...)`) instead of the service layer.
- [ ] Introduce a proper `Money` value object instead of raw `BigDecimal + String`.
- [ ] Replace `create-drop` + imperative seed with Flyway/Liquibase migrations (critical once we stop using H2 locally).
- [ ] Consider introducing an Application Service / Use Case layer to keep controllers thin.

### Infrastructure & Deployment

- [ ] Remove `ddl-auto=create-drop` from the `docker` profile — data loss on every container restart.
- [ ] Make the Nginx backend target configurable (build arg or runtime env) instead of hardcoding the Railway internal hostname.
- [ ] Add container vulnerability scanning and SBOM generation in CI.
- [ ] Separate production profile with `validate` (or `none`) + proper connection pool configuration.

### Frontend

- [ ] Extract the very large inline templates from component `.ts` files.
- [ ] Replace `setTimeout` hacks for Chart.js with proper lifecycle management and `ResizeObserver`.
- [ ] Add subscription cleanup using `takeUntilDestroyed`.
- [ ] Global HTTP error handling + toast service instead of scattered banners.
- [ ] Add frontend tests (component + e2e).

See also the more detailed, technology-specific lists:

- [Bank API Future Improvements](bank-api/README.md#future-improvements)
- [Bank Frontend Future Improvements](bank-frontend/README.md#future-improvements)

```
