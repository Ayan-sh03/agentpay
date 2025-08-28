# Payment Agent

A Spring Boot application for processing payments with policy enforcement, encryption, and audit capabilities.

## Current Implementation Status

✅ **Core functionality implemented** - The payment agent can process purchase requests, enforce policies via OPA, store encrypted credentials, and maintain audit trails.

⏳ **Work in progress** - FIDO2/WebAuthn authentication, advanced rate limiting, and comprehensive policy definitions are still pending.

## Prerequisites

- Java 17+
- Maven 3.6+
- PostgreSQL (for audit logs)
- Open Policy Agent (OPA) server

## Installation

```bash
# Clone the repository
git clone <repository-url>
cd payment-agent

# Build the project
mvnw clean package
```

## Database Setup

The easiest way to set up a minimal PostgreSQL database for testing is using Docker:

1. Install Docker Desktop: https://www.docker.com/products/docker-desktop/

2. Run a PostgreSQL container:
   ```bash
   docker run --name payment-agent-db -e POSTGRES_DB=payment_agent -e POSTGRES_USER=payment_user -e POSTGRES_PASSWORD=payment_pass -p 5432:5432 -d postgres:13-alpine
   ```

3. Configure database connection in `src/main/resources/application.properties`:
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/payment_agent
   spring.datasource.username=payment_user
   spring.datasource.password=payment_pass
   ```

4. The application will automatically create the following tables on startup:
   - `audit_log` - Stores transaction audit records
   - `token` - Stores encrypted agent credentials

   To disable automatic schema creation, add:
   ```properties
   spring.jpa.hibernate.ddl-auto=validate
   ```

For more detailed instructions, see [MINIMAL_POSTGRES_SETUP.md](MINIMAL_POSTGRES_SETUP.md).

## Database Migrations

This project includes sample Flyway migration scripts in `src/main/resources/db/migration/`.

For production environments, it's recommended to use a database migration tool like Flyway or Liquibase.

To enable Flyway migrations:
1. Add the following to `application.properties`:
   ```properties
   spring.jpa.hibernate.ddl-auto=validate
   ```
2. Run migrations with:
   ```bash
   mvnw flyway:migrate
   ```

## Running the Application

### Required Services

1. Start PostgreSQL database
2. Start Open Policy Agent:
   ```bash
   opa run -s
   ```

### Start the Application

```bash
# Run with Maven
mvnw spring-boot:run

# Or run the packaged JAR
java -jar target/payment-agent-0.0.1-SNAPSHOT.jar
```

## Configuration

Key configuration properties in `application.properties`:

```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/payment_agent
spring.datasource.username=payment_user
spring.datasource.password=payment_pass

# OPA Server
opa.url=http://localhost:8181

# Encryption
app.encryption.key=your-encryption-key-here
```

## API Endpoints

- POST `/api/v1/purchase` - Initiate a payment
- POST `/api/v1/purchase/{transactionId}/override` - Override a denied payment

## Testing

Run tests with:
```bash
mvnw test
```

## Building

```bash
mvnw clean package
```

## Implementation Progress

See [IMPLEMENTATION_SUMMARY.md](IMPLEMENTATION_SUMMARY.md) for details on what's been implemented and what's still pending.