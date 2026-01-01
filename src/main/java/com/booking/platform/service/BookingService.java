package com.booking.platform.service;

import com.booking.platform.model.BookingResponse;
import com.booking.platform.model.CreateBookingRequest;

import java.util.List;

public interface BookingService {

    BookingResponse createBooking(CreateBookingRequest request);

    BookingResponse getBookingById(Long id);

    List<BookingResponse> getBookingsByUser(String username);
}