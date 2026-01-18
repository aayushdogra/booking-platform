package com.booking.platform.event;

import com.booking.platform.event.consumer.PaymentEventConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class InMemoryEventPublisher implements EventPublisher {

    private static final Logger LOG = LoggerFactory.getLogger(InMemoryEventPublisher.class);
    private final PaymentEventConsumer consumer;

    public InMemoryEventPublisher(PaymentEventConsumer consumer) {
        this.consumer = consumer;
    }

    @Override
    public void publish(PaymentEvent event) {
        LOG.info("EVENT EMITTED: {}", event);
        consumer.consume(event); // synchronous, single-threaded
    }
}
