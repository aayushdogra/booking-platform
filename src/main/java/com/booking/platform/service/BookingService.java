package com.booking.platform.service;

import com.booking.platform.model.BookingResponse;
import com.booking.platform.model.CreateBookingRequest;

import java.util.List;

public interface BookingService {

    BookingResponse createBooking(CreateBookingRequest request);

    BookingResponse getBookingById(Long id);

    List<BookingResponse> getBookingsByUser(String username);

    BookingResponse cancelBooking(Long bookingId);

    BookingResponse confirmBooking(Long bookingId);

    // Async consumer entry point
    void confirmBookingFromPaymentEvent(Long bookingId);
}