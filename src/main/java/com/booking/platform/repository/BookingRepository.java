package com.booking.platform.repository;

import com.booking.platform.entity.BookingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<BookingEntity, Long> {
    Optional<BookingEntity> findByIdempotencyKey(String idempotencyKey);

    List<BookingEntity> findByUserName(String userName);
}