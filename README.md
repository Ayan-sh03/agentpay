# Payment Agent

A Spring Boot application for processing payments with policy enforcement, encryption, and audit capabilities.

## Current Implementation Status

✅ **Core functionality implemented and working** - The payment agent successfully processes purchase requests, enforces policies via OPA, handles database operations with Flyway migrations, and maintains audit trails.

✅ **Database integration complete** - H2 for development, PostgreSQL support for production with automatic migrations.

⏳ **Work in progress** - FIDO2/WebAuthn authentication, advanced rate limiting, and additional policy enhancements are still pending.

## Prerequisites

- Java 17+
- Maven 3.6+
- H2 Database (for development/testing) or PostgreSQL (for production)
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

### Development (H2 In-Memory)
For development and testing, the application uses H2 in-memory database by default. No additional setup required - tables are created automatically via Flyway migrations.

### Production (PostgreSQL)
For production environments, use PostgreSQL:

1. Install Docker Desktop: https://www.docker.com/products/docker-desktop/

2. Run a PostgreSQL container:
   ```bash
   docker run --name payment-agent-db -e POSTGRES_DB=payment_agent -e POSTGRES_USER=payment_user -e POSTGRES_PASSWORD=payment_pass -p 5432:5432 -d postgres:13-alpine
   ```

3. Update `src/main/resources/application.properties`:
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/payment_agent
   spring.datasource.username=payment_user
   spring.datasource.password=payment_pass
   spring.datasource.driver-class-name=org.postgresql.Driver
   ```

### Tables Created
The application automatically creates the following tables via Flyway migrations:
- `audit_log` - Stores transaction audit records
- `token` - Stores encrypted agent credentials

## Database Migrations

This project uses Flyway for database migrations. Migration scripts are located in `src/main/resources/db/migration/`.

### Current Configuration
Flyway is already configured in `application.properties`:
```properties
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.flyway.baseline-on-migrate=true
```

### Migration Scripts
- `V1__create_tables.sql` - Creates audit_log and token tables

Migrations run automatically on application startup. For manual migration:
```bash
mvnw flyway:migrate
```

## Running the Application

### Quick Start (Development)

1. Start Open Policy Agent:
   ```bash
   opa run -s
   ```

2. Start the application:
   ```bash
   mvnw spring-boot:run
   ```

The application will use H2 in-memory database and create tables automatically.

### Production Setup

1. Set up PostgreSQL database (see Database Setup section)
2. Start Open Policy Agent:
   ```bash
   opa run -s
   ```
3. Start the application:
   ```bash
   mvnw spring-boot:run
   ```

### Alternative Start Methods

```bash
# Run packaged JAR
java -jar target/payment-agent-0.0.1-SNAPSHOT.jar

# Build and run
mvnw clean package
java -jar target/payment-agent-0.0.1-SNAPSHOT.jar
```

## Configuration

Key configuration properties in `application.properties`:

```properties
# Database (H2 for development)
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# Flyway
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.flyway.baseline-on-migrate=true

# OPA Server
opa.url=http://localhost:8181

# Encryption
app.encryption.key=test-key-for-encryption
```

### Production Configuration
For production, update the database settings:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/payment_agent
spring.datasource.username=your_db_user
spring.datasource.password=your_db_password
spring.datasource.driver-class-name=org.postgresql.Driver
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