package com.booking.platform.repository;

import com.booking.platform.entity.DeadLetterEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeadLetterRepository extends JpaRepository<DeadLetterEntity, Long> {

}
