package com.booking.platform.service;

import com.booking.platform.domain.PaymentStatus;
import com.booking.platform.entity.PaymentEntity;
import com.booking.platform.event.EventPublisher;
import com.booking.platform.event.PaymentFailedEvent;
import com.booking.platform.event.PaymentSucceededEvent;
import com.booking.platform.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.time.Instant;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final EventPublisher eventPublisher;

    public PaymentServiceImpl(PaymentRepository paymentRepository, EventPublisher eventPublisher) {
        this.paymentRepository = paymentRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public PaymentStatus initiatePayment(Long bookingId) {

        // 1. Idempotency check
        Optional<PaymentEntity> existing = paymentRepository.findByBookingId(bookingId);

        if(existing.isPresent()) {
            return existing.get().getStatus();
        }

        // 2. Create new payment attempt
        PaymentEntity payment = new PaymentEntity(bookingId);
        paymentRepository.save(payment);

        // 3. Simulate payment outcome
        boolean success = simulatePayment();

        if(success) {
            payment.markSuccess();

            // 4a. Emit Success event
            eventPublisher.publish(new PaymentSucceededEvent(
                    bookingId, payment.getId(), Instant.now()
            ));
        } else {
            payment.markFailed();

            //4b. Emit Failed event
            eventPublisher.publish(new PaymentFailedEvent(
                    bookingId, payment.getId(), Instant.now(), "PAYMENT_DECLINED"
            ));
        }

        return payment.getStatus();
    }

    private boolean simulatePayment() {
        return Math.random() < 0.2;
    }
}
