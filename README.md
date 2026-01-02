# Booking Platform (Backend)

Building a production-style Agoda-inspired booking backend using Java + Spring Boot, designed to demonstrate real-world backend engineering concepts such as clean architecture, persistence, idempotency, lifecycle management, and scalable system design foundations.

This repository evolves step-by-step and mirrors how booking systems are actually built in industry.

---

## What This Project Already Demonstrates

- Clean layered architecture 
- Domain-driven thinking 
- State machines 
- Idempotent APIs 
- Validation best practices 
- Proper error modeling 
- Repository abstraction 
- Production-style REST APIs

---

## Current Architecture
```txt 
Controller  →  Service  →  Repository  →  Database
↓            ↓
DTOs      Domain + Rules
```
### Core Principles
- Controllers are thin (HTTP only)
- Services contain business rules
- Repositories handle persistence only
- Entities ≠ API DTOs
- Domain rules are explicit

---

## Tech Stack
- Java 21
- Spring Boot 3.3.2
- Spring Web
- Spring Data JPA
- Hibernate
- PostgreSQL
- Jakarta Validation
- Maven

---

## Project Structure
```txt
com.booking.platform
│
├── BookingPlatformApplication.java
│
├── controller
│   ├── BookingController.java        # Create + Read APIs
│   └── HealthController.java         # Health & DB check
│
├── service
│   ├── BookingService.java
│   ├── BookingServiceImpl.java       # Core business logic
│   ├── HealthService.java
│   └── HealthServiceImpl.java
│
├── repository
│   └── BookingRepository.java        # JPA access
│
├── entity
│   └── BookingEntity.java            # Persistent model
│
├── model
│   ├── CreateBookingRequest.java     # POST request DTO
│   ├── BookingDetailsResponse.java
│   └── BookingResponse.java          # Response DTO
│
├── domain
│   ├── BookingStatus.java            # Booking lifecycle states
│   ├── RoomType.java                 # Enum
│   ├── Booking.java                  # Pure domain model
│   ├── User.java
│   ├── Hotel.java
│   └── Room.java
│
├── exception
│   ├── ApiError.java
│   └── GlobalExceptionHandler.java
```
---

## Implemented Features

### 1. Health Endpoints
`GET /health`, `GET /health/db`

Used for readiness / liveness and DB connectivity checks.

### 2. Booking Creation API (Write Path)
`POST /bookings`

#### Request Body
```json
{
    "idempotencyKey": "abc-123",
    "userName": "Aayush",
    "hotelName": "Taj",
    "roomType": "DELUXE",
    "nights": 2
}
```

#### Validation Rules
- `idempotencyKey` → required
- `userName` → required
- `hotelName` → required
- `roomType` → enum validated
- `nights` >= 1

Validation errors return structured responses.

---

### 3. Booking Lifecycle (State Machine)

#### Supported States

```txt
CREATED
CONFIRMED
CANCELLED
EXPIRED
```

#### Allowed Transitions

```txt
CREATED → CONFIRMED
CREATED → CANCELLED
CREATED → EXPIRED
```

Invalid transitions throw exceptions.
Lifecycle logic lives inside `BookingEntity`: `changeStatus(...)`

This enforces domain correctness.

---

### 4 Idempotent Booking Creation (Critical Feature)

#### Why this matters
Booking systems must tolerate:
- retries
- network failures
- duplicate client requests

Without idempotency → duplicate bookings.

#### How it works
- Client sends `idempotencyKey`
- Stored in DB with **unique constraint**
- Repository method: `Optional<BookingEntity> findByIdempotencyKey(String key);`

#### Service behavior
- If key exists → return existing booking 
- Else → create new booking

This guarantees: `Same request → same booking`

---

### 5. Read APIs

#### Get booking by ID

`GET /bookings/{id}`

Response: 

```json
{
    "status": "CREATED",
    "message": "Success",
    "bookingId": 1
}
```
---

#### Get bookings by user

`GET /bookings?userName=Aayush`

Response:

```json
[
    {
        "status": "CREATED",
        "message": "Success",
        "bookingId": 1
    }
]
```
---

## BookingEntity (Persistence Model)

### Fields
- `id`
- `userName`
- `hotelName`
- `roomType`
- `nights`
- `status`
- `createdAt`
- `updatedAt`
- `idempotencyKey` (unique)

### Responsibilities
- Enforces valid state transitions 
- Tracks timestamps 
- Acts as source of truth for persistence

---

## Error Handling

Centralized via `@RestControllerAdvice`.

### Covered cases
- Validation errors (`@Valid`)
- Invalid enum values 
- Illegal state transitions 
- Generic server failures

### Standard Error Format
```json
{
    "timestamp": "...",
    "status": 400,
    "error": "Validation Failed",
    "message": "...",
    "path": "/bookings"
}
```

---
