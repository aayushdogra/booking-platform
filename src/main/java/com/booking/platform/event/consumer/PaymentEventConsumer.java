package com.booking.platform.event.consumer;

import com.booking.platform.event.*;
import com.booking.platform.event.dlq.DeadLetterEvent;
import com.booking.platform.event.dlq.DeadLetterStore;
import com.booking.platform.service.BookingService;
import com.booking.platform.service.RedisCoordinatorService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.Duration;

@Component
public class PaymentEventConsumer implements DomainEventConsumer {

    private static final int MAX_RETRIES = 3;
    private static final long BACKOFF_MS = 200;

    private final BookingService bookingService;
    private final DeadLetterStore deadLetterStore;
    private final RedisCoordinatorService redisCoordinatorService;

    public PaymentEventConsumer(@Lazy BookingService bookingService,
                                DeadLetterStore deadLetterStore,
                                RedisCoordinatorService redisCoordinatorService) {
        this.bookingService = bookingService;
        this.deadLetterStore = deadLetterStore;
        this.redisCoordinatorService = redisCoordinatorService;
    }

    @Override
    public boolean supports(DomainEvent event) {
        return event instanceof PaymentSucceededEvent
                || event instanceof PaymentFailedEvent;
    }

    @Override
    public void consume(DomainEvent event) {

        if(event instanceof PaymentSucceededEvent successEvent) {
            handleWithRetry(successEvent);
            return;
        }

        if(event instanceof PaymentFailedEvent failedEvent) {
            // Payment failure is a terminal fact -> never retry
            return;
        }
    }

    private void handleWithRetry(PaymentSucceededEvent event) {
        String eventKey = "payment:event:" + event.bookingId();

        // If event already processed, skip
        boolean firstTime = redisCoordinatorService.markEventIfAbsent(eventKey, Duration.ofHours(3));

        if(!firstTime) {
            return; // duplicate event
        }

        int attempt = 0;

        while (attempt < MAX_RETRIES) {
            try {
                attempt++;

                bookingService.confirmBookingFromPaymentEvent(event.bookingId());
                return; // success or safe no-op

            } catch (Exception ex) {

                RetryDecision decision = RetryClassifier.classify(ex);

                // Non-retryable -> immediate DLQ
                if (decision == RetryDecision.NON_RETRYABLE) {

                    deadLetterStore.save(
                            new DeadLetterEvent(
                                    event,
                                    attempt,
                                    "NON_RETRYABLE",
                                    ex.getMessage(),
                                    Instant.now()
                            )
                    );

                    return;
                }

                // Retryable failure
                backoff(attempt);
            }
        }

        // Retry exhausted -> DLQ
        deadLetterStore.save(
                new DeadLetterEvent(
                        event,
                        MAX_RETRIES,
                        "RETRY_EXHAUSTED",
                        "Max retries exceeded",
                        Instant.now()
                )
        );
    }

    private void backoff(int attempt) {
        try {
            Thread.sleep(BACKOFF_MS * attempt);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
