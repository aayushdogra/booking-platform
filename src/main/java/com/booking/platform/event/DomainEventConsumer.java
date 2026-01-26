package com.booking.platform.event;

public interface DomainEventConsumer {

    boolean supports(DomainEvent event);

    void consume(DomainEvent event);
}
