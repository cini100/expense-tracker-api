# Personal Expense Tracker API

Spring Boot backend API for tracking personal income and expenses.

## Stack
- Java 21
- Spring Boot 3
- PostgreSQL + Spring Data JPA
- Flyway migrations
- JUnit + Testcontainers
- Docker-ready configuration

## Project Structure

```text
expense-tracker-api
├── build.gradle.kts
├── settings.gradle.kts
├── docs/openapi-v1.yaml
├── src/main/java/com/example/expensetracker
│   ├── ExpenseTrackerApiApplication.java
│   ├── auth
│   │   ├── controller/AuthController.java
│   │   └── dto/{RegisterRequest,LoginRequest,AuthResponse}.java
│   ├── category
│   │   ├── controller/CategoryController.java
│   │   ├── dto/{CreateCategoryRequest,UpdateCategoryRequest,CategoryResponse}.java
│   │   ├── entity/Category.java
│   │   └── repository/CategoryRepository.java
│   ├── transaction
│   │   ├── controller/TransactionController.java
│   │   ├── dto/{CreateTransactionRequest,UpdateTransactionRequest,TransactionResponse}.java
│   │   ├── entity/TransactionRecord.java
│   │   └── repository/TransactionRepository.java
│   ├── summary
│   │   ├── controller/SummaryController.java
│   │   └── dto/{CategoryTotalItem,MonthlySummaryResponse}.java
│   ├── user
│   │   ├── entity/User.java
│   │   └── repository/UserRepository.java
│   ├── common
│   │   ├── error/{ApiErrorResponse,FieldValidationError,GlobalExceptionHandler}.java
│   │   ├── model/EntryType.java
│   │   └── web/PageResponse.java
│   └── config/SecurityConfig.java
└── src/main/resources
    ├── application.yml
    └── db/migration/V1__init_schema.sql
```

## Implemented Pieces
- Data model entities: `User`, `Category`, `TransactionRecord`
- DTO contracts for auth/categories/transactions/summaries
- JWT auth with register/login and protected API routes
- Category CRUD with per-user ownership checks
- Transaction CRUD with pagination and filtering
- Monthly summaries with income, expense, net, count, and expense-category breakdown
- Validation annotations for request payloads
- Global exception response shape and handler
- Initial DB schema migration + indexes
- OpenAPI contract in `docs/openapi-v1.yaml`

## Run Locally

1. Start PostgreSQL (or Docker Postgres) and create database `expense_tracker`.
2. Set env vars as needed:
   - `SPRING_DATASOURCE_URL`
   - `SPRING_DATASOURCE_USERNAME`
   - `SPRING_DATASOURCE_PASSWORD`
   - `JWT_SECRET`
3. Run app:

```bash
./gradlew bootRun
```

## Test

```bash
./gradlew test
```

The test suite includes unit, MVC/security, and PostgreSQL-backed Testcontainers integration tests.

## Current Status
Core auth, category, transaction, and summary endpoints are implemented. Run `./gradlew test` for the current test suite.

## Docker Run
1. Copy env template:

```bash
cp .env.example .env
```

Generate a new Base64 JWT secret for local use:

```bash
JWT_SECRET=$(openssl rand -base64 32)
```

Replace the `JWT_SECRET` value in `.env` with that generated value.

2. Build and start:

```bash
docker compose up --build
```

3. Stop:

```bash
docker compose down
```

Run the Docker smoke test after Docker Desktop is running:

```bash
./scripts/smoke-test.sh
```

The smoke test requires `curl` and `jq`; it builds the app, starts Postgres, checks health, registers/logs in, creates a category and transaction, lists transactions, and reads the monthly summary.

## API Quickstart

Register:

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password123"}'
```

Login and store token:

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password123"}' \
  | jq -r '.accessToken')
```

Create an expense category:

```bash
CATEGORY_ID=$(curl -s -X POST http://localhost:8080/api/v1/categories \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Food","type":"EXPENSE"}' \
  | jq -r '.id')
```

Create a transaction:

```bash
curl -s -X POST http://localhost:8080/api/v1/transactions \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"categoryId\": $CATEGORY_ID,
    \"type\": \"EXPENSE\",
    \"amount\": 12.50,
    \"description\": \"Lunch\",
    \"transactionDate\": \"2024-04-10\"
  }"
```

List filtered transactions:

```bash
curl -s "http://localhost:8080/api/v1/transactions?type=EXPENSE&from=2024-04-01&to=2024-04-30&page=0&size=20" \
  -H "Authorization: Bearer $TOKEN"
```

Get monthly summary:

```bash
curl -s "http://localhost:8080/api/v1/summaries/monthly?year=2024&month=4" \
  -H "Authorization: Bearer $TOKEN"
```

Expected auth response shape:

```json
{
  "accessToken": "jwt-token",
  "tokenType": "Bearer",
  "expiresInSeconds": 3600
}
```

Expected error response shape:

```json
{
  "timestamp": "2026-04-21T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Request validation failed",
  "path": "/api/v1/categories",
  "fieldErrors": []
}
```
