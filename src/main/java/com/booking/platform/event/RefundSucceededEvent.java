package com.booking.platform.event;

import java.time.Instant;

public record RefundSucceededEvent(Long bookingId, Long refundId, Instant occurredAt)
        implements RefundResultEvent {

}
