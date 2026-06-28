# Bank Frontend — Swedbank Angular App

**Stack**  
Angular 21 · TypeScript · Chart.js · jsPDF · zone.js · Nginx

## Running locally

```bash
npm install
ng serve
```

App: http://localhost:4200  
Requires backend running on http://localhost:8080

## Pages

- `/` — Home (lists all accounts with balances)
- `/account/:id` — Account overview (balance, chart, transactions, credit/debit)
- `/transaction/:id` — Transaction detail (fields + PDF export)

## Key technical decisions

- Standalone components (no NgModules)
- SSR disabled (`--ssr=false`)
- Chart.js used directly (no ng2-charts — version conflict with Angular 21)
- IntersectionObserver for infinite scroll (no external library)
- `provideHttpClient()` without `withFetch()` (required for zone.js compatibility)
- zone.js installed manually (Angular 21 defaults to zoneless)
- Signals for all reactive state
- `inject()` instead of constructor injection

## Build for production

```bash
npm run build
# Output: dist/bank-frontend/browser/
```

## Docker

Served via nginx with:

- SPA routing fallback (`try_files` → `index.html`)
- `/api/**` proxied to `bank-api:8080`
- Long-term caching for hashed assets
- No caching for `index.html`
