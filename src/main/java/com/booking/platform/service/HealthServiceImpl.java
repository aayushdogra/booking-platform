package com.booking.platform.service;

import com.booking.platform.repository.BookingRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class HealthServiceImpl implements HealthService {

    private final BookingRepository bookingRepository;
    private final StringRedisTemplate redisTemplate;

    public HealthServiceImpl(BookingRepository bookingRepository,  StringRedisTemplate redisTemplate) {
        this.bookingRepository = bookingRepository;
        this.redisTemplate = redisTemplate;
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

    @Override
    public boolean isRedisUp() {
        try {
            String pong = redisTemplate
                    .getConnectionFactory()
                    .getConnection()
                    .ping();

            return "PONG".equalsIgnoreCase(pong);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean isPaymentsUp() {
        // Simulated external gateway check
        return true;
    }
}
