package com.booking.platform.service;

import com.booking.platform.domain.PaymentStatus;
import com.booking.platform.entity.PaymentEntity;
import com.booking.platform.event.EventPublisher;
import com.booking.platform.event.PaymentFailedEvent;
import com.booking.platform.event.PaymentSucceededEvent;
import com.booking.platform.repository.PaymentRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Lazy;
import org.springframework.transaction.annotation.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Optional;
import java.time.Instant;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final EventPublisher eventPublisher;

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    public PaymentServiceImpl(PaymentRepository paymentRepository,
                              @Lazy EventPublisher eventPublisher) {
        this.paymentRepository = paymentRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public PaymentStatus initiatePayment(Long bookingId) {
        MDC.put("bookingId", String.valueOf(bookingId));

        try {
            log.info("Initiating payment");
            // 1. Idempotency check
            Optional<PaymentEntity> existing = paymentRepository.findByBookingId(bookingId);

            if(existing.isPresent()) {
                log.info("Payment already exists. Returning existing status={}",
                        existing.get().getStatus());
                return existing.get().getStatus();
            }

            // 2. Create new payment attempt
            PaymentEntity payment = new PaymentEntity(bookingId);

            try {
                paymentRepository.save(payment);
                log.info("Created new payment attempt. paymentId={}", payment.getId());

            } catch (DataIntegrityViolationException e) {
                log.warn("Race detected. Returning existing payment status.");

                return paymentRepository.findByBookingId(bookingId)
                        .orElseThrow()
                        .getStatus();
            }

            // 3. Simulate payment outcome
            boolean success = simulatePayment();
            log.info("Payment simulation result={}", success ? "SUCCESS" : "FAILED");

            if(success) {
                payment.markSuccess();

                log.info("Publishing PaymentSucceededEvent. paymentId={}", payment.getId());

                // 4a. Emit Success event
                eventPublisher.publish(new PaymentSucceededEvent(
                        bookingId, payment.getId(), Instant.now()
                ));
            } else {
                payment.markFailed();

                log.warn("Publishing PaymentFailedEvent. paymentId={}", payment.getId());

                //4b. Emit Failed event
                eventPublisher.publish(new PaymentFailedEvent(
                        bookingId, payment.getId(), Instant.now(), "PAYMENT_DECLINED"
                ));
            }

            return payment.getStatus();

        } finally {
            MDC.clear();
        }
    }

    private boolean simulatePayment() {
        return Math.random() < 0.5;
    }
}
