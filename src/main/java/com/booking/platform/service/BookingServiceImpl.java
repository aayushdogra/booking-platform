package com.booking.platform.service;

import com.booking.platform.domain.BookingStatus;
import com.booking.platform.entity.BookingEntity;
import com.booking.platform.model.BookingResponse;
import com.booking.platform.model.CreateBookingRequest;
import com.booking.platform.repository.BookingRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BookingServiceImpl implements BookingService{

    private final BookingRepository bookingRepository;

    public BookingServiceImpl(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @Override
    public BookingResponse createBooking(CreateBookingRequest request) {

        // Check idempotency
        Optional<BookingEntity> existing = bookingRepository.findByIdempotencyKey(request.getIdempotencyKey());

        if(existing.isPresent()) {
            BookingEntity booking = existing.get();

            return new BookingResponse(
                    booking.getStatus().name(),
                    "Duplicate request - returning existing booking",
                    booking.getId()
            );
        }

        // Create new booking
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
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        return mapToResponse(booking);
    }

    @Override
    public List<BookingResponse> getBookingsByUser(String userName) {
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
