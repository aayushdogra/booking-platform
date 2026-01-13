package com.booking.platform.service;

import com.booking.platform.domain.PaymentStatus;
import com.booking.platform.entity.PaymentEntity;
import com.booking.platform.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentServiceImpl(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Override
    @Transactional
    public PaymentStatus initiatePayment(Long bookingId) {
        PaymentEntity payment = new PaymentEntity(bookingId);

        // Simulate outcome
        boolean success = Math.random() > 0.3;

        if(success) {
            payment.markSuccess();
        } else {
            payment.markFailed();
        }

        paymentRepository.save(payment);
        return payment.getStatus();
    }
}
