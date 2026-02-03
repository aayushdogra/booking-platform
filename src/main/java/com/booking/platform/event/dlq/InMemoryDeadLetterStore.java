package com.booking.platform.event.dlq;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class InMemoryDeadLetterStore implements DeadLetterStore {

    private final List<DeadLetterEvent> store = new CopyOnWriteArrayList<>();

    @Override
    public void save(DeadLetterEvent event) {
        store.add(event);
        System.err.println("DLQ STORED: " + event);
    }

    public List<DeadLetterEvent> getStore() {
        return List.copyOf(store);
    }
}
