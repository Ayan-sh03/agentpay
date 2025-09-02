# AI Agent Payment Gateway

A production-ready Spring Boot WebFlux application for AI agents to make purchases with JWT authentication, OPA policy enforcement, and comprehensive audit logging.

## Current Implementation Status

✅ **Production-grade JWT authentication** - API key to Bearer token exchange with reactive security context
✅ **OPA policy enforcement** - Real-time policy evaluation with resilient error handling  
✅ **Structured logging** - SLF4J with transaction correlation IDs for production observability
✅ **Database integration** - H2 for development, PostgreSQL for production with Flyway migrations
✅ **Agent-centric design** - Digital goods focus, spending limits, capability-based authorization

**Total codebase**: 3,548 lines of production-ready code

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
   # Windows
   .\opa.exe run --server --addr 127.0.0.1:8181
   
   # Linux/Mac
   opa run --server --addr 127.0.0.1:8181
   ```

2. Start the application:
   ```bash
   ./mvnw spring-boot:run
   ```

The application automatically loads policies into OPA and creates H2 tables via Flyway.

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
opa.url=http://127.0.0.1:8181

# JWT Authentication
app.jwt.secret=change-me-in-production-secret-key-32-chars
app.jwt.expiration=3600

# Encryption
app.encryption.key=change-me-in-production-32-chars
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

### Authentication
- POST `/api/v1/auth/token` - Exchange API key for JWT Bearer token
- GET `/api/v1/auth/validate` - Validate current JWT token

### Purchase Operations  
- POST `/api/v1/purchase` - Initiate a payment (requires Bearer token)
- POST `/api/v1/purchase/{transactionId}/override` - Override a denied payment (requires Bearer token)

### Testing the API

1. **Get JWT Token**:
   ```bash
   curl -X POST http://localhost:8080/api/v1/auth/token \
     -H 'Content-Type: application/json' \
     -d '{"apiKey":"test"}'
   ```

2. **Make Purchase** (copy accessToken from step 1):
   ```bash
   curl -X POST http://localhost:8080/api/v1/purchase \
     -H 'Content-Type: application/json' \
     -H "Authorization: Bearer YOUR_TOKEN" \
     -d '{
       "amount": 50,
       "merchant": "udemy",
       "productType": "course",
       "productId": "python-advanced",
       "currency": "USD"
     }'
   ```

3. **Test Denial** (exceeds limits):
   ```bash
   curl -X POST http://localhost:8080/api/v1/purchase \
     -H 'Content-Type: application/json' \
     -H "Authorization: Bearer YOUR_TOKEN" \
     -d '{
       "amount": 2000,
       "merchant": "physical_store",
       "productType": "hardware",
       "currency": "USD"
     }'
   ```

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