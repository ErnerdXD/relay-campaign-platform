# Relay

Relay is a multi-tenant notification and campaign platform. Operations teams upload a recipient CSV, pick a channel, and watch email, SMS, and push delivery in real time. Every send is simulated, recorded, and safe to retry.

Built for the Appfuxion take-home assessment: **Full-Stack + Mobile**.

![Relay operations dashboard](docs/dashboard.png)

![Campaign detail with delivery attempts and retry controls](docs/campaign-detail.png)

| Surface | Stack |
| --- | --- |
| Campaign API | Java 21, Spring Boot 4, Spring JDBC, Flyway, H2 |
| Operations dashboard | React 19, TypeScript, Vite, TanStack Query |
| Monitoring app | Expo SDK 58, React Native 0.88, TypeScript |
| Delivery | Durable database queue, virtual-thread worker, token bucket, exponential backoff |
| Delivery | Docker Compose, GitHub Actions |

## What works

- Create a campaign from a streamed CSV with per-row validation, preview, upload progress, and idempotent retries.
- Tenant-scoped campaigns, recipients, jobs, attempts, and suppression lists. A token can never read another tenant's data.
- Search, status/channel filters, pagination, and four-second live polling on the campaign list.
- Detail view with sent/failed/skipped/pending counts, per-minute delivery graph, error breakdown, retry queue, and manual failure retry.
- Simulated email, SMS, and push providers with 50–200 ms latency and a 20% failure rate.
- Per-channel token bucket at 100 requests/minute, three attempts with exponential backoff and jitter, and a durable retry schedule.
- Rule chain: global suppression, five-minute message deduplication, SMS/push quiet hours (22:00–08:00 local, transactional exempt), and monthly tenant quota.
- Structured, PII-free logs with `tenantId`, `campaignId`, `notificationId`, attempt, and status.
- Expo app: campaign health board, pull-to-refresh, offline cache, push preview with collapsed/expanded and narrow/wide states, local failure alerts, and biometric lock.

## Architecture

```
┌──────────────┐      ┌──────────────┐      ┌──────────────────────────┐
│ React web    │─────▶│ nginx / Vite │─────▶│ Spring Boot API          │
│ Expo mobile  │      │  /api proxy  │      │  auth · CSV · queries    │
└──────────────┘      └──────────────┘      └────────────┬─────────────┘
                                                        │
                                       ┌────────────────▼────────────────┐
                                       │ H2 (file, demo) / PostgreSQL    │
                                       │ tenants · campaigns · recipients│
                                       │ jobs · attempts · suppressions  │
                                       └────────────────┬────────────────┘
                                                        │ poll + claim
                                       ┌────────────────▼────────────────┐
                                       │ Delivery worker                  │
                                       │ 16 virtual threads · rules       │
                                       │ token bucket · backoff · logs    │
                                       └────────────────┬────────────────┘
                                                        │ 50–200 ms, 20% failure
                                       ┌────────────────▼────────────────┐
                                       │ Simulated providers              │
                                       │ EMAIL · SMS · PUSH               │
                                       └─────────────────────────────────┘
```

The API writes a campaign, recipients, and one durable job per recipient in a single transaction. The worker claims due jobs into `PROCESSING`, applies the rule chain, acquires a channel token, calls the simulated provider, and records every attempt. Campaign status is derived from terminal job states, so a crash never leaves a campaign "running" forever.

## Quick start

### Docker

```bash
docker compose up --build
```

Open [http://localhost:3000](http://localhost:3000). The API is also exposed at `http://localhost:8081/api`. Data persists in the `relay-data` volume.

### Local

Requirements: **JDK 21**, **Node.js 22+**.

```bash
# terminal 1 — API on :8081
cd api
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run

# terminal 2 — dashboard on :5173
cd web
npm ci
npm run dev

# terminal 3 — mobile (optional)
cd mobile
npm ci
npm start
```

Vite proxies `/api` to `localhost:8081`. The Expo app derives the API host from the Metro host, so a phone on the same network reaches the API without configuration. Override it with `EXPO_PUBLIC_API_URL` if needed.

To reset the demo database, stop the API and delete `api/data/`.

### Demo workspaces

`GET /api/session` exposes fixture credentials for the local demo only.

| Tenant | ID | Bearer token | Timezone |
| --- | --- | --- | --- |
| Northstar Bank | `northstar` | `demo-northstar-local` | Asia/Kuala_Lumpur |
| Meridian Air | `meridian` | `demo-meridian-local` | Asia/Singapore |

`demo-unsubscribed` is seeded on the suppression list for email at Northstar and SMS at Meridian.

## Try the workflow

1. Select **New campaign**, choose a channel, and paste a template. `{{recipientId}}` is replaced by the simulator.
2. Download the sample CSV, upload it, and check the first rows in the preview.
3. Create the campaign. The API returns `202` and a campaign id; the detail page polls until delivery finishes.
4. Use the tenant selector in the header to confirm Meridian cannot see Northstar's campaigns.
5. Open a campaign with failures and select **Retry failed**. Only terminal failures are requeued; sent jobs are never repeated.
6. In the Expo app, pull to refresh the health board, preview a push in both widths, and lock/unlock with biometrics.

## CSV format

```csv
recipientId,email,phone,timezone
ada,ada@example.com,,Asia/Kuala_Lumpur
grace,grace@example.com,+60123456789,
linus,linus@example.com,+6591234567,Asia/Singapore
```

- `recipientId` is required and unique within the upload.
- `email` is required for `EMAIL` campaigns; `phone` is required for `SMS` and must be E.164.
- `timezone` is optional; the tenant timezone is the fallback. Invalid zones are rejected.
- Rows are streamed and inserted in batches of 500. A bad row rejects the whole campaign.
- Header aliases are not accepted; the columns above are the contract.

## Delivery rules

| Rule | Scope | Action |
| --- | --- | --- |
| Global suppression | Recipient + channel | `SKIPPED` with `recipient_unsubscribed` |
| Deduplication | Same tenant, channel, recipient, and template sent in the last 5 minutes | `SKIPPED` with `duplicate_message` |
| Quiet hours | SMS/push, non-transactional, 22:00–08:00 recipient local time | Delayed until 08:00 |
| Monthly quota | Tenant | Campaign rejected with `402` |
| Idempotency | `Idempotency-Key` on create | Existing campaign is returned |

## API

All campaign routes require `Authorization: Bearer <tenant-token>`.

| Method | Path | Result |
| --- | --- | --- |
| `GET` | `/api/session` | Demo tenants and local tokens |
| `POST` | `/api/campaigns` | Multipart create; `202` with campaign id and recipient count |
| `GET` | `/api/campaigns` | Paged list: `search`, `status`, `channel`, `page`, `pageSize` |
| `GET` | `/api/campaigns/{id}` | Counts, retry queue, per-minute metrics, errors |
| `POST` | `/api/campaigns/{id}/retry-failures` | Requeue terminal failures; `202` with count |
| `POST` | `/api/provider/{channel}/send` | Process one due job by `notificationId` |
| `GET` | `/api/health` | API health |

Create fields: `tenantId`, `name`, `channel` (`EMAIL`/`SMS`/`PUSH`), `template`, `scheduledAt` (`now` or ISO-8601 with offset), `transactional` (optional), `csv` (file), optional `Idempotency-Key` header.

```bash
curl -X POST http://localhost:8081/api/campaigns \
  -H "Authorization: Bearer demo-northstar-local" \
  -H "Idempotency-Key: statement-2026-09" \
  -F tenantId=northstar \
  -F name="September statement" \
  -F channel=EMAIL \
  -F template="Hello {{recipientId}}" \
  -F scheduledAt=now \
  -F csv=@public/sample-recipients.csv
```

## Tests and load

```bash
cd api && ./mvnw test       # 20 tests: parser, rules, buckets, API, worker, tenancy
cd web && npm run lint && npm run build
cd mobile && npx tsc --noEmit
```

Import benchmark (API must be running; schedules seven days out so the worker does not drain it):

```bash
node scripts/load.mjs 10000
```

The script prints HTTP status, wall-clock seconds, and rows per second for the full multipart import and job creation path.

## Project layout

```
api/        Spring Boot service, Flyway migrations, integration tests
web/        React dashboard, Vite proxy, nginx image
mobile/     Expo app, offline cache, biometric lock, push preview
scripts/    Streamed import benchmark
docs/       System design document and architecture diagram
```

## Design documents

- [ENGINEERING_NOTES.md](ENGINEERING_NOTES.md) — decisions, tradeoffs, failure handling, limitations, AI disclosure.
- [docs/SYSTEM_DESIGN.docx](docs/SYSTEM_DESIGN.docx) — 10M notifications/day scaling, reliability, data strategy, observability, security.

## Scope and honesty

This is a simulated platform: no SMTP, SMS, or push provider is contacted. H2 keeps the demo one command; the design document describes the PostgreSQL and queue topology I would ship. The demo bearer tokens are fixtures, not an authentication system. Everything in this repository was built and verified locally, and the AI-assistance disclosure is in the engineering notes.
