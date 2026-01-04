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
    public void reserve(String hotelName, RoomType roomType, LocalDate date) {

        /*
         * RACE CONDITION WINDOW:
         * ----------------------
         * The following read-check-update sequence is NOT atomic
         * across concurrent transactions.
         *
         * Example under concurrency:
         *
         * T1 reads availableRooms = 1
         * T2 reads availableRooms = 1
         *
         * T1 decrements -> 0
         * T2 decrements -> -1
         *
         * Both transactions succeed, resulting in overbooking.
         *
         * This happens because:
         * - Default isolation level is READ_COMMITTED
         * - No row-level locking or version check is applied
         *
         * This implementation is intentionally naive and correct
         * only under low concurrency.
         */

        AvailabilityEntity availability =  availabilityRepository
                .findByHotelNameAndRoomTypeAndDate(hotelName, roomType, date)
                .orElseThrow(() -> new IllegalArgumentException("No availability configured for given date"));

        if(!availability.hasAvailability())
            throw new IllegalStateException("No rooms available for given date");

        availability.decrement();
        availabilityRepository.save(availability);
    }
}
