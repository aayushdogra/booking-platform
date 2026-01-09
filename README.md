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
├── config
│   └── AvailabilityDataInitializer.java         
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

### 4. Idempotent Booking Creation (Critical Feature)

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
## 12. Booking Cancellation (Write Path)

### Cancel Booking API
`POST /bookings/{id}/cancel`

This endpoint allows a user to cancel an existing booking.

#### Behaviour
- Transitions booking state to `CANCELLED`
- Releases reserved availability
- Is idempotent (safe to call multiple times)
- Returns success even if the booking was already `cancelled`

#### Why this matters
- Cancellation is a real-world requirement
- Inventory must be returned to the pool
- Destructive operations must be idempotent

#### Transactional Cancellation Flow
```txt
BEGIN TRANSACTION
1. Fetch booking by ID
2. If already CANCELLED → return success
3. Transition booking state to CANCELLED
4. Release availability
   COMMIT
```

Implemented in `BookingServiceImpl` using `@Transactional`.

This ensures:
- Booking state and inventory are updated atomically
- Partial updates cannot occur
- System invariants are preserved

---

## 13. Availability Release (Inventory Symmetry)

Availability release is implemented as the inverse of reservation.

### Inventory Operations
```text
reserve() → availableRooms--
release() → availableRooms++
```

### Domain Enforcement
- Inventory cannot go below zero
- Inventory cannot exceed total capacity

These invariants are enforced inside `AvailabilityEntity`.

This symmetry ensures:
- Correct behavior during cancellations
- Inventory consistency over time

---

## 14. Idempotent Cancellation Semantics

Cancellation is designed to be idempotent.

#### Behaviour
- Cancelling an already `cancelled` booking:
    - Does not throw an error 
    - Does not release availability again 
    - Returns a successful response

This protects the system against:
- Client retries
- Duplicate requests
- Concurrent cancellation attempts

---

## 15. Concurrency Awareness (Documented, Not Yet Fixed)

Both reservation and release follow a read–modify–write pattern.

### Known Limitations
- Multiple concurrent transactions can read the same availability
- Default isolation level is `READ_COMMITTED`
- Race conditions are possible under high concurrency

These limitations are explicitly documented in code.

The system is:
- Correct under low concurrency
- Intentionally naive as a foundation for later improvements

---

## 16. Dev-Only Availability Seeding

To support local development and testing, availability is pre-seeded in development mode.

### Characteristics
- Runs only under the `dev` profile
- Seeds availability for known hotels, room types, and dates
- Does not run in production
- Does not affect booking or availability logic

#### This avoids:
- Manual database inserts during development
- Polluting core domain logic with test behavior

---

## 17. Clear Separation of Read vs Write Paths

### Read Paths
 - `GET /bookings/{id}`
- `GET /bookings?userName=...`

#### Characteristics:
- Non-transactional
- Side-effect free
- Lightweight and scalable

### Write Paths
- `POST /bookings`
- `POST /bookings/{id}/cancel`

#### Characteristics:
- Transactional
- Enforce domain invariants
- Mutate multiple entities atomically

This separation mirrors real production backend systems.

---

## 18. Optimistic Locking (Concurrency Control)

Availability updates use **optimistic locking** to detect concurrent write conflicts.

### Implementation

- AvailabilityEntity includes a version field:
```java
@Version
private Long version;
```
- Every availability update is guarded by a version check at the database level.
- If another transaction modifies the same row before commit, the update fails.

This prevents **lost updates and silent overbooking**.

---

## 19. Observed Concurrency Failure Mode (Verified)

Concurrent booking requests against the same availability row were tested using parallel requests.

### Observed Behavior
- One booking succeeds 
- One booking fails with a concurrency conflict 
- Inventory is decremented exactly once 
- Version increments exactly once 
- No data corruption occurs

### API Response on Conflict

```json
{
  "status": 409,
  "error": "Concurrency Conflict",
  "message": "Resource was modified by another request. Please retry.",
  "path": "/bookings"
}
```

This failure mode is intentional and desirable.

---