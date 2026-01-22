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
- Incremental transition from synchronous to event-driven flows

---

## High-Level Architecture

```txt 
Controller  →  Service (Orchestration) →  Repository  →  Database
                            ↓            
                       Domain Rules
```

## Architectural Intent

- Controllers are HTTP adapters only
- Services orchestrate workflows and transactions
- Domain entities enforce lifecycle invariants
- Repositories are persistence-only
- No cross-domain mutation
- State transitions are explicit and validated

---

## Core Concepts Implemented

- Explicit booking lifecycle (`CREATED → CONFIRMED | CANCELLED | EXPIRED`)
- Availability modeled as a first-class domain
- Idempotent booking creation and cancellation
- Time-bound booking holds with expiry
- Optimistic locking with bounded retries
- Synchronous payment confirmation
- Domain events for payment outcomes
- In-process event consumers with idempotent handling
- Controlled retries at async boundaries

This project intentionally avoids premature infrastructure such as Kafka, DLQs, or schedulers
until correctness and ownership are clearly defined.

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
│   └── consumer      // In-process async consumers
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

## Payments & Events

Payments are initiated synchronously and produce immutable domain events:

- `PaymentSucceededEvent`
- `PaymentFailedEvent`

### Current behavior:

- Events are emitted after payment state is finalized
- Events are consumed in-process
- Consumers are single-threaded
- Consumers are idempotent
- Retries are bounded and applied only to transient failures

This establishes a safe foundation for later async infrastructure.

---

## Failure & Retry Semantics

- Business-rule violations are **never retried**
- Transient failures (DB, contention) are retried
- Retries are bounded
- Backoff is applied
- Retry logic lives at async boundaries, not in domain logic

DLQ and broker-backed delivery are intentionally deferred.

---

## Development Notes

- Availability is pre-seeded under the dev profile
- Read paths are side-effect free
- Write paths are transactional
- All concurrency behavior is explicit and documented

---