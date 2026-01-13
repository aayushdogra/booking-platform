package com.booking.platform.service;

import com.booking.platform.domain.PaymentStatus;

public interface PaymentService {
    PaymentStatus initiatePayment(Long bookingId);
}
