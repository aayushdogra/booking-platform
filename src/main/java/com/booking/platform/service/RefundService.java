package com.booking.platform.service;

import com.booking.platform.entity.RefundEntity;

public interface RefundService {
    RefundEntity initiateRefund(Long bookingId);
}