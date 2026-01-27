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

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    protected RefundEntity() {}

    public RefundEntity(Long bookingId) {
        this.bookingId = bookingId;
        this.status = RefundStatus.INITIATED;
    }

    public void markSuccess() {
        this.status = RefundStatus.SUCCESS;
    }

    public void markFailed() {
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
}
