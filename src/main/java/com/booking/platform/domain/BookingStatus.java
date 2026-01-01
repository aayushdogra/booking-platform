package com.booking.platform.domain;

public enum BookingStatus {
    CREATED, // Initial
    CONFIRMED, // payment success
    CANCELLED, // user/system cancelled
    EXPIRED // timeout/no payment
}