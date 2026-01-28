package com.booking.platform.service;

import com.booking.platform.domain.RefundStatus;

public interface RefundService {
    RefundStatus initiateRefund(Long bookingId);
}