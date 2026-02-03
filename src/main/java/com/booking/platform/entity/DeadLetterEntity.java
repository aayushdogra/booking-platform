package com.booking.platform.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "dead_letter_events",
        indexes = {
                @Index(name = "idx_dead_letter_aggregate", columnList = "aggregateId"),
                @Index(name = "idx_dead_letter_failed_at", columnList = "failedAt")
        })
public class DeadLetterEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private Long aggregateId; // bookingId

    @Lob
    @Column(nullable = false)
    private String payload; // serialized JSON

    @Column(nullable = false)
    private int retryCount;

    @Column(nullable = false)
    private String failureReason;

    @Lob
    @Column(nullable = false)
    private String errorMessage;

    @Column(nullable = false)
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
