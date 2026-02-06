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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.time.Instant;
import java.util.Optional;

@Service
public class RefundServiceImpl implements RefundService {

    private final RefundRepository refundRepository;
    private final EventPublisher eventPublisher;

    private static final Logger log = LoggerFactory.getLogger(RefundServiceImpl.class);

    public RefundServiceImpl(RefundRepository refundRepository,
                             @Lazy EventPublisher eventPublisher) {
        this.refundRepository = refundRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public RefundStatus initiateRefund(Long bookingId) {
        MDC.put("bookingId", String.valueOf(bookingId));

        try {
            log.info("Initiating refund");

            // Idempotency check
            Optional<RefundEntity> existing = refundRepository.findByBookingId(bookingId);

            if (existing.isPresent()) {
                log.info("Refund already exists. Returning status={}",
                        existing.get().getStatus());
                return existing.get().getStatus();
            }

            // Create refund attempt
            RefundEntity refund = new RefundEntity(bookingId);

            try {
                refundRepository.save(refund);
                log.info("Created new refund attempt. refundId={}", refund.getId());

            }  catch (DataIntegrityViolationException e) {
                log.warn("Race detected while creating refund. Returning existing status.");
                return refundRepository.findByBookingId(bookingId)
                        .orElseThrow()
                        .getStatus();
            }

            // Simulate gateway outcome
            boolean success = simulateRefund();
            log.info("Refund simulation result={}", success ? "SUCCESS" : "FAILED");

            if (success) {
                refund.markSuccess();
                log.info("Publishing RefundSucceededEvent. refundId={}", refund.getId());

                eventPublisher.publish(new RefundSucceededEvent(
                        bookingId, refund.getId(), Instant.now()
                ));
            } else {
                refund.markFailed();
                log.warn("Publishing RefundFailedEvent. refundId={}", refund.getId());

                eventPublisher.publish(new RefundFailedEvent(
                        bookingId, refund.getId(), Instant.now(), "REFUND_GATEWAY_FAILED"
                ));
            }

            return refund.getStatus();
        } finally {
            MDC.clear();
        }
    }

    private boolean simulateRefund() {
        return Math.random() < 0.5;
    }
}
