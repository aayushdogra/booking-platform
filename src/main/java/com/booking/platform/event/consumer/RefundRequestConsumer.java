package com.booking.platform.event.consumer;

import com.booking.platform.event.DomainEvent;
import com.booking.platform.event.DomainEventConsumer;
import com.booking.platform.event.RefundRequestedEvent;
import com.booking.platform.service.RefundService;
import org.springframework.stereotype.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

@Component
public class RefundRequestConsumer implements DomainEventConsumer {

    private final RefundService refundService;
    private static final Logger log = LoggerFactory.getLogger(RefundRequestConsumer.class);


    public RefundRequestConsumer(RefundService refundService) {
        this.refundService = refundService;
    }

    @Override
    public void consume(DomainEvent event) {
        RefundRequestedEvent requested = (RefundRequestedEvent) event;

        MDC.put("bookingId", String.valueOf(requested.bookingId()));
        MDC.put("eventType", event.getClass().getSimpleName());

        try {
            log.info("Processing RefundRequestedEvent");
            refundService.initiateRefund(requested.bookingId());

        } finally {
            MDC.clear();
        }
    }

    @Override
    public boolean supports(DomainEvent event) {
        return event instanceof RefundRequestedEvent;
    }
}