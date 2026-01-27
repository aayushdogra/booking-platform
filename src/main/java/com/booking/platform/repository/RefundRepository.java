package com.booking.platform.repository;

import com.booking.platform.entity.RefundEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefundRepository extends JpaRepository<RefundEntity, Long> {

    Optional<RefundEntity> findByBookingId(Long bookingId);
}
