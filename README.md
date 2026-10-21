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
- **Production ready**: Includes monitoring, health checks, and comprehensive error handling
- **Spring Boot 3.x**: Modern Java framework with excellent performance
- **PostgreSQL**: Robust, scalable relational database
- **Micrometer metrics**: Built-in monitoring and observability
- **Swagger/OpenAPI**: Interactive API documentation

## Technology Stack

- **Language**: Java 17
- **Framework**: Spring Boot 3.2.0
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

### Running with Docker

```bash
# Build and start all services
docker-compose up --build

# The application will be available at http://localhost:8080/api
```

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
| `SPRING_MAIL_USERNAME` | Email username for notifications | - |
| `SPRING_MAIL_PASSWORD` | Email password for notifications | - |

## API Endpoints

### Authentication

- `POST /api/auth/login` - Authenticate user and receive JWT token
- `POST /api/auth/register` - Register with a username and password, then receive a JWT token
- `POST /api/auth/logout` - Logout (client-side token disposal)

Set both administrator environment variables to provision the first admin user. Admins can approve or suspend accounts with `PUT /api/admin/accounts/{id}/status`.

### Accounts

- `GET /api/accounts/{id}` - Get account by ID
- `GET /api/accounts/number/{accountNumber}` - Get account by account number
- `GET /api/accounts` - Get all accounts
- `POST /api/accounts` - Create new account
- `PUT /api/accounts/{id}` - Update account
- `DELETE /api/accounts/{id}` - Delete account
- `POST /api/accounts/{id}/deposit` - Deposit funds
- `POST /api/accounts/{id}/withdraw` - Withdraw funds

### Transactions

- `GET /api/transactions/{id}` - Get transaction by ID
- `GET /api/transactions/reference/{referenceId}` - Get transaction by reference ID
- `GET /api/transactions/account/{accountId}` - Get a page of transactions for an account
- `GET /api/transactions/account/{accountId}/range` - Get a page of transactions in a time range
- `POST /api/transactions/deposit` - Create deposit transaction
- `POST /api/transactions/withdraw` - Create withdrawal transaction
- `POST /api/transactions/transfer` - Create transfer transaction

Transaction history endpoints accept `page` (zero-based, default `0`) and `size` (default `20`, maximum `100`) query parameters. Results are ordered newest first and include page metadata such as `totalElements` and `totalPages`.

Example: `GET /api/transactions/account/{accountId}?page=0&size=20`

### Monitoring & Health

- `GET /actuator/health` - Application health status
- `GET /actuator/metrics` - Application metrics
- `GET /actuator/info` - Application information

## API Documentation

Once the application is running, you can access the interactive API documentation at:
- Swagger UI: `http://localhost:8080/api/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/api/v3/api-docs`

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

All API endpoints (except `/api/auth/**` and actuator endpoints) require JWT authentication:

1. Obtain a token by calling `/api/auth/login` with username and password
2. Include the token in the Authorization header: `Bearer <token>`
3. Tokens expire after 1 hour (configurable)

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

## Building for Production

Create a production Docker image:

```bash
docker build -t vaultstream:latest .
```

Or use Docker Compose:

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
