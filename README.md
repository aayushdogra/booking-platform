# Booking Platform (Backend)

A backend architecture exploration inspired by real-world booking systems, built with **Java 21 + Spring Boot**.

This project demonstrates how a system evolves from synchronous correctness to controlled event-driven 
coordination — with strong emphasis on lifecycle integrity, concurrency safety, and deterministic domain 
behaviour.

It is intentionally single-process and single-database to focus on architectural clarity over infrastructure complexity.

---

## Purpose

This repository is designed to showcase practical backend engineering principles:
- Aggregate-driven domain modeling
- Explicit lifecycle management
- Inventory correctness under concurrency
- Event-driven orchestration within clear boundaries
- Retry classification and failure isolation
- Deterministic state transitions
- Clean read/write separation

The goal is to explore how systems grow in complexity responsibly, not to simulate a full booking product.

The intent is not to simulate a production-scale booking product, but to demonstrate how complex 
backend systems are structured responsibly.

---

## High-Level Architecture
This project follows a layered architecture with explicit responsibility boundaries:

```txt 
Controller → Application Service → Domain Aggregate → Repository → Database  
                                             ↓  
                                      Event Boundary (Async)
```

### Design Principles

- Controllers act strictly as HTTP adapters
- Services orchestrate workflows and transactions
- Domain aggregates enforce lifecycle invariants
- Repositories are persistence-only
- State transitions are explicit and validated
- Events represent domain facts
- Idempotency is enforced primarily via domain state
- Retry logic is isolated from domain logic

The system emphasises correctness before scalability.

---

## System Evolution Approach

The architecture was evolved incrementally:

1. Baseline synchronous flow
2. Inventory correctness with optimistic locking
3. Time-bound booking lifecycle (expiry)
4. Explicit aggregate state machine
5. Async event boundary introduction
6. Retry classification and bounded retries
7. Dead-letter persistence
8. Read/write separation (CQRS-lite)

Each phase introduces one complexity dimension at a time.

---

## Core Domain Concepts

### Booking Lifecycle

The booking aggregate enforces explicit lifecycle transitions such as:

- `Created → Confirmed`
- `Created → Cancelled`
- `Created → Expired`
- `Confirmed → Refund Pending`
- `Refund Pending → Refunded`

Lifecycle invariants are enforced inside the aggregate to ensure:

- No illegal transitions
- Terminal state protection
- Idempotent operations
- Deterministic behavior under retries

This prevents business rules from leaking across services.

---

### Availability Model

Inventory is modelled explicitly by: `(hotel, roomType, date)`

Characteristics:

- Quantity-based availability
- Optimistic locking for concurrency safety
- Symmetric reserve and release operations
- No derived inventory logic
- No negative inventory states

The system ensures correctness under concurrent booking attempts.

---

## Async Coordination

Payments and refunds cross an explicit async boundary within the JVM.

Design characteristics:

- Executor-based async dispatch
- Separate transaction per consumer execution
- Deterministic domain updates
- Retry classification (retryable vs non-retryable)
- Bounded retries with backoff
- Dead-letter persistence for exhaustion

This simulates distributed behaviour while maintaining architectural clarity.

---

## Idempotency Strategy

Idempotency is enforced at multiple levels:

- Unique constraints for booking/payment/refund creation
- Aggregate-level state guards
- Event-level deduplication
- Retry boundary isolation

The aggregate remains the ultimate consistency guard.

---

## Failure Isolation

Failure states are modelled explicitly and persisted.

The system demonstrates:

- Retry classification
- Controlled retry exhaustion
- Dead-letter persistence
- Failure reason tracking
- Deterministic aggregate behavior under repeated events

Async consumers remain predictable and side effect safe.

---

## Read / Write Separation

The system exposes:

- REST endpoints for write operations
- GraphQL as a read-only aggregation layer

This establishes a CQRS-lite separation where:

`REST = Command Layer`
`GraphQL = Query Layer`

The read layer has no side effects and no async behaviour.

---

## Tech Stack

- Java 21
- Spring Boot 3.x
- Spring Web
- Spring Data JPA (Hibernate)
- PostgreSQL
- Redis (idempotency & retry coordination)
- Spring GraphQL
- Maven

---

## Engineering Focus

This project demonstrates:

- Aggregate design and invariant enforcement
- Concurrency-aware modeling
- Event-driven coordination patterns
- Retry boundary design
- Failure isolation strategies
- Defensive idempotency patterns
- Clean service boundary refactoring
- Architecture-first scaling mindset

It reflects hands-on backend system design thinking rather than framework usage alone.

---

## Scope & Intentional Constraints

- Single JVM
- Single database
- No external message broker
- No distributed tracing
- No horizontal scaling assumptions

The goal is architectural discipline, not infrastructure simulation.

---

## What This Project Represents

This repository represents a structured backend evolution journey:

From correctness →  
To concurrency safety →  
To async orchestration →  
To failure isolation →  
To design clarity before scale.

It reflects how production systems should be cleaned and stabilised before introducing horizontal scaling.

---

Built as an architectural learning and engineering discipline project.