package com.booking.platform.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InMemoryEventPublisher implements EventPublisher {

    private static final Logger LOG = LoggerFactory.getLogger(InMemoryEventPublisher.class);

    private final List<DomainEventConsumer> consumers;

    public InMemoryEventPublisher(List<DomainEventConsumer> consumers) {
        this.consumers = consumers;
    }

    @Override
    public void publish(DomainEvent event) {
        LOG.info("EVENT EMITTED: {}", event);

        for(DomainEventConsumer consumer : consumers) {
            consumer.consume(event);
        }
    }
}
