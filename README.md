# Booking Platform (Backend)

Building a production-style Agoda-inspired booking backend using Java + Spring Boot, 
designed to demonstrate real-world backend engineering concepts such as clean architecture, persistence, 
idempotency, lifecycle management, transactional consistency, availability modeling, 
and scalable system design foundations.

---

## What This Project Already Demonstrates

- Clean layered architecture
- Domain-driven modeling
- Booking lifecycle & state transitions
- Idempotent APIs
- Validation & structured error handling
- Availability modeling
- Transaction boundaries
- Separation of orchestration vs domain logic
- Foundations for concurrency handling

---

## Current Architecture
```txt 
Controller  →  Service (Orchestration) →  Repository  →  Database
↓            ↓
DTOs      Domain logic + Rules
```
### Core Principles
- Controllers are thin (HTTP-only)
- Services coordinate workflows 
- Availability logic is isolated 
- Repositories handle persistence only 
- Entities represent database state 
- DTOs are API contracts 
- Domain rules are explicit 
- Transactions live at the service layer

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
│   ├── BookingServiceImpl.java       # Orchestrates booking flow (transactional)
│   ├── AvailabilityService.java      # Availability contract
│   ├── AvailabilityServiceImpl.java  # Availability rules & updates
│   ├── HealthService.java
│   └── HealthServiceImpl.java
│
├── repository
│   ├── BookingRepository.java        # JPA access
│   └── AvailabilityRepository.java   # Availability persistence        
│
├── entity
│   ├── BookingEntity.java            # Booking persistence model
│   └── AvailabilityEntity.java       # Inventory per date & room type
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

Used for:
- liveness checks
- database connectivity validation

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

#### Implementation
- Client sends `idempotencyKey`
- Stored with unique DB constraint
- Repository lookup prevents duplication 

```java Optional<BookingEntity> findByIdempotencyKey(String key);```

#### Behaviour
- If key exists → return existing booking
- Otherwise → create new booking

This guarantees: `Same request → same booking`

---

### 5. Availability Modeling

Availability is modeled explicitly instead of being derived from bookings.

`(hotelName, roomType, date)`

Each row represents capacity for one day.

#### AvailabilityEntity Fields

- `hotelName`
- `roomType`
- `date`
- `totalRooms`
- `availableRooms`
- `timestamps`

Unique constraint ensures only one row per:

`hotel + roomType + date`

---

### 6. Availability Service

Availability logic is isolated into its own service.

#### Responsibilities
- Fetch availability by key
- Validate remaining capacity
- Decrement availability
- Persist changes

#### Public contract
```java void reserve(String hotelName, RoomType roomType, LocalDate date); ```

---

### 7. Booking ↔ Availability Integration

#### Transactional Flow

```text
BEGIN TRANSACTION
  1. Check idempotency
  2. Reserve availability
  3. Create booking
COMMIT
```

Implemented in `BookingServiceImpl` using `@Transactional`.

#### Why this matters
- Keeps business logic readable
- Ensures atomic updates
- Prevents partial writes
- Mirrors real backend orchestration

---

### 8. Current Concurrency Model (Intentional Limitation)

The current implementation is correct but naive.

#### Behavior
- Uses default READ_COMMITTED isolation
- Can suffer race conditions under heavy concurrency
- Two requests may read the same availability before commit

This is intentional and serves as the foundation for later improvements.

---

### 9. Read APIs

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

## 10. BookingEntity (Persistence Model)

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

## 11. Error Handling

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
