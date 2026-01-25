package com.booking.platform.event.dlq;

public interface DeadLetterStore {
    void save(DeadLetterEvent event);
}
