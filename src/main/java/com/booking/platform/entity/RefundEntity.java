package com.booking.platform.entity;

import com.booking.platform.domain.RefundStatus;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "refunds",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"booking_id"})
        }
)
public class RefundEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RefundStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    protected RefundEntity() {}

    public RefundEntity(Long bookingId) {
        this.bookingId = bookingId;
        this.status = RefundStatus.INITIATED;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void markSuccess() {
        if (status == RefundStatus.SUCCESS) return;

        if (status != RefundStatus.INITIATED) {
            throw new IllegalStateException("Invalid refund transition to SUCCESS");
        }

        this.status = RefundStatus.SUCCESS;
    }

    public void markFailed() {
        if (status == RefundStatus.FAILED) return;

        if (status != RefundStatus.INITIATED) {
            throw new IllegalStateException("Invalid refund transition to FAILED");
        }

        this.status = RefundStatus.FAILED;
    }

    public Long getId() {
        return id;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public RefundStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
