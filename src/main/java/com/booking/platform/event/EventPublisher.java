package com.booking.platform.event;

public interface EventPublisher {
    void publish(DomainEvent event);
}
