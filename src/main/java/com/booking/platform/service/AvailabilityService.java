package com.booking.platform.service;

import com.booking.platform.domain.RoomType;

import java.time.LocalDate;

/**
 * AvailabilityService owns inventory rules and availability mutations.
 *
 * BookingService must NOT manipulate availability directly.
 */
public interface AvailabilityService {

    /**
     * Reserves availability for a given hotel, room type, and date.
     *
     * This operation:
     * - Validates remaining capacity
     * - Decrements availableRooms by 1
     *
     * Note:
     * - Currently operates on a SINGLE date.
     * - In real booking systems, this would be invoked once
     *   per date in the booking date range.
     */
    void reserve(String hotelName, RoomType roomType, LocalDate date);
}
