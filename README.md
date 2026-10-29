<div align="center">

# VaultStream

### A ledger and payments API engineering portfolio project

[![CI](https://github.com/Priyadharsan-85/VaultStream/actions/workflows/ci.yml/badge.svg)](https://github.com/Priyadharsan-85/VaultStream/actions/workflows/ci.yml)
![Java 17](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-4169E1?logo=postgresql&logoColor=white)
![Demo only](https://img.shields.io/badge/use-synthetic%20demo%20only-blue)

**VaultStream is a Java backend portfolio project exploring ledger integrity, API security, and operational safeguards.**

[Explore the design](#architecture) · [Run locally](#run-locally) · [Try the API](#api-overview) · [Deploy a demo](RENDER_DEMO.md) · [Production status](PRODUCTION_READINESS.md)

</div>

---

> [!CAUTION]
> **Educational demo only. VaultStream is not a bank, wallet, payment processor, or production-ready financial service. Never connect real customer funds or personal data.** Customer transfers, manual balance adjustments, and registration remain disabled in the hosted demo configuration.

## Project at a glance

| | |
|---|---|
| **What it demonstrates** | Double-entry ledger patterns, transactional APIs, JWT authentication, idempotency, reconciliation, and monitoring |
| **Application type** | Backend REST API with a read-only portfolio landing page; no customer wallet interface |
| **Demo data** | Use synthetic accounts and transactions only |
| **Stack** | Java 17 · Spring Boot · PostgreSQL · Flyway · Docker |
| **Quality checks** | Maven verification, container build and image scan, CodeQL, dependency review |

## Architecture

```mermaid
flowchart LR
    Client[API client] --> API[Spring Boot API]
    API --> Auth[JWT authentication]
    API --> Services[Account and transaction services]
    Services --> Ledger[Double-entry ledger]
    Services --> Idempotency[Idempotency records]
    Ledger --> DB[(PostgreSQL)]
    Idempotency --> DB
    Migrate[Flyway migrations] --> DB
    API --> Monitoring[Health and metrics]
```

### A transfer request

```mermaid
sequenceDiagram
    actor Client
    participant API as VaultStream API
    participant Auth as JWT security
    participant Service as Transaction service
    participant DB as PostgreSQL
    Client->>API: POST /api/transactions/transfer
    API->>Auth: Validate bearer token and access
    Auth-->>API: Authorized
    API->>Service: Validate request and idempotency key
    Service->>DB: Write balanced ledger entries in one transaction
    DB-->>Service: Commit or reject
    Service-->>API: Transaction result
    API-->>Client: Response
```

## Capabilities

- Double-entry journal entries with reconciliation checks
- JWT authentication, role-based administration, and logout token revocation
- Request idempotency and duplicate reference protection
- Account statements and paginated transaction history
- Login throttling, transfer limits, and velocity checks
- Flyway migrations, health endpoints, metrics, and containerized local services
- Production safeguards that disable risky ledger operations by default

These are software demonstrations, not evidence of financial compliance, high-volume capacity, or suitability for real funds.

## Run locally

### With Docker Compose

```bash
docker compose up --build
```

| Service | Local address | Purpose |
|---|---|---|
| API | `http://localhost:8082` | REST API |
| Swagger UI | `http://localhost:8082/swagger-ui/index.html` | Explore the development API |
| OpenAPI JSON | `http://localhost:8082/v3/api-docs` | API schema |
| Prometheus | `http://localhost:9090` | Local metrics collection |
| Grafana | `http://localhost:3000` | Local dashboards |

Local Compose credentials and sample settings are for development only. Never reuse them in a public deployment. Stop the services with `docker compose down`.

### Without Docker

Install JDK 17, Maven, and PostgreSQL, set the database and JWT environment values, then run:

```bash
mvn spring-boot:run
```

## API overview

Authentication is available at `/api/auth/login`. Account and transaction routes require a bearer token. Administrative routes require an administrator token.

| Area | Example routes |
|---|---|
| Public demo | `GET /api/demo/overview` (read-only, synthetic data; available in the Render demo profile) |
| Authentication | `POST /api/auth/login`, `POST /api/auth/register`, `POST /api/auth/logout` |
| Accounts | `GET /api/accounts`, `GET /api/accounts/{id}`, `GET /api/accounts/{id}/statement`, `POST /api/accounts` |
| Transactions | `GET /api/transactions/account/{accountId}`, `POST /api/transactions/transfer` |
| Administration | `GET /api/admin/ledger/reconcile/latest`, `POST /api/admin/ledger/reconcile`, account management routes |
| Health | `GET /actuator/health`, `GET /actuator/health/liveness` |

Visitors can open the public sample endpoint to inspect two synthetic accounts and their sample transaction history without signing in. It is read-only and only included in the Render demo profile. Registration, transfers, and manual ledger adjustments are disabled in the hosted demo. API documentation is enabled for local development and disabled by the production profile. See the OpenAPI page when running locally for request and response schemas.

## Deploy a portfolio demo

The repository includes a Render Blueprint for a small public demo backed by a separate PostgreSQL database. It is intended for synthetic data only and uses free-tier services that may sleep while idle. Follow [RENDER_DEMO.md](RENDER_DEMO.md) for setup and verification steps.

Do not put database credentials or signing keys in GitHub, screenshots, or chat. The demo is not intended for personal data or customer funds.

## Production and customer funds

**VaultStream is not ready to accept customer funds.** A portfolio deployment is not a real-money launch. Before any financial use, the business model and jurisdiction need legal and regulatory review; required authorization and an eligible financial partner must be secured; and security, onboarding, settlement, reconciliation, support, and incident-response controls must be independently reviewed.

See [PRODUCTION_READINESS.md](PRODUCTION_READINESS.md) for the current status and open gates. The production Compose configuration is an engineering example, not a certification.

## Repository map

```text
src/main/java/       REST API, services, ledger, security, monitoring
src/main/resources/  Runtime profiles and Flyway migrations
src/test/            Automated tests
.github/workflows/   Build, test, and security checks
render.yaml          Render demo service blueprint
RENDER_DEMO.md       Demo database and hosting guide
PRODUCTION_READINESS.md  Financial production readiness status
```

## Contributing and verification

Build and run the automated checks with:

```bash
mvn verify
```

GitHub Actions also builds the Docker image and runs configured security checks. A passing build does not certify the project for real-world financial use.

## License

MIT. See [LICENSE](LICENSE).
