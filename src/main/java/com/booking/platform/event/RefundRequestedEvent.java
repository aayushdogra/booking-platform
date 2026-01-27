package com.booking.platform.event;

import java.time.Instant;

public record RefundRequestedEvent(Long bookingId, Instant occurredAt) implements DomainEvent {}
