package com.booking.platform.service;

import com.booking.platform.model.BookingResponse;
import com.booking.platform.model.CreateBookingRequest;

public interface BookingService {

    BookingResponse createBooking(CreateBookingRequest request);
}