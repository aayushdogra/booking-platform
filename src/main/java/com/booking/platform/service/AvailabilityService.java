package com.booking.platform.service;

import com.booking.platform.domain.RoomType;

import java.time.LocalDate;

public interface AvailabilityService {
    void reserve(String hotelName, RoomType roomType, LocalDate date);
}
