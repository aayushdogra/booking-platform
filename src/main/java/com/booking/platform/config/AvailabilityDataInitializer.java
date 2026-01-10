package com.booking.platform.config;

import com.booking.platform.domain.RoomType;
import com.booking.platform.entity.AvailabilityEntity;
import com.booking.platform.repository.AvailabilityRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@Profile("dev")
public class AvailabilityDataInitializer implements CommandLineRunner {

    private final AvailabilityRepository availabilityRepository;

    public AvailabilityDataInitializer(AvailabilityRepository availabilityRepository) {
        this.availabilityRepository = availabilityRepository;
    }

    @Override
    public void run(String... args) {

        LocalDate today = LocalDate.now();

        seedIfMissing("TAJ", RoomType.DELUXE, today, 5);
        seedIfMissing("TAJ", RoomType.SINGLE, today, 10);
        seedIfMissing("TAJ", RoomType.DOUBLE, today, 5);
        seedIfMissing("Hilton", RoomType.DELUXE, today, 5);
        seedIfMissing("Hilton", RoomType.SINGLE, today, 10);
        seedIfMissing("Hilton", RoomType.DOUBLE, today, 5);
    }

    private void seedIfMissing(String hotelName, RoomType roomType, LocalDate date,int totalRooms) {
        boolean exists = availabilityRepository
                .findByHotelNameAndRoomTypeAndDate(hotelName, roomType, date)
                .isPresent();

        if (!exists) {
            AvailabilityEntity availability = new AvailabilityEntity(
                    hotelName,
                    roomType,
                    date,
                    totalRooms,
                    totalRooms
            );

            availabilityRepository.save(availability);
        }
    }
}