package com.booking.platform.event;

public interface RefundResultEvent extends DomainEvent {
    Long bookingId();
    Long refundId();
}
