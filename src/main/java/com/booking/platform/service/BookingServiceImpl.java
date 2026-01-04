package com.booking.platform.service;

import com.booking.platform.domain.BookingStatus;
import com.booking.platform.entity.BookingEntity;
import com.booking.platform.model.BookingResponse;
import com.booking.platform.model.CreateBookingRequest;
import com.booking.platform.repository.BookingRepository;
import com.booking.platform.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final AvailabilityService availabilityService;

    public BookingServiceImpl(BookingRepository bookingRepository, AvailabilityService availabilityService) {
        this.bookingRepository = bookingRepository;
        this.availabilityService = availabilityService;
    }

    @Override
    @Transactional
    public BookingResponse createBooking(CreateBookingRequest request) {

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
         */

        // 1. Idempotency check
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
                "CREATED",
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
        return bookingRepository.findByUserName(userName)
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
}
