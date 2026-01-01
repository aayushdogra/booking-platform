package com.booking.platform.service;

import com.booking.platform.repository.BookingRepository;
import org.springframework.stereotype.Service;

@Service
public class HealthServiceImpl implements HealthService {
    private final BookingRepository bookingRepository;

    public HealthServiceImpl(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @Override
    public boolean isDatabaseUp() {
        try {
            bookingRepository.count();
            return true;
        }  catch (Exception e) {
            return false;
        }
    }
}
