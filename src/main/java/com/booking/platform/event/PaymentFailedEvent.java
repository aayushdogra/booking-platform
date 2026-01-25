package com.booking.platform.event;

import java.time.Instant;

public record PaymentFailedEvent(
        Long bookingId, Long paymentId, Instant occurredAt, String reason
) implements PaymentResultEvent {}
