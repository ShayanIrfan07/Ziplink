# Ziplink

Short links. Smart insights.

Ziplink turns a long `http` or `https` URL into a short link, stores it in PostgreSQL, and redirects visitors to the original destination. Links created in a browser stay with that browser until accounts arrive.

## What works now

This is the Phase 1 URL shortener:

- Create a short link from the landing page.
- Optional custom alias and expiration.
- Copy or open the short link.
- Public redirect at `GET /{shortCode}`.
- List, search, sort, disable, enable, and delete links owned by the current browser.
- Click events are stored for later analytics. There is no analytics dashboard yet.

## What is not built yet

Registration, login, per-user dashboards, analytics charts, Redis caching, Kafka, and splitting the backend into separate services are later phases. Do not treat those as available.

## Stack

- Frontend: Next.js 16, React 19, TypeScript, Tailwind CSS, shadcn-style components, React Hook Form, Zod
- Backend: Java 21, Spring Boot 4.1, Spring Security, Spring Data JPA, Flyway, PostgreSQL
- Tests: JUnit 5, Mockito, Testcontainers, Vitest

## Architecture

Ziplink is a modular Spring Boot application plus a Next.js UI. The backend owns links, redirects, and the database. The frontend calls `/api/v1` and does not store link data itself.

Important decisions for this phase:

- **Ownership.** There are no user accounts yet. Creating a link sets an HTTP-only cookie, `ziplink_guest`, whose value is a random token. The database stores only the SHA-256 hash of that token. Listing, updating, and deleting require that cookie. A missing or foreign link returns 404, so one browser cannot read another browser's links. Anonymous creation is rate limited to 20 requests per minute per client address, in memory, for this single process.
- **CSRF.** The guest cookie is a credential. Spring Security issues a readable `XSRF-TOKEN` cookie, and the frontend sends it back as `X-XSRF-TOKEN`. Cookies use `SameSite=Lax` and are marked `Secure` only when `COOKIE_SECURE=true`. There is no default username or password. HTTP basic and form login are disabled.
- **Redirects use 302.** Destinations, status, and expiration can change. A 301 would be cached by browsers after those changes. Redirect responses send `Cache-Control: no-store` and `Referrer-Policy: no-referrer`.
- **Destination safety.** Only `http` and `https` URLs are accepted. Other schemes, missing hosts, embedded credentials, and control characters are rejected. The backend does not fetch the destination. Redirecting to an address the creator supplied is the product; it is not an open redirect of an arbitrary `next` parameter.
- **Clicks.** A click is a successful redirect. Missing, disabled, and expired links are not counted. Requests that look like bots, monitors, `curl`, or browser prefetch are not stored. The stored referrer keeps the scheme, host, and path, and drops the query string and fragment. Raw IP addresses and raw user-agent strings are not stored. The category is an approximate label derived from the user agent: desktop, mobile, tablet, or unknown. Recording runs after the redirect decision. If the recorder is busy or fails, the visitor is still redirected. This is not exactly-once delivery: a refresh counts as another click.
- **Aliases.** A custom alias becomes the short code. It must be 3–32 characters from `A-Z`, `a-z`, `0-9`, `_`, and `-`. Reserved names such as `api` and `swagger-ui` are rejected. Uniqueness is case-insensitive, and an existing code is never overwritten.

The original static page that called a third-party shortener is kept in `legacy/static-prototype`. It is not part of the running application.

## Prerequisites

- JDK 21
- Maven 3.9+
- Node.js 24
- Docker, for PostgreSQL and the database integration tests

## Local setup

Start PostgreSQL:

```powershell
docker compose -f infra/docker-compose.yml up -d
```

The compose file publishes PostgreSQL on host port **5433** so it does not collide with another database already using 5432. The local placeholder password is `ziplink`. That value is for this machine only.

Start the API from `backend`:

```powershell
mvn spring-boot:run
```

The API listens on `http://localhost:8080`. Interactive docs are at `http://localhost:8080/swagger-ui.html`. Liveness is `http://localhost:8080/actuator/health/liveness`.

Start the UI from `frontend`:

```powershell
npm install
npm run dev
```

Open `http://localhost:3000`. The UI calls `http://localhost:8080` unless `NEXT_PUBLIC_API_BASE_URL` is set. Copy `frontend/.env.example` to `frontend/.env.local` to change it.

Create a link on the home page, then open the returned short URL. It is served by the API, for example `http://localhost:8080/docs-link`.

## Environment variables

See `.env.example`. Local defaults in `backend/src/main/resources/application.yml` match the compose file, so a local run does not need a shell export.

| Variable | Purpose |
| --- | --- |
| `DATABASE_URL` | JDBC URL |
| `DATABASE_USERNAME` | Database user |
| `DATABASE_PASSWORD` | Database password |
| `PUBLIC_BASE_URL` | Origin used to build short links |
| `CORS_ALLOWED_ORIGINS` | Comma-separated browser origins allowed to call the API with cookies |
| `COOKIE_SECURE` | Set `true` behind HTTPS |
| `PORT` | API port |
| `FORWARD_HEADERS_STRATEGY` | Set `framework` when a trusted proxy terminates TLS. Leave `none` otherwise. The app does not trust `X-Forwarded-For` on its own. |
| `CREATE_RATE_LIMIT_PER_MINUTE` | Anonymous create limit for one API process |
| `NEXT_PUBLIC_API_BASE_URL` | API origin used by the browser. This is public configuration, not a secret. |

Set `COOKIE_SECURE=true` and an `https` public URL in production. Do not commit a real `.env`.

## Database

Flyway applies `backend/src/main/resources/db/migration` when the API starts. Hibernate validates the schema and does not change it. There is no automatic destructive migration.

## Tests

Backend, from `backend`:

```powershell
mvn test
```

Unit tests cover URL validation, aliases, code generation, rate limiting, ownership, expiration, and click filtering. `UrlFlowIntegrationTest` starts PostgreSQL with Testcontainers and is skipped when Docker is not running.

Frontend, from `frontend`:

```powershell
npm test
npm run lint
npm run typecheck
npm run build
```

## API

| Method | Path | Result |
| --- | --- | --- |
| `POST` | `/api/v1/urls` | `201` and the new link. Sets the guest cookie when needed. |
| `GET` | `/api/v1/urls` | Page of links for the guest cookie. An empty page when the cookie is absent. |
| `GET` | `/api/v1/urls/{id}` | One owned link, or `404`. |
| `PATCH` | `/api/v1/urls/{id}` | Change destination, status, or expiration. |
| `DELETE` | `/api/v1/urls/{id}` | `204` when the link belonged to the guest. |
| `GET` | `/{shortCode}` | `302` when the link is active. `404` when it is missing or disabled. `410` when it is expired. |

Unsafe requests need the CSRF header. Errors use `{ "code", "message", "fields", "timestamp", "path" }` and do not include stack traces. Page size is limited to 100. Sort fields are `createdAt`, `expiresAt`, and `shortCode`.

## Docker

`infra/docker-compose.yml` runs PostgreSQL for local development. `backend/Dockerfile` and `frontend/Dockerfile` build the API and the UI. They are not required for the local Maven and npm workflow.

## Deployment notes

Bind the API to `0.0.0.0` and the platform port. The checked-in configuration does that. Use managed PostgreSQL, set the variables above, and run the same Flyway migrations on startup. Put the short-link host in `PUBLIC_BASE_URL` and the UI origin in `CORS_ALLOWED_ORIGINS`. Health checks should use `/actuator/health/liveness` and `/actuator/health/readiness`. Actuator exposes only health and info, without environment details.

Redis, Kafka, and separate deployable services are intentionally absent.
