package com.booking.platform.domain;

public enum BookingStatus {
    CREATED, // Initial hold
    CONFIRMED, // payment successful
    CANCELLED, // user/system cancelled

    PAYMENT_FAILED,

    REFUND_PENDING,  // Refund initiated
    REFUNDED,       // Refund completed
    REFUND_FAILED, // Refund failed

    EXPIRED // timeout/no payment
}