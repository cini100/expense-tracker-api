# Personal Expense Tracker API (Starter Blueprint)

Spring Boot starter skeleton for a portfolio-grade expense tracker API.

## Stack
- Java 21
- Spring Boot 3
- PostgreSQL + Spring Data JPA
- Flyway migrations
- JUnit + Testcontainers
- Docker-ready configuration

## Starter Structure

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

## Implemented Blueprint Pieces
- Data model entities: `User`, `Category`, `TransactionRecord`
- DTO contracts for auth/categories/transactions/summaries
- Endpoint routing skeletons (all v1 routes)
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

## Next Implementation Order
1. JWT provider + authentication service (`register/login`).
2. Category service + mapper + ownership checks.
3. Transaction service with filtering/specifications and pagination response mapping.
4. Monthly summary query service.
5. Integration tests with Testcontainers.
6. CI pipeline (test + lint + image build).

## Current Status
Controllers are intentionally scaffold-only and return `501 Not Implemented` so that API shape is fixed before business logic is added.

## Docker Run
1. Copy env template:

```bash
cp .env.example .env
```

2. Build and start:

```bash
docker compose up --build
```

3. Stop:

```bash
docker compose down
```
