package com.booking.platform.event.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import com.booking.platform.event.*;
import com.booking.platform.event.dlq.DeadLetterEvent;
import com.booking.platform.event.dlq.DeadLetterStore;
import com.booking.platform.service.RedisCoordinatorService;
import com.booking.platform.service.metrics.BookingMetrics;
import com.booking.platform.service.port.BookingEventHandler;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
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
    private final BookingMetrics bookingMetrics;
    private final MeterRegistry meterRegistry;

    private static final Logger log = LoggerFactory.getLogger(PaymentEventConsumer.class);

    public PaymentEventConsumer(BookingEventHandler bookingEventHandler,
                                DeadLetterStore deadLetterStore,
                                RedisCoordinatorService redisCoordinatorService,
                                BookingMetrics bookingMetrics, MeterRegistry meterRegistry) {
        this.bookingEventHandler = bookingEventHandler;
        this.deadLetterStore = deadLetterStore;
        this.redisCoordinatorService = redisCoordinatorService;
        this.bookingMetrics = bookingMetrics;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public boolean supports(DomainEvent event) {
        return event instanceof PaymentResultEvent;
    }

    @Override
    public void consume(DomainEvent event) {
        MDC.put("bookingId", String.valueOf(((PaymentResultEvent) event).bookingId()));
        MDC.put("eventType", event.getClass().getSimpleName());

        Timer.Sample sample = bookingMetrics.startTimer(meterRegistry);

        try {
            log.info("Processing payment result event");

            if (event instanceof PaymentSucceededEvent successEvent) {
                log.info("Payment succeeded. Confirming booking.");

                processWithRetry(
                        successEvent,
                        "payment:success",
                        () -> bookingEventHandler
                                .confirmBookingFromPaymentEvent(successEvent.bookingId())
                );

                bookingMetrics.incrementPaymentSuccess();
                return;
            }

            if (event instanceof PaymentFailedEvent failedEvent) {
                log.warn("Payment failed. Marking booking as PAYMENT_FAILED. Reason={}",
                        failedEvent.reason());

                processWithRetry(
                        failedEvent,
                        "payment:failed",
                        () -> bookingEventHandler
                                .markPaymentFailed(failedEvent.bookingId(), failedEvent.reason())
                );

                bookingMetrics.incrementPaymentFailure();
            }
        } catch (Exception ex) {
            log.error("Unexpected error while processing payment event", ex);
            throw ex;

        } finally {
            bookingMetrics.stopPaymentTimer(sample);
            MDC.clear();
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
                log.error("Max retries exceeded. Sending event to DLQ");

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

                log.warn("Retry attempt {} for payment event", attempt);
                bookingMetrics.incrementPaymentRetry();
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
