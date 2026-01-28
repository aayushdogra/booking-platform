package com.booking.platform.event.dlq;

import com.booking.platform.entity.DeadLetterEntity;
import com.booking.platform.event.DomainEvent;
import com.booking.platform.event.PaymentSucceededEvent;
import com.booking.platform.event.RefundSucceededEvent;
import com.booking.platform.repository.DeadLetterRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class JpaDeadLetterStore implements DeadLetterStore {

    private final DeadLetterRepository repository;
    private final ObjectMapper objectMapper;

    public JpaDeadLetterStore(DeadLetterRepository repository,
                              ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(DeadLetterEvent event) {
        try {
            String payloadJson = objectMapper.writeValueAsString(event.originalEvent());

            DeadLetterEntity entity = new DeadLetterEntity(
                    event.originalEvent().getClass().getSimpleName(),
                    extractAggregateId(event.originalEvent()),
                    payloadJson,
                    event.retryCount(),
                    event.failureReason(),
                    event.errorMessage(),
                    event.failedAt()
            );

            repository.save(entity);

        } catch (Exception ex) {
            System.err.println("CRITICAL: Failed to persist DLQ event " + ex.getMessage());
        }
    }

    private Long extractAggregateId(DomainEvent event) {
        if (event instanceof PaymentSucceededEvent e) return e.bookingId();
        if (event instanceof RefundSucceededEvent e) return e.bookingId();

        return null;
    }
}
