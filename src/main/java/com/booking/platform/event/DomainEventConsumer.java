package com.booking.platform.event;

public interface DomainEventConsumer {
    void consume(DomainEvent event);
}
