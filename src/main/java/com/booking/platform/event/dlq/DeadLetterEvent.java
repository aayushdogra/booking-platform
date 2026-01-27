package com.booking.platform.event.dlq;

import com.booking.platform.event.DomainEvent;
import com.booking.platform.event.PaymentResultEvent;

import java.time.Instant;

public record DeadLetterEvent(
        DomainEvent originalEvent,
        int retryCount,
        String failureReason,
        String errorMessage,
        Instant failedAt
) {}
