package com.booking.platform.event;

import java.time.Instant;

public record PaymentRequestedEvent(Long bookingId, Instant occurredAt) implements DomainEvent {

}