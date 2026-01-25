package com.booking.platform.event.dlq;

import com.booking.platform.event.PaymentResultEvent;

import java.time.Instant;

public record DeadLetterEvent(
        PaymentResultEvent originalEvent,
        int retryCount,
        String failureReason,
        String errorMessage,
        Instant failedAt
) {}
