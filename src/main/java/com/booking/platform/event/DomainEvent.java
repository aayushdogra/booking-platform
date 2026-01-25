package com.booking.platform.event;

import java.time.Instant;

public interface DomainEvent {
    Instant occurredAt();
}
