package com.booking.platform.event.consumer;

import com.booking.platform.event.DomainEvent;
import com.booking.platform.event.DomainEventConsumer;
import com.booking.platform.event.PaymentRequestedEvent;
import com.booking.platform.service.PaymentService;
import org.springframework.stereotype.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

@Component
public class PaymentRequestConsumer implements DomainEventConsumer {

    private final PaymentService paymentService;
    private static final Logger log = LoggerFactory.getLogger(RefundRequestConsumer.class);

    public PaymentRequestConsumer(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Override
    public void consume(DomainEvent event) {
        PaymentRequestedEvent requested = (PaymentRequestedEvent) event;

        MDC.put("bookingId", String.valueOf(requested.bookingId()));
        MDC.put("eventType", event.getClass().getSimpleName());

        try {
            log.info("Processing PaymentRequestedEvent");
            paymentService.initiatePayment(requested.bookingId());

        } finally {
            MDC.clear();
        }
    }

    @Override
    public boolean supports(DomainEvent event) {
        return event instanceof PaymentRequestedEvent;
    }
}
