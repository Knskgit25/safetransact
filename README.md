# SafeTransact — Idempotent Payment Processor

SafeTransact is a backend service that simulates exactly-once payment processing,
the same reliability guarantee real-world payment gateways (Stripe, Razorpay, UPI)
rely on to prevent users from being charged twice due to network retries, duplicate
clicks, or concurrent requests.

## The Problem

In distributed systems, a client may retry a payment request if it doesn't receive
a response in time — even if the original request actually succeeded on the server.
Without protection, this naive retry can result in the same payment being processed
twice. SafeTransact solves this using the **idempotency key pattern**, combined with
database-level concurrency controls to guarantee correctness even under race conditions.

## How It Works

**1. Idempotency Keys**
Every payment request must include a unique `Idempotency-Key` header. Before processing
any payment, the system checks whether this key has been seen before:
- If yes, the existing payment result is returned — no duplicate processing occurs.
- If no, a new idempotency key record is inserted, and the payment is processed.

**2. Database-Level Race Condition Protection**
A unique constraint on the idempotency key column ensures that even if two requests
with the same key arrive at the exact same moment (e.g. a double-click or a network
retry firing concurrently), only one insert can succeed. The database itself rejects
the second insert with a `DataIntegrityViolationException`, which the service layer
catches and handles gracefully — preventing duplicate payments without relying on
application-level locks alone.

**3. Optimistic Locking**
Each `Payment` record has a `@Version` field. Whenever a payment is updated, this
version number increments. If two concurrent operations try to update the same
payment, Spring Data JPA detects the version mismatch and rejects the stale update,
preventing lost updates — a classic race condition in concurrent systems.

## Tech Stack

- Java 17
- Spring Boot 3 (Web, Data JPA)
- H2 Database (in-memory, for fast local development)
- Lombok

## API

**POST** `/api/payments`

Headers:

Body:
```json
{
  "amount": 500.00,
  "currency": "INR",
  "payerAccount": "acc123",
  "payeeAccount": "acc456"
}
```

Sending the same request twice with the same `Idempotency-Key` returns the same
payment record both times — no duplicate is created.

## Running Locally

```bash
./mvnw spring-boot:run
```

The application starts on `http://localhost:8080`.

## What This Project Demonstrates

This project was built to explore core backend reliability concepts that are
critical in payment systems: idempotency, optimistic concurrency control, and
database-level constraint enforcement as a defense against race conditions —
concepts that go beyond basic CRUD APIs.