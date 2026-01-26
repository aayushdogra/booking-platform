# Booking Platform (Backend)

A backend architecture exploration inspired by real-world booking systems, built with **Java 21 + Spring Boot**.

This project intentionally evolves from a simple synchronous flow into a controlled, event-driven system.
It prioritizes **correctness, lifecycle integrity, and architectural clarity** over feature breadth.

This is not a production-ready SaaS product.
It is an intentionally incremental backend design exercise.

---

## Project Intent

This repository demonstrates backend evolution patterns:
- From synchronous correctness
- To concurrency safety
- To event-driven coordination
- To bounded retries and failure isolation

The goal is to explore how systems grow in complexity responsibly, not to simulate a full booking product.

This project focuses on:
- Domain-driven lifecycle modeling
- Deterministic state transitions
- Inventory correctness under contention
- Explicit async boundary design
- Failure classification and retry isolation

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
- Idempotency is primarily enforced via domain state

---

## System Evolution Approach

The architecture evolved in phases:
- Baseline synchronous flow
- Optimistic locking for availability safety
- Time-bound booking holds
- Idempotent payment modeling
- Introduction of domain events
- Async boundary within a single JVM
- Consumer-level retry and DLQ design

Each phase introduces one complexity dimension at a time.

The system remains intentionally single-process and single-database.

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

- Transitions are explicit
- Terminal states are immutable
- Expiry is system-driven
- Cancellation is user-driven and idempotent
- Confirmation occurs only via payment success event

Lifecycle enforcement lives inside the aggregate.

---

## Availability Model

Availability is modeled explicitly as:

`(hotel, roomType, date)`


### Characteristics:

- Quantity-based inventory
- Optimistic locking via `@Version`
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

Payments cross an explicit async boundary inside the JVM.

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
PaymentEventConsumer
    ↓
Booking confirmation
```

### Characteristics:

- Executor-based async dispatch
- Separate transaction per consumer execution
- Idempotent event handling
- Explicit state guards
- Controlled retry classification
- In-memory DLQ design (non-durable)

This simulates event-driven behavior without introducing distributed infrastructure.

---

## Retry & Failure Semantics

Retries are scoped to event consumers only.

### Retry Rules
Retryable:
- Optimistic locking
- Transient persistence issues

Non-retryable:
- Invalid lifecycle transitions
- Domain rule violations

### Properties
- Bounded retries (max 3)
- Linear backoff
- Classification-based retry decisions
- DLQ triggered only for processing failures

Domain services remain deterministic and free of retry logic.

---

## Dead Letter Queue (Conceptual)

The DLQ is currently in-memory and design-focused.

It stores:
- Original event
- Retry count
- Failure classification
- Error message
- Timestamp

It exists to demonstrate:
- Failure isolation
- Retry exhaustion handling
- Async processing control

Durable persistence is intentionally deferred.

---

## Development Notes

- Single JVM
- Single PostgreSQL database
- Dev profile seeds availability
- No external broker
- No distributed tracing
- No horizontal scaling assumptions

This repository reflects an architectural journey, not a finished product.

---