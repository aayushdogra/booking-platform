package com.booking.platform.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class InMemoryEventPublisher implements EventPublisher {

    private static final Logger LOG = LoggerFactory.getLogger(InMemoryEventPublisher.class);

    private final List<DomainEventConsumer> consumers;

    // Fixed thread pool for async execution
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    public InMemoryEventPublisher(List<DomainEventConsumer> consumers) {
        this.consumers = consumers;
    }

    @Override
    public void publish(DomainEvent event) {
        LOG.info("EVENT EMITTED: {}", event);

        for(DomainEventConsumer consumer : consumers) {

            if(!consumer.supports(event)) {
                continue;
            }

            executor.submit(() -> {
                try {
                    LOG.info("Dispatching event {} to consumer {}",
                            event.getClass().getSimpleName(),
                            consumer.getClass().getSimpleName());

                    consumer.consume(event);

                } catch (Exception ex) {
                    LOG.error("Error processing event {} in consumer {}",
                            event.getClass().getSimpleName(),
                            consumer.getClass().getSimpleName(),
                            ex);
                }
            });
        }
    }

    @PreDestroy
    public void shutdown() {
        LOG.info("Shutting down async event executor");
        executor.shutdown();
    }
}
