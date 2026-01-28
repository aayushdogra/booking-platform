package com.booking.platform.event;

import java.time.Instant;

public record PaymentSucceededEvent(Long bookingId, Long paymentId, Instant occurredAt)
        implements PaymentResultEvent {

}
