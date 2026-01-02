package com.booking.platform.repository;

import com.booking.platform.entity.AvailabilityEntity;
import com.booking.platform.domain.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface AvailabilityRepository extends  JpaRepository<AvailabilityEntity, Long> {
    Optional<AvailabilityEntity> findByHotelNameAndRoomTypeAndDate(
            String hotelName,
            RoomType roomType,
            LocalDate date
    );
}
