package com.booking.platform.event;

import java.time.Instant;

public interface PaymentEvent {
    Long bookingId();
    Long paymentId();
    Instant occurredAt();
}
