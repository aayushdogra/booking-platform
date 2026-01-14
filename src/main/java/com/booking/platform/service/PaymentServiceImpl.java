package com.booking.platform.service;

import com.booking.platform.domain.PaymentStatus;
import com.booking.platform.entity.PaymentEntity;
import com.booking.platform.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentServiceImpl(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Override
    @Transactional
    public PaymentStatus initiatePayment(Long bookingId) {

        // 1. Idempotency check
        Optional<PaymentEntity> existing = paymentRepository.findByBookingId(bookingId);

        if(existing.isPresent()) {
            return  existing.get().getStatus();
        }

        // 2. Create new payment attempt
        PaymentEntity payment = new PaymentEntity(bookingId);
        paymentRepository.save(payment);

        // 3. Simulate payment outcome
        boolean success = simulatePayment();

        if(success) {
            payment.markSuccess();
        } else {
            payment.markFailed();
        }

        return payment.getStatus();
    }

    private boolean simulatePayment() {
        return Math.random() < 0.5;
    }
}
