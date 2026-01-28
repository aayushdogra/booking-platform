package com.booking.platform.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "dead_letter_events")
public class DeadLetterEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String eventType;

    private Long aggregateId; // bookingId

    @Column(columnDefinition = "TEXT")
    private String payload; // serialized JSON

    private int retryCount;

    private String failureReason;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    private Instant failedAt;

    protected DeadLetterEntity() {}

    public DeadLetterEntity(
            String eventType,
            Long aggregateId,
            String payload,
            int retryCount,
            String failureReason,
            String errorMessage,
            Instant failedAt
    ) {
        this.eventType = eventType;
        this.aggregateId = aggregateId;
        this.payload = payload;
        this.retryCount = retryCount;
        this.failureReason = failureReason;
        this.errorMessage = errorMessage;
        this.failedAt = failedAt;
    }
}
