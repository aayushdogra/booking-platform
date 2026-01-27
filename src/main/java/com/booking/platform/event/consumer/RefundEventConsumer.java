package com.booking.platform.event.consumer;

import com.booking.platform.event.DomainEvent;
import com.booking.platform.event.DomainEventConsumer;
import com.booking.platform.event.RefundRequestedEvent;
import com.booking.platform.service.RefundService;
import org.springframework.stereotype.Component;

@Component
public class RefundEventConsumer implements DomainEventConsumer {

    private final RefundService refundService;

    public RefundEventConsumer(RefundService refundService) {
        this.refundService = refundService;
    }

    @Override
    public void consume(DomainEvent event) {

        if (event instanceof RefundRequestedEvent refundEvent) {
            refundService.processRefund(refundEvent.bookingId());
        }
    }

    @Override
    public boolean supports(DomainEvent event) {
        return event instanceof RefundRequestedEvent;
    }
}
