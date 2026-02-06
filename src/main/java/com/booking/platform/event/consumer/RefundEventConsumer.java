package com.booking.platform.event.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import com.booking.platform.event.*;
import com.booking.platform.event.dlq.DeadLetterEvent;
import com.booking.platform.event.dlq.DeadLetterStore;
import com.booking.platform.service.RedisCoordinatorService;
import com.booking.platform.service.metrics.BookingMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import com.booking.platform.service.port.BookingEventHandler;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Component
public class RefundEventConsumer implements DomainEventConsumer {

    private static final int MAX_RETRIES = 3;
    private static final long BACKOFF_MS = 200;

    private final BookingEventHandler bookingEventHandler;
    private final DeadLetterStore deadLetterStore;
    private final RedisCoordinatorService redisCoordinatorService;
    private final BookingMetrics bookingMetrics;
    private final MeterRegistry meterRegistry;

    private static final Logger log = LoggerFactory.getLogger(RefundEventConsumer.class);

    public RefundEventConsumer(BookingEventHandler bookingEventHandler,
                               DeadLetterStore deadLetterStore,
                               RedisCoordinatorService redisCoordinatorService,
                               BookingMetrics bookingMetrics,
                               MeterRegistry meterRegistry) {
        this.bookingEventHandler = bookingEventHandler;
        this.deadLetterStore = deadLetterStore;
        this.redisCoordinatorService = redisCoordinatorService;
        this.bookingMetrics = bookingMetrics;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public boolean supports(DomainEvent event) {
        return event instanceof RefundSucceededEvent
                || event instanceof RefundFailedEvent;
    }

    @Override
    public void consume(DomainEvent event) {
        MDC.put("bookingId", String.valueOf(((RefundResultEvent) event).bookingId()));
        MDC.put("eventType", event.getClass().getSimpleName());

        Timer.Sample sample = bookingMetrics.startTimer(meterRegistry);

        try {
            log.info("Processing refund result event");

            if (event instanceof RefundSucceededEvent successEvent) {
                log.info("Refund succeeded. Completing refund.");

                processWithRetry(
                        successEvent,
                        "refund:success",
                        () -> bookingEventHandler
                                .completeRefundFromRefundEvent(successEvent.bookingId())
                );

                bookingMetrics.incrementRefundSuccess();
                return;
            }

            if (event instanceof RefundFailedEvent failedEvent) {
                log.warn("Refund failed. Marking booking REFUND_FAILED. Reason={}",
                        failedEvent.reason());

                processWithRetry(
                        failedEvent,
                        "refund:failed",
                        () -> bookingEventHandler
                                .markRefundFailed(failedEvent.bookingId(), failedEvent.reason())
                );

                bookingMetrics.incrementRefundFailure();
            }
        } catch (Exception ex) {
            log.error("Unexpected error while processing refund event", ex);
            throw ex;
        } finally {
            bookingMetrics.stopRefundTimer(sample);
            MDC.clear();
        }
    }

    private void processWithRetry(RefundResultEvent event,
                                  String prefix,
                                  Runnable handler) {

        String eventKey = "event:" + prefix + ":" + event.bookingId();

        if (!redisCoordinatorService.markEventIfAbsent(eventKey, Duration.ofHours(3))) {
            return;
        }

        String lockKey = "lock:" + prefix + ":" + event.bookingId();

        Optional<String> lockToken = redisCoordinatorService.acquireLock(lockKey, Duration.ofSeconds(10));
        if (lockToken.isEmpty()) {
            return;
        }

        try {
            handleWithRetry(event, prefix, handler);
        } finally {
            redisCoordinatorService.releaseLock(lockKey,  lockToken.get());
        }
    }

    private void handleWithRetry(RefundResultEvent event, String prefix, Runnable handler) {

        String retryKey = "retry:" + prefix + ":" + event.bookingId();

        while(true) {

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
                return;

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

                log.warn("Retry attempt {} for refund event", attempt);
                bookingMetrics.incrementRefundRetry();
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
