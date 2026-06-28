# Swedbank Fullstack Challenge

A fullstack banking application demonstrating multi-currency accounts, transactions with currency conversion, and a modern Angular frontend.

## Tech Stack

| Layer          | Technology |
|----------------|------------|
| **Backend**    | Java 21, Spring Boot 4.1, Gradle, H2 (in-memory), MapStruct, Lombok |
| **Frontend**   | Angular 21, Chart.js, jsPDF, standalone components |
| **Infrastructure** | Docker, Docker Compose, Nginx (reverse proxy + SPA), GitHub Actions CI |

## Features

- Multi-currency accounts (EUR, USD, SEK, GBP, VND)
- Credit with automatic currency conversion
- Debit with currency and balance validation
- External logging simulation before debit (httpstat.us)
- Transaction history with infinite scroll pagination
- Balance timeline chart (Chart.js)
- PDF export for transactions (jsPDF)
- Fixed exchange rates (EUR-based)

## Architecture

```
Browser → Nginx (port 80) → Angular SPA
                    → /api/** proxy → Spring Boot (port 8080) → H2
```

## Quick Start (Docker — Recommended)

```bash
git clone https://github.com/rafaelmenezes/swedbank-fullstack-challenge
cd swedbank-fullstack-challenge
docker compose up --build
```

Access the app at: **http://localhost**

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

## Default Seed Data

User automatically created on startup:

- **Email**: rafael@bank.com
- **EUR Account**: ~1500 EUR
- **USD Account**: ~800 USD
- **SEK Account**: ~5000 SEK
- 25 transactions per account (for infinite scroll demo)

## Running Tests

```bash
cd bank-api
./gradlew test
```

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
