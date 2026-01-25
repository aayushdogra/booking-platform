package com.booking.platform.event;

public interface PaymentResultEvent extends DomainEvent {
    Long bookingId();
    Long paymentId();
}
