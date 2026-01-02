package com.booking.platform.service;

import com.booking.platform.domain.RoomType;
import com.booking.platform.entity.AvailabilityEntity;

import java.time.LocalDate;

public interface AvailabilityService {
    AvailabilityEntity getAvailabilityOrThrow(String hotelName, RoomType roomType, LocalDate date);

    void decrementAvailability(AvailabilityEntity  availability);
}
