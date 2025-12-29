package com.booking.platform.service;

import com.booking.platform.model.BookingResponse;
import com.booking.platform.model.CreateBookingRequest;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class BookingServiceImpl implements BookingService{

    @Override
    public BookingResponse createBooking(CreateBookingRequest request) {
        String bookingId = "BK-" + UUID.randomUUID();

        return new BookingResponse("SUCCESS", "Booking created successfully", bookingId);
    }
}
