package com.booking.platform.service;

import com.booking.platform.domain.RoomType;
import com.booking.platform.entity.AvailabilityEntity;
import com.booking.platform.repository.AvailabilityRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class AvailabilityServiceImpl implements AvailabilityService{
    private final AvailabilityRepository availabilityRepository;

    public AvailabilityServiceImpl(AvailabilityRepository availabilityRepository) {
        this.availabilityRepository = availabilityRepository;
    }

    @Override
    public AvailabilityEntity getAvailabilityOrThrow(String hotelName, RoomType roomType, LocalDate date) {
        return availabilityRepository
                .findByHotelNameAndRoomTypeAndDate(hotelName, roomType, date)
                .orElseThrow(() -> new IllegalArgumentException("No availability configured for given date"));
    }

    @Override
    public void decrementAvailability(AvailabilityEntity availability) {
        availability.decrement();
        availabilityRepository.save(availability);
    }
}
