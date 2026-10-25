# VaultStream - Real-Time Event-Driven Ledger System

VaultStream is a microservices-based banking backend implementing double-entry accounting with ACID-compliant transactions, ensuring zero financial discrepancy under high-concurrency fund transfers.

## Features

- **Double-entry accounting**: Every transaction affects at least two accounts to maintain balance
- **ACID-compliant transactions**: Ensures data integrity even under high concurrency
- **Idempotency keys**: Prevents duplicate charges from network retries
- **Real-time event processing**: Handles high-volume transaction streams efficiently
- **RESTful API**: Clean, well-documented endpoints for account and transaction management
- **JWT authentication**: Secure token-based authentication for API access
- **Docker support**: Easy containerization and deployment
- **Operational safeguards**: Health monitoring, hardened containers, backup tooling, and automated security checks
- **Spring Boot 3.x**: Modern Java framework with excellent performance
- **PostgreSQL**: Robust, scalable relational database
- **Micrometer metrics**: Built-in monitoring and observability
- **Swagger/OpenAPI**: Interactive API documentation

## Technology Stack

- **Language**: Java 17
- **Framework**: Spring Boot 3.5.16
- **Database**: PostgreSQL 15
- **Build Tool**: Maven
- **Containerization**: Docker & Docker Compose
- **Security**: JWT (JSON Web Tokens)
- **Validation**: Jakarta Validation
- **Monitoring**: Spring Boot Actuator, Micrometer
- **API Documentation**: Springdoc OpenAPI (Swagger UI)
- **Lombok**: Reduced boilerplate code
- **MapStruct**: Entity-DTO mapping

## Project Structure

```
vaultstream/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/vaultstream/
│   │   │       ├── controller/        # REST controllers
│   │   │       ├── dto/               # Data Transfer Objects
│   │   │       ├── exception/         # Custom exceptions
│   │   │       ├── idempotency/       # Idempotency handling
│   │   │       ├── mapper/            # MapStruct mappers
│   │   │       ├── model/             # JPA entities
│   │   │       ├── monitoring/        # Health checks & metrics
│   │   │       ├── repository/        # Spring Data repositories
│   │   │       ├── security/          # JWT authentication & authorization
│   │   │       └── service/           # Business logic services
│   │   └── resources/
│   │       ├── application.yml        # Configuration
│   │       └── init-db.sql            # Database initialization
├── Dockerfile                         # Docker build file
├── docker-compose.yml                 # Docker Compose configuration
├── pom.xml                            # Maven dependencies
└README.md                            # This file
```

## Getting Started

For the current launch decision and the remaining production gates, see the [production readiness checklist](PRODUCTION_READINESS.md).

### Prerequisites

- Java 17 JDK
- Maven 3.8+
- Docker & Docker Compose (for containerized deployment)
- PostgreSQL 15 (if not using Docker)

### Running Locally

1. Clone the repository
2. Configure your PostgreSQL database (update `application.yml` or set environment variables)
3. Build and run the application:

```bash
# Build the project
mvn clean package

# Run the application
mvn spring-boot:run
```

The application will start on `http://localhost:8080/api`

### Running locally with Docker

```bash
# Build and start all services
docker-compose up --build

# The application will be available at http://localhost:8080/api
```

### Production deployment

The production stack is separate from the local development stack. It runs the app with the `prod` profile, requires credentials and mounted secret files, persists PostgreSQL data in a named volume, and binds the app to localhost by default. Put a host-based HTTPS reverse proxy in front of it before exposing the service publicly.

Create secret files outside version control, then create a `.env` file:

```dotenv
DB_NAME=vaultstream
DB_USERNAME=vaultstream_user
VAULTSTREAM_ADMIN_USERNAME=admin
VAULTSTREAM_ALLOWED_ORIGINS=https://app.example.com
VAULTSTREAM_REGISTRATION_ENABLED=false
DB_PASSWORD_FILE=./secrets/db_password
JWT_SECRET_FILE=./secrets/jwt_secret
VAULTSTREAM_ADMIN_PASSWORD_FILE=./secrets/admin_password
# Optional: change the local bind address/port when configuring a reverse proxy.
APP_BIND_ADDRESS=127.0.0.1
APP_PORT=8081
```

Create the files with unique values (the admin password must be 12-72 characters):

```bash
mkdir -p secrets
python3 -c "import secrets,sys; sys.stdout.write(secrets.token_urlsafe(48))" > secrets/db_password
python3 -c "import secrets,sys; sys.stdout.write(secrets.token_urlsafe(64))" > secrets/jwt_secret
python3 -c "import secrets,sys; sys.stdout.write(secrets.token_urlsafe(32))" > secrets/admin_password
chmod 600 secrets/*
```

The secret files are mounted read-only into the containers and loaded by Spring's config tree support. Keep `.env` and the `secrets/` directory out of source control and protect them on the deployment host. Then start the stack:

```bash
docker compose --env-file .env -f docker-compose.prod.yml up -d --build
```

The app starts only after PostgreSQL reports healthy. Flyway applies schema migrations on startup. PostgreSQL is not published to the host, and the app container runs as a non-root user with a read-only filesystem. Back up the PostgreSQL volume using a tested database backup and restore process before handling real data.

#### Database backups

Install the `age` encryption utility on the host and create an age key pair. Keep the private identity outside the application host and outside source control. Then create a consistent, encrypted custom-format database dump:

```bash
VAULTSTREAM_BACKUP_AGE_RECIPIENT=age1... bash scripts/backup-postgres.sh /path/to/secure/backup-storage
```

The script writes encrypted `.dump.age` files and removes incomplete output if the dump or encryption step fails. Configure your host scheduler to run it, copy completed files to access-controlled storage outside the application host, and enforce a retention policy there. The script does not schedule itself or upload backups. Before go-live, decrypt and restore a backup into a separate PostgreSQL instance and verify the application can read the restored data; record the restore result and recovery time.

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `DB_HOST` | Database host | `vaultstream-db` |
| `DB_PORT` | Database port | `5432` |
| `DB_NAME` | Database name | `vaultstream` |
| `DB_USERNAME` | Database username | `vaultstream_user` |
| `DB_PASSWORD` | Database password | `vaultstream_pass` |
| `JWT_SECRET` | Secret key for JWT signing | Local-development fallback; set a secret in deployments |
| `VAULTSTREAM_ADMIN_USERNAME` | Optional username for the first administrator | - |
| `VAULTSTREAM_ADMIN_PASSWORD` | Optional administrator password (12-72 characters) | - |
Production deployments must set `DB_NAME`, `DB_USERNAME`, `VAULTSTREAM_ADMIN_USERNAME`, `VAULTSTREAM_ALLOWED_ORIGINS`, and the three secret file paths in the `.env` file. Set `VAULTSTREAM_ALLOWED_ORIGINS` to the exact HTTPS origin(s) of trusted browser clients, comma-separated when there are multiple. Self-registration is disabled by default in the production profile; enable it only after an identity-verification and customer-onboarding process is in place. Account requests still require administrator approval. Do not use a wildcard or the local development defaults in a deployed environment.

## API Endpoints

### Authentication

- `POST /api/auth/login` - Authenticate user and receive JWT token
- `POST /api/auth/register` - Register with a username and password, then receive a JWT token (disabled by default in production)
- `POST /api/auth/logout` - Revoke the current bearer token

Set both administrator environment variables to provision the first admin user. Admins can approve or suspend accounts with `PUT /api/admin/accounts/{id}/status`.

### Accounts

- `GET /api/accounts/{id}` - Get account by ID
- `GET /api/accounts/number/{accountNumber}` - Get account by account number
- `GET /api/accounts` - Get a page of the authenticated user's accounts
- `POST /api/accounts` - Create new account
- `PUT /api/accounts/{id}` - Update account
- `DELETE /api/accounts/{id}` - Delete account
- `POST /api/accounts/{id}/deposit` - Admin-only manual deposit entry
- `POST /api/accounts/{id}/withdraw` - Admin-only manual withdrawal entry

Account and transaction history endpoints accept `page` (zero-based, default `0`) and `size` (default `20`, maximum `100`) query parameters. Results include page metadata such as `totalElements` and `totalPages`.

### Transactions

- `GET /api/transactions/{id}` - Get transaction by ID
- `GET /api/transactions/reference/{referenceId}` - Get transaction by reference ID
- `GET /api/transactions/account/{accountId}` - Get a page of transactions for an account
- `GET /api/transactions/account/{accountId}/range` - Get a page of transactions in a time range
- `POST /api/transactions/deposit` - Admin-only manual deposit transaction
- `POST /api/transactions/withdraw` - Admin-only manual withdrawal transaction
- `POST /api/transactions/transfer` - Create transfer transaction

Example transaction history request: `GET /api/transactions/account/{accountId}?page=0&size=20`

### Monitoring & Health

- `GET /actuator/health` - Public application health status (details require an admin token)
- `GET /actuator/metrics` - Application metrics (admin token required)
- `GET /actuator/info` - Application information (admin token required)

The Prometheus scrape endpoint requires an administrator JWT. Configure the scraper to send `Authorization: Bearer <token>` and keep the endpoint behind a trusted network or reverse proxy when deploying publicly. The log-file endpoint also requires an administrator JWT.

## API Documentation

Once the application is running, you can access the interactive API documentation at:
- Swagger UI: `http://localhost:8080/api/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/api/v3/api-docs`

These endpoints are for local/development use and are disabled in the production profile.

## Idempotency

VaultStream supports idempotency keys to prevent duplicate processing of requests. To use idempotency:

1. Include the `Idempotency-Key` header in your POST, PUT, or PATCH requests
2. The key should be unique for each request (UUID recommended)
3. The same key with the same method and path will return the cached response
4. Idempotency keys expire after 24 hours and expired records are cleaned up hourly

Example:
```http
POST /api/transactions/deposit
Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000
Content-Type: application/json

{
  "accountId": "123e4567-e89b-12d3-a456-426614174000",
  "amount": "100.00",
  "referenceId": "dep_001",
  "description": "Initial deposit"
}
```

## Security

Login, API documentation, and basic health status are public. Registration is public only when enabled. Logout and other API endpoints require JWT authentication. Manual deposits and withdrawals and all actuator endpoints other than basic health require an administrator JWT. Deposits and withdrawals are administrative ledger adjustments; connect a verified payment or settlement provider before using them to represent real money movement:

1. Obtain a token by calling `/api/auth/login` with username and password
2. Include the token in the Authorization header: `Bearer <token>`
3. Tokens expire after 1 hour (configurable)
4. Call `/api/auth/logout` with the bearer token to revoke it immediately

## Monitoring & Observability

VaultStream includes comprehensive monitoring capabilities:

- **Health Checks**: `/actuator/health` includes ledger-specific health indicators
- **Metrics**: Micrometer metrics for accounts and transactions
- **Logging**: Structured logging with SLF4J
- **Distributed Tracing**: Ready for integration with Zipkin/Jäger

## Testing

Run the test suite:

```bash
mvn test
```

## Building an image manually

Create a production Docker image:

```bash
docker build -t vaultstream:latest .
```

For production, use the production Compose stack above. The default Compose file is for local development:

```bash
docker-compose build
docker-compose up -d
```

## Configuration

The application can be configured through:
- `src/main/resources/application.yml` (default configuration)
- Environment variables (override YAML values)
- Command line arguments (override both)

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- Built with Spring Boot ecosystem
- Inspired by banking ledger best practices
- Designed for high-availability financial systems
