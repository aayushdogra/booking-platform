package com.booking.platform.event.consumer;

import com.booking.platform.event.DomainEvent;
import com.booking.platform.event.DomainEventConsumer;
import com.booking.platform.event.RefundRequestedEvent;
import com.booking.platform.service.RedisGuardService;
import com.booking.platform.service.RefundService;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RefundEventConsumer implements DomainEventConsumer {

    private static final Duration EVENT_TTL = Duration.ofHours(4);

    private final RefundService refundService;
    private final RedisGuardService redisGuardService;

    public RefundEventConsumer(RefundService refundService,  RedisGuardService redisGuardService) {
        this.refundService = refundService;
        this.redisGuardService = redisGuardService;
    }

    @Override
    public void consume(DomainEvent event) {
        if (!(event instanceof RefundRequestedEvent refundEvent)) {
            return;
        }

        String redisKey = "refund:event:" + refundEvent.eventId();

        // Idempotency guard
        boolean acquired = redisGuardService.acquireEventLock(redisKey, EVENT_TTL);

        if (!acquired) {
            // Event already processed (or in progress)
            return;
        }

        refundService.processRefund(refundEvent.bookingId());
    }

    @Override
    public boolean supports(DomainEvent event) {
        return event instanceof RefundRequestedEvent;
    }
}
