# Relay

Multi-tenant notification and campaign platform: an operations dashboard, a campaign API, and a mobile monitoring app for email, SMS, and push.

Built for the Appfuxion take-home assessment.

## Layout

| Path | What it is |
| --- | --- |
| `api/` | Spring Boot 4 / Java 21 campaign service |
| `web/` | React 19 + TypeScript operations dashboard |
| `mobile/` | Expo React Native monitoring app |

## Run

### API

```bash
cd api
./mvnw spring-boot:run
```

The API listens on `http://localhost:8081/api`. Demo data and the H2 file database are created on first start.

### Dashboard

```bash
cd web
npm install
npm run dev
```

Vite serves `http://localhost:5173` and proxies `/api` to the Spring service.

### Mobile

```bash
cd mobile
npm install
npm start
```

Press `a` for Android, `i` for iOS, or scan the QR code with Expo Go.

## Tests

```bash
cd api && ./mvnw test
cd web && npm run build
```

More detail lives in [ENGINEERING_NOTES.md](ENGINEERING_NOTES.md) and the system design document under `docs/`.
