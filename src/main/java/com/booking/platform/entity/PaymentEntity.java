package com.booking.platform.entity;

import com.booking.platform.domain.PaymentStatus;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "payments",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"booking_id"})
        }
)
public class PaymentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    protected PaymentEntity() {}

    public PaymentEntity(Long bookingId) {
        this.bookingId = bookingId;
        this.status = PaymentStatus.INITIATED;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void markSuccess() {
        if (status == PaymentStatus.SUCCESS) return;

        if (status != PaymentStatus.INITIATED) {
            throw new IllegalStateException("Invalid payment transition: " + status + " -> SUCCESS");
        }

        this.status = PaymentStatus.SUCCESS;
    }

    public void markFailed() {
        if (status == PaymentStatus.FAILED) return;

        if (status != PaymentStatus.INITIATED) {
            throw new IllegalStateException("Invalid payment transition: " + status + " -> FAILED");
        }

        this.status = PaymentStatus.FAILED;
    }

    // Getters
    public Long getId() {
        return this.id;
    }

    public Long getBookingId() {
        return this.bookingId;
    }

    public PaymentStatus getStatus() {
        return this.status;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
