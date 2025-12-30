package com.booking.platform.service;

import com.booking.platform.domain.BookingStatus;
import com.booking.platform.domain.RoomType;
import com.booking.platform.entity.BookingEntity;
import com.booking.platform.model.BookingResponse;
import com.booking.platform.model.CreateBookingRequest;
import com.booking.platform.repository.BookingRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class BookingServiceImpl implements BookingService{

    private final BookingRepository bookingRepository;

    public BookingServiceImpl(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @Override
    public BookingResponse createBooking(CreateBookingRequest request) {

        BookingEntity bookingEntity = new BookingEntity(
                request.getUserName(),
                request.getHotelName(),
                request.getRoomType(),
                request.getNights(),
                BookingStatus.PENDING,
                Instant.now()
        );

        BookingEntity saved = bookingRepository.save(bookingEntity);

        return new BookingResponse("CREATED", "Booking created successfully", saved.getId());
    }
}
