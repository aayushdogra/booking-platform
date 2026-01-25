# Booking Platform (Backend)

A production-style, Agoda-inspired booking backend built with **Java 21 + Spring Boot**.

This project is designed as an **incrementally evolved backend system**, focusing on correctness first
and introducing complexity only when justified. Each phase mirrors how real booking systems are
built and hardened in production environments.

---

## Project Focus

This codebase focuses on **engineering trade-offs**, not feature count.

### Primary goals:

- Strong domain boundaries
- Correct lifecycle management
- Inventory safety under concurrency
- Idempotent write paths
- Deterministic failure semantics
- Controlled sync → async evolution
- Safe retry & DLQ boundaries

---

## High-Level Architecture

```txt 
Controller  →  Service (Orchestration) →  Repository  →  Database
                            ↓            
                       Domain Rules
                            ↓
                    In-Process Consumers
```

## Architectural Intent

- Controllers are HTTP adapters only
- Services orchestrate workflows and transactions
- Domain entities enforce lifecycle invariants
- Repositories are persistence-only
- No cross-domain mutation
- State transitions are explicit and validated
- Events represent facts, not commands
- Domain state is the primary idempotency guard

---

## Core Concepts Implemented

- Explicit booking lifecycle (`CREATED → CONFIRMED | CANCELLED | EXPIRED`)
- Availability as a first-class domain
- Idempotent booking creation and cancellation
- Time-bound booking holds with expiry
- Optimistic locking with bounded retries
- Payment idempotency
- Domain events for payment flow
- In-process async boundary simulation
- Controlled retries at event consumers
- Dead Letter Queue (in-memory design)

---

## Tech Stack

- Java 21
- Spring Boot 3.3.2
- Spring Web
- Spring Data JPA / Hibernate
- PostgreSQL
- Jakarta Validation
- Maven

---

## Module Structure

```txt
com.booking.platform
├── controller        // HTTP layer
├── service           // Orchestration & transactions
├── domain            // Enums and domain concepts
├── entity            // Persistence models with invariants
├── repository        // Data access
├── event             // Domain events & publisher
│   ├── consumer      // Async boundary (in-process)
│   └── dlq           // Dead-letter abstractions
├── model             // API DTOs
├── exception         // Error handling
└── config            // Dev-only setup
```

---

## Booking Lifecycle

Bookings are stateful and lifecycle-driven.

### Supported states:

```text
CREATED → CONFIRMED
CREATED → CANCELLED
CREATED → EXPIRED
```

### Rules:

- Only valid transitions are allowed
- Terminal states cannot be exited
- Lifecycle enforcement lives inside BookingEntity
- Expiry is system-driven
- Cancellation is user-driven and idempotent

---

## Availability Model

Availability is modeled explicitly as:

`(hotel, roomType, date)`


### Characteristics:

- Quantity-based inventory
- Optimistic locking via @Version
- No derived availability
- No negative inventory
- Reservation and release are symmetric operations

---

## Idempotency Strategy

Idempotency is enforced at multiple layers:

- Booking creation via `idempotencyKey` (DB constraint + lookup)
- Cancellation via booking state checks
- Payments via one-payment-per-booking invariant
- Event consumption via booking state

Domain state is the **primary idempotency guard**, not infrastructure.

---

## Payments & Async Boundary

Payments now cross an explicit async boundary.

```text
POST /confirm
    ↓
PaymentRequestedEvent
    ↓
PaymentRequestConsumer
    ↓
PaymentService
    ↓
PaymentSucceeded / PaymentFailed
    ↓
PaymentEventConsumer (retry + DLQ)
    ↓
Booking confirmation
```

### Characteristics:

- Event delivery is synchronous (single JVM)
- No broker yet
- Consumer logic is isolated
- Booking confirmation is idempotent
- Async boundary is architecturally defined
- This establishes a safe foundation for later async infrastructure.

---

## Retry & DLQ Semantics

Retries are controlled and scoped.

### Retry Rules
Retryable:
- Optimistic locking failures
- Transient DB issues

Non-retryable:
- Invalid booking state
- Business rule violations

### Implementation
- Bounded retries (max 3)
- Linear backoff
- Retry classification
- Consumer-level retry only

Retry logic does NOT live in domain services.

---

## Dead Letter Queue (Design Phase)

DLQ is introduced conceptually and in-memory.

DLQ is triggered when:
- Non-retryable exception occurs
- Retry exhaustion happens

Stored metadata:
- Original event
- Retry count
- Failure type
- Error message
- Timestamp

DLQ is for processing failures, not business outcomes.

Example:
- `PaymentFailedEvent` is NOT DLQ.
- Consumer crash after `PaymentSucceededEvent` IS DLQ candidate.

Persistence-backed DLQ is intentionally deferred.

---

## Development Notes

- Availability is pre-seeded under the dev profile
- Read paths are side-effect free
- Write paths are transactional
- All concurrency behavior is explicit and documented

---