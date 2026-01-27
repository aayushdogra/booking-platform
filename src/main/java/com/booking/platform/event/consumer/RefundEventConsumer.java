package com.booking.platform.event.consumer;

import com.booking.platform.event.*;
import com.booking.platform.event.dlq.DeadLetterEvent;
import com.booking.platform.event.dlq.DeadLetterStore;
import com.booking.platform.service.RedisCoordinatorService;
import com.booking.platform.service.BookingService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class RefundEventConsumer implements DomainEventConsumer {

    private static final int MAX_RETRIES = 3;
    private static final long BACKOFF_MS = 200;

    private final BookingService bookingService;
    private final DeadLetterStore deadLetterStore;
    private final RedisCoordinatorService redisCoordinatorService;

    public RefundEventConsumer(@Lazy BookingService bookingService,
                               DeadLetterStore deadLetterStore,
                               RedisCoordinatorService redisCoordinatorService) {
        this.bookingService = bookingService;
        this.deadLetterStore = deadLetterStore;
        this.redisCoordinatorService = redisCoordinatorService;
    }

    @Override
    public boolean supports(DomainEvent event) {
        return event instanceof RefundSucceededEvent
                || event instanceof RefundFailedEvent;
    }

    @Override
    public void consume(DomainEvent event) {

        if (event instanceof RefundSucceededEvent successEvent) {
            handleSuccess(successEvent);
            return;
        }

        if (event instanceof RefundFailedEvent failedEvent) {
            handleFailureWithRetry(failedEvent);
        }
    }

    private void handleSuccess(RefundSucceededEvent event) {

        String eventKey = "refund:success:" + event.bookingId();
        boolean firstTime = redisCoordinatorService.markEventIfAbsent(eventKey, Duration.ofHours(3));

        if (!firstTime) {
            return;
        }

        bookingService.completeRefundFromRefundEvent(event.bookingId());
    }

    private void handleFailureWithRetry(RefundFailedEvent event) {

        String eventKey = "refund:failed:" + event.bookingId();
        boolean firstTime = redisCoordinatorService.markEventIfAbsent(eventKey, Duration.ofHours(3));

        if (!firstTime) {
            return;
        }

        int attempt = 0;

        while (attempt < MAX_RETRIES) {
            try {
                attempt++;
                bookingService.handleRefundFailureFromRefundEvent(event.bookingId(), event.reason());
                return;

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

                backoff(attempt);
            }
        }

        // Retry exhausted → for now we just persist failure
        deadLetterStore.save(
                new DeadLetterEvent(
                        event,
                        MAX_RETRIES,
                        "REFUND_RETRY_EXHAUSTED",
                        "Refund failed after max retries",
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
