# SafeTransact

A production-style idempotent payment processing engine built with Spring Boot, designed to guarantee **exactly-once payment execution** under concurrent duplicate requests.

## Problem It Solves

Payment APIs are frequently called twice — a client retries after a timeout, a mobile app double-taps "Pay", or a load balancer replays a request. Without protection, this can cause **duplicate money transfers**. SafeTransact solves this using the **Idempotency-Key pattern**, combined with database-level race protection and payload-integrity checks.

## Key Features

- **Exactly-once payment processing** — the same `Idempotency-Key` always returns the same result, even under 50+ concurrent duplicate requests
- **Atomic balance transfer** — sender debit and receiver credit happen inside a single `@Transactional` boundary
- **Race-condition safe** — a unique DB constraint on the idempotency key catches simultaneous duplicate requests that arrive within milliseconds of each other, before any business logic runs
- **Payload-integrity validation** — if the same idempotency key is reused with a *different* payment payload (e.g. a different amount), the request is rejected instead of silently processed
- **Insufficient balance handling** — failed payments are recorded (not silently dropped), with no partial fund deduction
- **Clean DTO boundary** — request/response DTOs with Jakarta Bean Validation (`@NotNull`, `@Positive`, `@Email`, etc.); controllers never expose raw JPA entities

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot |
| Persistence | Spring Data JPA + H2 (dev) |
| Validation | Jakarta Bean Validation |
| Testing | JUnit 5, `ExecutorService` + `CountDownLatch` for concurrency simulation |
| Build | Maven |

> Docker and Redis were deliberately scoped out for this project — the focus is on correctness of the core payment engine and concurrency guarantees rather than infrastructure, to keep the scope tight and resume-relevant.

## Architecture

```
Client
  │
  ▼
Controller (validates DTO, maps to entity)
  │
  ▼
Service (business logic, @Transactional)
  │
  ├── IdempotencyKeyRepository (unique constraint on key)
  ├── PaymentRepository
  └── UserRepository
  │
  ▼
Database (H2)
```

Payment flow uses a **Controller → Service → Repository** layering. User flow is intentionally simpler (**Controller → Repository**) since user creation has no business logic beyond validation.

## How Idempotency Is Enforced

1. **Request hash** — every payment request is hashed (SHA-256 over amount, currency, payer, payee). The amount is normalized (`stripTrailingZeros().toPlainString()`) before hashing, so `500` and `500.00` are treated as identical — preventing false "payload mismatch" errors from formatting differences alone.
2. **Key reuse, same payload** → returns the original result (true idempotency).
3. **Key reuse, different payload** → rejected with a clear error (protects against key misuse).
4. **Simultaneous duplicate requests** (race condition) → a unique DB constraint on the idempotency key means only one request can insert the key record; every other concurrent request fails fast on `DataIntegrityViolationException` instead of both proceeding to debit the sender.

## Tested Guarantees

A concurrency test fires **50 simultaneous requests** with the same idempotency key at the payment endpoint:

- **1** request succeeds and creates a payment record
- **49** are rejected
- Sender balance is debited **exactly once** — verified against the expected post-transfer balance

Additional unit tests confirm the request-hash function treats equivalent amounts (`500` vs `500.00`) identically, while genuinely different amounts (`500` vs `999`) produce different hashes — so idempotency-key reuse detection is both accurate and scale-safe.

## API Endpoints

### Create User
```
POST /api/users
Content-Type: application/json

{
  "name": "Alice",
  "email": "alice@example.com",
  "balance": 1000.00
}
```

### Get User
```
GET /api/users/{id}
```

### Create Payment
```
POST /api/payments
Content-Type: application/json
Idempotency-Key: <unique-client-generated-key>

{
  "amount": 500.00,
  "currency": "INR",
  "payerAccount": "alice@example.com",
  "payeeAccount": "bob@example.com"
}
```

Returns `201 Created` with the payment record. Reusing the same `Idempotency-Key` with the same payload returns the same payment (no duplicate transfer). Reusing it with a different payload returns an error.

## Running Locally

```bash
git clone https://github.com/Knskgit25/safetransact.git
cd safetransact
mvn spring-boot:run
```

The app starts on `http://localhost:8080` with an in-memory H2 database (data resets on restart).

## Running Tests

```bash
mvn test
```

Includes the concurrency test (50 parallel duplicate requests) and payload-hash unit tests.

## What This Project Demonstrates

- Designing for **idempotency** in distributed/retry-prone systems
- Handling **race conditions** at the database level, not just in application code
- Writing **concurrency tests** that actually prove correctness under load, not just happy-path unit tests
- Clean separation between API contracts (DTOs) and persistence models (entities)