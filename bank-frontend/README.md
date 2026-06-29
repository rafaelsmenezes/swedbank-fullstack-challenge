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

## Environment-Specific Nginx Configuration

Because service discovery works differently locally vs in Railway, we maintain two Nginx configurations:

| File                  | Environment       | Backend Upstream                          | When used |
|-----------------------|-------------------|-------------------------------------------|-----------|
| `nginx.conf`          | Railway / Prod    | `swedbank-bank-api.railway.internal:8080` | Default in Docker image |
| `nginx.local.conf`    | Local Docker      | `bank-api:8080`                           | Mounted at runtime in `docker-compose.yml` |

### How it works

**Dockerfile** supports a build argument (though we prefer volume mount for local dev):

```dockerfile
ARG NGINX_CONF=nginx.conf
COPY ${NGINX_CONF} /etc/nginx/conf.d/default.conf
```

**docker-compose.yml** (local development):

```yaml
bank-frontend:
  build:
    context: ./bank-frontend
  volumes:
    - ./bank-frontend/nginx.local.conf:/etc/nginx/conf.d/default.conf:ro
```

When deploying to Railway (or any other environment), the image is built **without** the volume mount, so `nginx.conf` (the default) is used.

### Switching environments

- **Local development**: Just run `docker compose up --build`. The volume mount automatically uses the local config.
- **Railway / Production**: Build the image as normal. No volume mounts are applied, so the production `nginx.conf` is baked in.
- To force the local config in a one-off build:
  ```bash
  docker build --build-arg NGINX_CONF=nginx.local.conf -t bank-frontend:local ./bank-frontend
  ```

This approach keeps the two environments as similar as possible while solving the fundamental difference in internal DNS / service names.

## Future Improvements

### Architecture & Patterns
- [ ] Move large inline `template:` strings out of `.ts` files into proper `.html` templates — current approach hurts readability and editor support.
- [ ] Introduce a lightweight state management solution (e.g. NgRx Signals or a simple service with `computed()`) for shared state instead of scattering logic across components.
- [ ] Replace manual `setTimeout(0)` + `ViewChild` timing workarounds for Chart.js with `AfterViewInit` + `ResizeObserver` or a small wrapper component.

### Reactivity & RxJS
- [ ] Add proper subscription management using `takeUntilDestroyed()` (from `@angular/core/rxjs-interop`) on all `HttpClient.subscribe()` calls.
- [ ] Prefer the `async` pipe or `toSignal()` over manual `.subscribe()` where possible.
- [ ] Add a global HTTP interceptor for error handling instead of repeating error logic in every component.

### Performance & UX
- [ ] Use virtual scrolling (e.g. `@angular/cdk/scrolling`) or proper cursor-based pagination instead of loading all transactions into memory.
- [ ] Improve Chart.js integration (avoid full recreation on every update; use `chart.update()` with dataset replacement).
- [ ] Add skeleton loading states that are consistent across Home and Account Overview.
- [ ] Lazy load the PDF generation logic (jsPDF is currently bundled even if the user never exports).

### Testing
- [ ] Add component tests using Angular's `TestBed` + `ComponentFixture`.
- [ ] Add E2E tests (Playwright or Cypress) covering: Quick Transfer flow, credit/debit, chart rendering, PDF download, and infinite scroll.
- [ ] Visual regression testing for the branded design (especially the timeline chart).

### Observability & DX
- [ ] Add global error boundary / toast service for unhandled HTTP errors.
- [ ] Better loading and error states for the IntersectionObserver-based infinite scroll.
- [ ] Track chart initialization failures (currently silent if canvas is not ready).
```
