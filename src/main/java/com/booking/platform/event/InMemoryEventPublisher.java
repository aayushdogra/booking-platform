package com.booking.platform.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class InMemoryEventPublisher implements EventPublisher {

    private static final Logger LOG = LoggerFactory.getLogger(InMemoryEventPublisher.class);

    @Override
    public void publish(PaymentEvent event) {
        LOG.info("EVENT EMITTED: {}", event);
    }
}
