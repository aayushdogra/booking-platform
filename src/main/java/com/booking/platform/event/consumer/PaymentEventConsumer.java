package com.booking.platform.event.consumer;

import com.booking.platform.event.PaymentEvent;
import com.booking.platform.event.PaymentSucceededEvent;
import com.booking.platform.event.PaymentFailedEvent;
import com.booking.platform.service.BookingService;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventConsumer {

    private static final int MAX_RETRIES = 3;
    private static final long BACKOFF_MS = 200;

    private final BookingService bookingService;

    public PaymentEventConsumer(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    public void consume(PaymentEvent event) {

        if(event instanceof PaymentSucceededEvent successEvent) {
            handleWithRetry(successEvent.bookingId());
        }

        if(event instanceof PaymentFailedEvent failedEvent) {
            // Payment failure is a terminal fact -> never retry
            return;
        }
    }

    private void handleWithRetry(Long bookingId) {
        int attempt = 0;

        while (attempt < MAX_RETRIES) {
            try {
                attempt++;

                bookingService.confirmBookingFromPaymentEvent(bookingId);
                return; // success or safe no-op
            } catch (ObjectOptimisticLockingFailureException | TransientDataAccessResourceException ex) {
                // Retryable failure
                backoff(attempt);
            } catch (Exception ex) {
                // Non-retryable (business or unexpected)
                return;
            }
        }

        // Retry exhausted -> DLQ later
    }

    private void backoff(int attempt) {
        try {
            Thread.sleep(BACKOFF_MS * attempt);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
