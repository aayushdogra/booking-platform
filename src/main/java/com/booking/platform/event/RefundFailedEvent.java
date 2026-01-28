package com.booking.platform.event;

import java.time.Instant;

public record RefundFailedEvent(Long bookingId, Long refundId, Instant occurredAt, String reason)
        implements RefundResultEvent {

}
