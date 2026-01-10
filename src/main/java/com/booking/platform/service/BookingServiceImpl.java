package com.booking.platform.service;

import com.booking.platform.domain.BookingStatus;
import com.booking.platform.entity.BookingEntity;
import com.booking.platform.model.BookingResponse;
import com.booking.platform.model.CreateBookingRequest;
import com.booking.platform.repository.BookingRepository;
import com.booking.platform.exception.ResourceNotFoundException;
import org.springframework.data.domain.Sort;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final AvailabilityService availabilityService;

    private static final int MAX_RETRIES = 3;

    public BookingServiceImpl(BookingRepository bookingRepository, AvailabilityService availabilityService) {
        this.bookingRepository = bookingRepository;
        this.availabilityService = availabilityService;
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
         * - thundering herd under load
         */
        while (true) {
            try {
                attempt++;
                return createBookingInternal(request);
            } catch (ObjectOptimisticLockingFailureException ex) {

                if (attempt >= MAX_RETRIES) {
                    throw ex;
                }

                // retry with fresh transaction
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
        Optional<BookingEntity> existing = bookingRepository.findByIdempotencyKey(request.getIdempotencyKey());

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
    public BookingResponse getBookingById(Long id) {
        BookingEntity booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));

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

    private BookingResponse mapToResponse(BookingEntity booking) {
        return new BookingResponse(
                booking.getStatus().name(),
                "Success",
                booking.getId()
        );
    }

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

        // Idempotent cancellation
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return new BookingResponse(
                    BookingStatus.CANCELLED.name(),
                    "Booking already cancelled",
                    booking.getId()
            );
        }

        /*
         * State transition is enforced inside the entity.
         * Illegal transitions will throw an exception.
         */
        booking.changeStatus(BookingStatus.CANCELLED);

        /*
         * Availability release happens AFTER state transition.
         * This ensures we never release availability for invalid bookings.
         */
        availabilityService.release(
                booking.getHotelName(),
                booking.getRoomType(),
                booking.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        );

        return new BookingResponse(
                BookingStatus.CANCELLED.name(),
                "Booking cancelled successfully",
                booking.getId()
        );
    }
}
