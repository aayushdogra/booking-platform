package com.booking.platform.event.dlq;

import com.booking.platform.event.PaymentEvent;

import java.time.Instant;

public record DeadLetterEvent(
        PaymentEvent originalEvent,
        int retryCount,
        String failureReason,
        String errorMessage,
        Instant failedAt
) {}
