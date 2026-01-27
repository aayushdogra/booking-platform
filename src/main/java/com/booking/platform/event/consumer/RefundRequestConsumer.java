package com.booking.platform.event.consumer;

import com.booking.platform.domain.RefundStatus;
import com.booking.platform.entity.RefundEntity;
import com.booking.platform.event.*;
import com.booking.platform.service.RefundService;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class RefundRequestConsumer implements DomainEventConsumer {

    private final RefundService refundService;

    public RefundRequestConsumer(RefundService refundService) {
        this.refundService = refundService;
    }

    @Override
    public boolean supports(DomainEvent event) {
        return event instanceof RefundRequestedEvent;
    }

    @Override
    public void consume(DomainEvent event) {
        if (event instanceof RefundRequestedEvent refundEvent) {
            refundService.initiateRefund(refundEvent.bookingId());
        }
    }
}