package com.booking.platform.event;

import java.time.Instant;
import java.util.UUID;

public record RefundRequestedEvent(String eventId, Long bookingId, Instant occurredAt)
        implements DomainEvent {

    public  RefundRequestedEvent(Long bookingId, Instant occurredAt) {
        this(UUID.randomUUID().toString(), bookingId, occurredAt);
    }
}
