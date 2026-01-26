package com.booking.platform.event.consumer;

import com.booking.platform.event.DomainEvent;
import com.booking.platform.event.DomainEventConsumer;
import com.booking.platform.event.PaymentRequestedEvent;
import com.booking.platform.service.PaymentService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class PaymentRequestConsumer implements DomainEventConsumer {

    private final PaymentService paymentService;

    public PaymentRequestConsumer(@Lazy PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Override
    public void consume(DomainEvent event) {

        if(event instanceof PaymentRequestedEvent requested) {
            paymentService.initiatePayment(requested.bookingId());
        }
    }

    @Override
    public boolean supports(DomainEvent event) {
        return event instanceof PaymentRequestedEvent;
    }
}
