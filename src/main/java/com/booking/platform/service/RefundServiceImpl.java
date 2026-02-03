package com.booking.platform.service;

import com.booking.platform.domain.RefundStatus;
import com.booking.platform.entity.RefundEntity;
import com.booking.platform.event.EventPublisher;
import com.booking.platform.event.RefundFailedEvent;
import com.booking.platform.event.RefundSucceededEvent;
import com.booking.platform.repository.RefundRepository;
import org.springframework.dao.DataIntegrityViolationException;
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
    public RefundStatus initiateRefund(Long bookingId) {

        // Idempotency check
        Optional<RefundEntity> existing = refundRepository.findByBookingId(bookingId);

        if (existing.isPresent()) {
            return existing.get().getStatus();
        }

        // Create refund attempt
        RefundEntity refund = new RefundEntity(bookingId);

        try {
            refundRepository.save(refund);
        }  catch (DataIntegrityViolationException e) {
            return refundRepository.findByBookingId(bookingId)
                    .orElseThrow()
                    .getStatus();
        }

        // Simulate gateway outcome
        boolean success = simulateRefund();

        if (success) {
            refund.markSuccess();

            eventPublisher.publish(new RefundSucceededEvent(
                            bookingId, refund.getId(), Instant.now()
            ));
        } else {
            refund.markFailed();

            eventPublisher.publish(new RefundFailedEvent(
                    bookingId, refund.getId(), Instant.now(), "REFUND_GATEWAY_FAILED"
            ));
        }

        return refund.getStatus();
    }

    private boolean simulateRefund() {
        return Math.random() < 0.5;
    }
}
