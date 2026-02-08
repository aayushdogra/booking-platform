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
import com.booking.platform.service.metrics.BookingMetrics;
import org.springframework.data.domain.Sort;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.time.LocalDate;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final AvailabilityService availabilityService;
    private final EventPublisher  eventPublisher;
    private final BookingMetrics  bookingMetrics;

    private static final Logger log = LoggerFactory.getLogger(BookingServiceImpl.class);

    private static final int MAX_RETRIES = 3;

    public BookingServiceImpl(
            BookingRepository bookingRepository,
            AvailabilityService availabilityService,
            EventPublisher eventPublisher,
            BookingMetrics bookingMetrics) {
        this.bookingRepository = bookingRepository;
        this.availabilityService = availabilityService;
        this.eventPublisher = eventPublisher;
        this.bookingMetrics = bookingMetrics;
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
                log.info("Attempting to create booking. Attempt={}", attempt);

                return createBookingInternal(request);

            } catch (ObjectOptimisticLockingFailureException ex) {
                log.warn("Optimistic locking failure while creating booking. Attempt={}", attempt);

                bookingMetrics.incrementBookingFailed();

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
            log.info("Booking already exists. Returning existing booking. BookingId={}", existing.get().getId());
            BookingEntity booking = existing.get();
            return mapToResponse(booking);
        }

        // 2. Determine booking date
        // Currently simplified to a single-day booking (today).
        // In real systems, this would iterate over a date range
        // and reserve availability for each date in the stay.
        LocalDate bookingDate = LocalDate.now();

        // 3. Reserve availability
        reserveAvailability(request, bookingDate);

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
        log.info("Booking created successfully. BookingId={}", saved.getId());
        bookingMetrics.incrementBookingCreated();

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingById(Long id) {
        BookingEntity booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));

        return mapToResponse(booking);
    }

    @Override
    @Transactional(readOnly = true)
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
        MDC.put("bookingId", String.valueOf(id));
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

        try {
            log.info("Cancelling booking. BookingId={}", id);

            BookingEntity booking = bookingRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));

            // Expire first if needed
            boolean expiredNow = booking.expireIfNeeded(Instant.now());

            if (expiredNow) {
                log.warn("Booking expired before cancelling. BookingId={}", booking.getId());
                releaseAvailability(booking);

                return mapToResponse(booking);
            }

            BookingStatus before = booking.getStatus();
            boolean refundRequired = booking.cancel(Instant.now());

            if(before == BookingStatus.CREATED ||
                    before == BookingStatus.CONFIRMED) {

                releaseAvailability(booking);
            }

            // Publish refund request if needed
            if (refundRequired) {
                log.info("Refund required for bookingId={}", booking.getId());
                bookingMetrics.incrementRefundRequested();

                eventPublisher.publish(
                        new RefundRequestedEvent(booking.getId(), Instant.now())
                );
            }

            return mapToResponse(booking);

        } finally {
            MDC.clear();
        }
    }

    // Payment -> Confirmation
    @Override
    @Transactional
    public BookingResponse confirmBooking(Long id) {

        MDC.put("bookingId", String.valueOf(id));

        try {
            log.info("Confirm booking requested. BookingId={}", id);

            BookingEntity booking = bookingRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));

            // Enforce expiry first
            boolean expiredNow = booking.expireIfNeeded(Instant.now());

            if (expiredNow) {
                log.warn("Booking expired before payment request. BookingId={}", booking.getId());
                releaseAvailability(booking);
                //throw new ConflictException("Booking has expired");
                return mapToResponse(booking);
            }

            booking.requestPayment(Instant.now());

            log.info("Publishing PaymentRequestedEvent. BookingId={}", booking.getId());
            eventPublisher.publish(new PaymentRequestedEvent(booking.getId(), Instant.now()));
            return mapToResponse(booking);

        } finally {
            MDC.clear();
        }
    }

    private void reserveAvailability(CreateBookingRequest request, LocalDate bookingDate) {
        availabilityService.reserve(request.getHotelName(), request.getRoomType(), bookingDate);
    }

    private void releaseAvailability(BookingEntity booking) {
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
