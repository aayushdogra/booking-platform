package com.booking.platform.event.consumer;

import com.booking.platform.event.PaymentEvent;
import com.booking.platform.event.PaymentSucceededEvent;
import com.booking.platform.event.PaymentFailedEvent;
import com.booking.platform.service.BookingService;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventConsumer {

    private final BookingService bookingService;

    public PaymentEventConsumer(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    public void consume(PaymentEvent event) {

        if(event instanceof PaymentSucceededEvent successEvent) {
            bookingService.confirmBookingFromPaymentEvent(successEvent.bookingId());
        }

        if(event instanceof PaymentFailedEvent failedEvent) {

        }
    }
}
