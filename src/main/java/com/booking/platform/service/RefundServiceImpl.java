package com.booking.platform.service;

import com.booking.platform.entity.RefundEntity;
import com.booking.platform.event.EventPublisher;
import com.booking.platform.event.RefundFailedEvent;
import com.booking.platform.event.RefundSucceededEvent;
import com.booking.platform.repository.RefundRepository;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Lazy;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class RefundServiceImpl implements RefundService {

    private final RefundRepository refundRepository;
    private final EventPublisher eventPublisher;

    public RefundServiceImpl(RefundRepository refundRepository,
                             @Lazy EventPublisher eventPublisher) {
        this.refundRepository = refundRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public RefundEntity initiateRefund(Long bookingId) {

        // Idempotency check
        Optional<RefundEntity> existing = refundRepository.findByBookingId(bookingId);

        if (existing.isPresent()) {
            return existing.get();
        }

        // Create refund attempt
        RefundEntity refund = new RefundEntity(bookingId);
        refundRepository.save(refund);

        // Simulate gateway outcome
        boolean success = simulateRefund();

        if (success) {
            refund.markSuccess();

            eventPublisher.publish(
                    new RefundSucceededEvent(
                            refund.getBookingId(),
                            refund.getId(),
                            Instant.now()
                    )
            );
        } else {
            refund.markFailed();

            eventPublisher.publish(
                    new RefundFailedEvent(
                            refund.getBookingId(),
                            refund.getId(),
                            Instant.now(),
                            "REFUND_GATEWAY_FAILED"
                    )
            );
        }

        return refund;
    }

    private boolean simulateRefund() {
        return Math.random() < 0.5;
    }
}
