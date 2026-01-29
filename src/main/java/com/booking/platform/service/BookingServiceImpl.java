package com.booking.platform.service;

import com.booking.platform.domain.BookingStatus;
import com.booking.platform.entity.BookingEntity;
import com.booking.platform.event.EventPublisher;
import com.booking.platform.event.PaymentRequestedEvent;
import com.booking.platform.event.RefundRequestedEvent;
import com.booking.platform.exception.ConflictException;
import com.booking.platform.exception.ResourceNotFoundException;
import com.booking.platform.exception.RetryExhaustedException;
import com.booking.platform.model.BookingResponse;
import com.booking.platform.model.CreateBookingRequest;
import com.booking.platform.repository.BookingRepository;
import org.springframework.data.domain.Sort;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final AvailabilityService availabilityService;
    private final EventPublisher  eventPublisher;

    private static final int MAX_RETRIES = 3;

    public BookingServiceImpl(
            BookingRepository bookingRepository,
            AvailabilityService availabilityService,
            EventPublisher eventPublisher) {
        this.bookingRepository = bookingRepository;
        this.availabilityService = availabilityService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public BookingResponse createBooking(CreateBookingRequest request) {
        int attempt = 0;

        /*
         * RETRY STRATEGY:
         *
         * Optimistic locking failures indicate concurrent modification.
         * We retry a bounded number of times using fresh transactions.
         *
         * Retries are bounded to avoid:
         * - infinite loops
         * - request amplification under load
         * - thundering herd under load
         *
         * If contention persists after retries,
         * we fail fast and surface a concurrency conflict.
         */
        while (true) {
            try {
                attempt++;
                return createBookingInternal(request);
            } catch (ObjectOptimisticLockingFailureException ex) {

                if (attempt >= MAX_RETRIES) {
                    throw new RetryExhaustedException(
                            "Booking could not be completed due to high contention. Please retry.");
                }
            }
        }
    }

    @Transactional
    protected BookingResponse createBookingInternal(CreateBookingRequest request) {
        /*
         * Transaction boundary:
         * ---------------------
         * This transaction ensures that:
         * - Availability reservation
         * - Booking creation
         * either both succeed or both fail.
         *
         * IMPORTANT:
         * Transactional consistency does NOT prevent race conditions
         * when multiple requests execute concurrently.
         *
         * Under high load, multiple transactions can still:
         * - Read the same availability row
         * - See the same availableRooms value
         * before either transaction commits.
         *
         * This read-modify-write sequence is vulnerable under high concurrency.
         *
         * Multiple transactions can:
         * - read the same availability
         * - pass validation
         * - decrement concurrently
         *
         * This is acceptable for now because:
         * - traffic is low
         * - correctness improvements (locking / retries) come next
         */

        // 1. Idempotency check
        /*
         * IDEMPOTENCY NOTE:
         * -----------------
         * Code-level checks prevent duplicate retries.
         * Database-level UNIQUE constraint guarantees safety
         * under concurrent requests with the same idempotency key.
         */
        Optional<BookingEntity> existing = bookingRepository.findByIdempotencyKey(
                request.getIdempotencyKey());

        if(existing.isPresent()) {
            BookingEntity booking = existing.get();

            return new BookingResponse(
                    booking.getStatus().name(),
                    "Duplicate request - returning existing booking",
                    booking.getId()
            );
        }

        // 2. Determine booking date
        // Currently simplified to a single-day booking (today).
        // In real systems, this would iterate over a date range
        // and reserve availability for each date in the stay.
        LocalDate bookingDate = LocalDate.now();

        // 3. Reserve availability
        availabilityService.reserve(request.getHotelName(), request.getRoomType(), bookingDate);

        // 4. Create new booking
        BookingEntity bookingEntity = new BookingEntity(
                request.getUserName(),
                request.getHotelName(),
                request.getRoomType(),
                request.getNights(),
                BookingStatus.CREATED,
                request.getIdempotencyKey()
        );

        BookingEntity saved = bookingRepository.save(bookingEntity);

        return new BookingResponse(
                BookingStatus.CREATED.name(),
                "Booking created successfully",
                saved.getId()
        );
    }

    @Override
    @Transactional
    public BookingResponse getBookingById(Long id) {
        BookingEntity booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));

        expireBookingIfNeeded(booking);

        return mapToResponse(booking);
    }

    @Override
    public List<BookingResponse> getBookingsByUser(String userName) {
        // Returning empty list is preferred over 404 for collection resources
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");

        return bookingRepository.findByUserName(userName, sort)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // Cancellation
    @Override
    @Transactional
    public BookingResponse cancelBooking(Long id) {

        /*
         * CONCURRENCY NOTE:
         * -----------------
         * This release operation is also vulnerable under high concurrency.
         * Optimistic locking / retries will be added in a later phase.
         *
         * DESIGN NOTE:
         * ------------
         * Cancellation is implemented as a state transition
         * followed by inventory release.
         *
         * This ensures:
         * - domain invariants are enforced
         * - availability is never released for invalid bookings
         */

        BookingEntity booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));

        // Expire first if needed
        expireBookingIfNeeded(booking);

        BookingStatus currentStatus = booking.getStatus();

        switch (currentStatus) {
            case EXPIRED -> { // Expired bookings cancellation is not allowed
                throw new ConflictException("Booking has expired");
            }
            case CANCELLED -> { // Idempotent cancellation
                return new BookingResponse(
                        BookingStatus.CANCELLED.name(),
                        "Booking already cancelled",
                        booking.getId());
            }
            case REFUNDED -> { // Refunded bookings cancellation is not allowed
                throw new ConflictException("Booking has cancelled and refunded");
            }
            case REFUND_PENDING -> {
                throw new ConflictException("Booking has cancelled and refund is in progress");
            }
            case CREATED -> {
                // No payment happened → simple cancel
                booking.changeStatus(BookingStatus.CANCELLED);

                availabilityService.release(
                        booking.getHotelName(),
                        booking.getRoomType(),
                        booking.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                );

                return new BookingResponse(
                        BookingStatus.CANCELLED.name(),
                        "Booking cancelled successfully",
                        booking.getId());
            }
            case CONFIRMED -> {
                // Payment happened → go directly to REFUND_PENDING
                booking.changeStatus(BookingStatus.REFUND_PENDING);

                availabilityService.release(
                        booking.getHotelName(),
                        booking.getRoomType(),
                        booking.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                );

                eventPublisher.publish(new RefundRequestedEvent(booking.getId(), Instant.now()));

                return new BookingResponse(
                        BookingStatus.REFUND_PENDING.name(),
                        "Booking cancelled and refund initiated",
                        booking.getId());
            }
            default -> throw new IllegalStateException(
                    "Unhandled booking status: " + currentStatus
            );
        }
    }

    // Payment -> Confirmation
    @Override
    @Transactional
    public BookingResponse confirmBooking(Long id) {
        BookingEntity booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));

        // 1. Enforce expiry first
        expireBookingIfNeeded(booking);

        if(booking.getStatus() == BookingStatus.EXPIRED) {
            throw new ConflictException("Booking has expired");
        }

        if(booking.getStatus() == BookingStatus.CONFIRMED) {
            return mapToResponse(booking);
        }

        if(booking.getStatus() != BookingStatus.CREATED) {
            throw new ConflictException("Booking is canceled");
        }

        // PHASE 7: Emit async payment request
        eventPublisher.publish(new PaymentRequestedEvent(id, Instant.now()));

        return new BookingResponse(
                BookingStatus.CREATED.name(),
                "Payment requested asynchronously",
                booking.getId()
        );
    }

    // Expiry
    @Transactional
    protected void expireBookingIfNeeded(BookingEntity booking) {

        if(!booking.isExpired(Instant.now())) {
            return;
        }

        booking.expireIfNeeded(Instant.now());

        availabilityService.release(
                booking.getHotelName(),
                booking.getRoomType(),
                booking.getCreatedAt()
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate()
        );
    }

    private BookingResponse mapToResponse(BookingEntity booking) {
        return new BookingResponse(
                booking.getStatus().name(),
                "Success",
                booking.getId()
        );
    }
}
