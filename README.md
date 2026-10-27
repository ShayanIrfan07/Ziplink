# Ziplink

Short links. Smart insights.

Ziplink turns a long `http` or `https` URL into a short link, stores it in PostgreSQL, and redirects visitors to the original destination. An account keeps that library private. Without an account, links stay with the browser that created them.

## What works now

- Create a short link from the landing page, with an optional alias and expiration.
- Register, sign in, and sign out with an HTTP-only session cookie.
- A signed-in dashboard with link totals, click totals, active and expired counts, top links, and recent clicks.
- List, search, sort, disable, enable, and delete only the links that belong to the signed-in account.
- Visitors who are not signed in can still create and manage links through the browser cookie.
- Public redirect at `GET /{shortCode}`.
- Click events are stored. The signed-in dashboard shows totals, a daily UTC click chart, client categories, and the top referrers for the last 7, 14, or 30 days.
- Redirect lookups are cached in Redis. A changed, disabled, or deleted link is removed from the cache. If Redis is unavailable, the redirect is read from PostgreSQL.

## What is not built yet

Kafka and splitting the backend into separate services are later phases. Do not treat those as available.

## Stack

- Frontend: Next.js 16, React 19, TypeScript, Tailwind CSS, shadcn-style components, React Hook Form, Zod
- Backend: Java 21, Spring Boot 4.1, Spring Security, Spring Data JPA, Flyway, PostgreSQL, Redis
- Tests: JUnit 5, Mockito, Testcontainers, Vitest

## Architecture

Ziplink is a modular Spring Boot application plus a Next.js UI. The backend owns links, redirects, and the database. The frontend calls `/api/v1` and does not store link data itself.

Important decisions for this phase:

- **Accounts.** Registration and login use BCrypt password hashes. The browser receives an HTTP-only `ziplink_session` cookie. The database stores only the SHA-256 hash of that token. A session lasts 14 days and is deleted on logout. Login and registration are limited to 10 attempts per minute per client address, in memory, for this single process. Failed login always returns the same message.
- **Ownership.** A signed-in request creates and lists links by account id. A missing or foreign link returns 404. Signing up or signing in from a browser that already has guest links moves those links onto the account and clears the guest association, so a later account on the same browser does not inherit them. Without a session, the HTTP-only `ziplink_guest` cookie still scopes anonymous links. Anonymous creation is rate limited to 20 requests per minute per client address.
- **CSRF.** Session and guest cookies are credentials. Spring Security issues a readable `XSRF-TOKEN` cookie, and the frontend sends it back as `X-XSRF-TOKEN`. Cookies use `SameSite=Lax` and are marked `Secure` only when `COOKIE_SECURE=true`. HTTP basic and form login are disabled. Passwords, session tokens, and password hashes are not written to logs.
- **Redirect cache.** An active redirect is stored in Redis under the lowercased short code for up to 10 minutes, or sooner when the link has an expiration. Updating or deleting the link removes that entry after the database change commits. Redis does not decide whether a missing or disabled link exists, and a Redis failure falls back to PostgreSQL. Browser redirects stay temporary (`302`, `Cache-Control: no-store`).
- **Redirects use 302.** Destinations, status, and expiration can change. A 301 would be cached by browsers after those changes. Redirect responses send `Cache-Control: no-store` and `Referrer-Policy: no-referrer`.
- **Destination safety.** Only `http` and `https` URLs are accepted. Other schemes, missing hosts, embedded credentials, and control characters are rejected. The backend does not fetch the destination. Redirecting to an address the creator supplied is the product; it is not an open redirect of an arbitrary `next` parameter.
- **Clicks.** A click is a successful redirect. Missing, disabled, and expired links are not counted. Requests that look like bots, monitors, `curl`, or browser prefetch are not stored. The stored referrer keeps the scheme, host, and path, and drops the query string and fragment. Raw IP addresses and raw user-agent strings are not stored. The category is an approximate label derived from the user agent: desktop, mobile, tablet, or unknown. Recording runs after the redirect decision. If the recorder is busy or fails, the visitor is still redirected. This is not exactly-once delivery: a refresh counts as another click.
- **Aliases.** A custom alias becomes the short code. It must be 3–32 characters from `A-Z`, `a-z`, `0-9`, `_`, and `-`. Reserved names such as `api` and `swagger-ui` are rejected. Uniqueness is case-insensitive, and an existing code is never overwritten.

The original static page that called a third-party shortener is kept in `legacy/static-prototype`. It is not part of the running application.

## Prerequisites

- JDK 21
- Maven 3.9+
- Node.js 24
- Docker, for PostgreSQL, Redis, and the integration tests

## Local setup

Start PostgreSQL and Redis:

```powershell
docker compose -f infra/docker-compose.yml up -d
```

The compose file publishes PostgreSQL on host port **5433** so it does not collide with another database already using 5432. Redis is published on **6379**. The local placeholder password is `ziplink`. That value is for this machine only.

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
| `AUTH_RATE_LIMIT_PER_MINUTE` | Login and registration limit for one API process |
| `SESSION_MAX_AGE_DAYS` | Session cookie lifetime |
| `NEXT_PUBLIC_API_BASE_URL` | API origin used by the browser. This is public configuration, not a secret. |
| `REDIS_HOST` | Redis host. Redirects still use PostgreSQL when Redis cannot be reached. |
| `REDIS_PORT` | Redis port. The compose file publishes `6379`. |
| `REDIS_CACHE_ENABLED` | Set `false` to skip the redirect cache. |
| `REDIS_TTL_SECONDS` | Maximum age of a cached redirect, from 1 to 86400. |

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
| `POST` | `/api/v1/auth/register` | `201` and the account. Sets the session cookie. `409` when the email is taken. |
| `POST` | `/api/v1/auth/login` | `200` and the account. Sets the session cookie. `401` for a bad password. |
| `POST` | `/api/v1/auth/logout` | `204`. Requires a session. Deletes that session. |
| `GET` | `/api/v1/auth/me` | The signed-in account, or `401`. |
| `GET` | `/api/v1/dashboard/summary` | Counts, top links, and recent clicks for the signed-in account, or `401`. |
| `GET` | `/api/v1/dashboard/clicks` | Daily UTC click counts, client categories, and up to five referrers for the signed-in account. `days` is 1–90 and defaults to 14. Optional `linkId` limits the series to one owned link, or returns `404`. |
| `POST` | `/api/v1/urls` | `201` and the new link. Uses the session when present, otherwise the guest cookie. |
| `GET` | `/api/v1/urls` | Page of links for the session, or for the guest cookie when signed out. |
| `GET` | `/api/v1/urls/{id}` | One owned link, or `404`. |
| `PATCH` | `/api/v1/urls/{id}` | Change destination, status, or expiration. |
| `DELETE` | `/api/v1/urls/{id}` | `204` when the link belonged to the caller. |
| `GET` | `/{shortCode}` | `302` when the link is active. `404` when it is missing or disabled. `410` when it is expired. |

Unsafe requests need the CSRF header. Errors use `{ "code", "message", "fields", "timestamp", "path" }` and do not include stack traces. Page size is limited to 100. Sort fields are `createdAt`, `expiresAt`, and `shortCode`.

## Docker

`infra/docker-compose.yml` runs PostgreSQL and Redis for local development. `backend/Dockerfile` and `frontend/Dockerfile` build the API and the UI. They are not required for the local Maven and npm workflow.

## Deployment notes

Bind the API to `0.0.0.0` and the platform port. The checked-in configuration does that. Use managed PostgreSQL, set the variables above, and run the same Flyway migrations on startup. Put the short-link host in `PUBLIC_BASE_URL` and the UI origin in `CORS_ALLOWED_ORIGINS`. Health checks should use `/actuator/health/liveness` and `/actuator/health/readiness`. Actuator exposes only health and info, without environment details.

Point `REDIS_HOST` at a Redis instance when the redirect cache should be used. Readiness stays up when Redis is down, and redirects fall back to PostgreSQL. Kafka and separate deployable services are later phases.
