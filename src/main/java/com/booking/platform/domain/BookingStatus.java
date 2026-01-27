package com.booking.platform.domain;

public enum BookingStatus {
    CREATED, // Initial hold
    CONFIRMED, // payment successful
    CANCELLED, // user/system cancelled
    EXPIRED, // timeout/no payment
    REFUND_PENDING,  // Refund initiated
    REFUNDED         // Refund completed
}