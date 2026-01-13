package com.booking.platform.entity;

import com.booking.platform.domain.PaymentStatus;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "payments")
public class PaymentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long bookingId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected PaymentEntity() {}

    public PaymentEntity(Long bookingId) {
        this.bookingId = bookingId;
        this.status = PaymentStatus.INITIATED;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public void markSuccess() {
        this.status = PaymentStatus.SUCCESS;
    }

    public void markFailed() {
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
}
