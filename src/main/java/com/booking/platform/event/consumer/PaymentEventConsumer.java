package com.booking.platform.event.consumer;

import com.booking.platform.event.*;
import com.booking.platform.event.dlq.DeadLetterEvent;
import com.booking.platform.event.dlq.DeadLetterStore;
import com.booking.platform.service.RedisCoordinatorService;
import com.booking.platform.service.port.BookingEventHandler;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.Duration;
import java.util.Optional;

@Component
public class PaymentEventConsumer implements DomainEventConsumer {

    private static final int MAX_RETRIES = 3;
    private static final long BACKOFF_MS = 200;

    private final BookingEventHandler bookingEventHandler;
    private final DeadLetterStore deadLetterStore;
    private final RedisCoordinatorService redisCoordinatorService;

    public PaymentEventConsumer(BookingEventHandler bookingEventHandler,
                                DeadLetterStore deadLetterStore,
                                RedisCoordinatorService redisCoordinatorService) {
        this.bookingEventHandler = bookingEventHandler;
        this.deadLetterStore = deadLetterStore;
        this.redisCoordinatorService = redisCoordinatorService;
    }

    @Override
    public boolean supports(DomainEvent event) {
        return event instanceof PaymentResultEvent;
    }

    @Override
    public void consume(DomainEvent event) {

        if (event instanceof PaymentSucceededEvent successEvent) {
            processWithRetry(
                    successEvent,
                    "payment:success",
                    () -> bookingEventHandler
                            .confirmBookingFromPaymentEvent(successEvent.bookingId())
            );
            return;
        }

        if (event instanceof PaymentFailedEvent failedEvent) {
            processWithRetry(
                    failedEvent,
                    "payment:failed",
                    () -> bookingEventHandler
                            .markPaymentFailed(failedEvent.bookingId(), failedEvent.reason())
            );
        }
    }

    // Unified Processing Logic
    private void processWithRetry(PaymentResultEvent event, String typePrefix, Runnable handler) {

        String eventKey = "event:" + typePrefix + ":" + event.bookingId();

        // Idempotency guard
        if (!redisCoordinatorService.markEventIfAbsent(eventKey, Duration.ofHours(3))) {
            return;
        }

        String lockKey = "lock:" + typePrefix + ":" + event.bookingId();

        Optional<String> lockToken = redisCoordinatorService.acquireLock(lockKey, Duration.ofSeconds(10));
        if (lockToken.isEmpty()) {
            return;
        }

        try {
            handleWithRetry(event, typePrefix, handler);
        } finally {
            redisCoordinatorService.releaseLock(lockKey, lockToken.get());
        }
    }

    private void handleWithRetry(PaymentResultEvent event, String  typePrefix, Runnable handler) {

        String retryKey = "retry:" + typePrefix + ":" + event.bookingId();

        while (true) {
            long attempt = redisCoordinatorService.incrementRetry(retryKey, Duration.ofHours(1));

            if (attempt > MAX_RETRIES) {

                deadLetterStore.save(
                        new DeadLetterEvent(
                                event,
                                (int) attempt,
                                "RETRY_EXHAUSTED",
                                "Max retries exceeded",
                                Instant.now()
                        )
                );

                redisCoordinatorService.delete(retryKey);
                return;
            }

            try {
                handler.run();
                redisCoordinatorService.delete(retryKey);
                return; // success or safe no-op

            } catch (Exception ex) {

                RetryDecision decision = RetryClassifier.classify(ex);

                // Non-retryable -> immediate DLQ
                if (decision == RetryDecision.NON_RETRYABLE) {

                    deadLetterStore.save(
                            new DeadLetterEvent(
                                    event,
                                    (int) attempt,
                                    "NON_RETRYABLE",
                                    ex.getMessage(),
                                    Instant.now()
                            )
                    );

                    redisCoordinatorService.delete(retryKey);
                    return;
                }

                // Retryable failure
                backoff((int) attempt);
            }
        }
    }

    private void backoff(int attempt) {
        try {
            Thread.sleep(BACKOFF_MS * attempt);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
