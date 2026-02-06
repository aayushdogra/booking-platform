package com.booking.platform.entity;

import com.booking.platform.domain.BookingStatus;
import com.booking.platform.domain.RoomType;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.Duration;

@Entity
@Table(name = "bookings")
public class BookingEntity {

    private static final Duration HOLD_DURATION = Duration.ofMinutes(15);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userName;

    @Column(nullable = false)
    private String hotelName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoomType roomType;

    @Column(nullable = false)
    private Integer nights;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Column(nullable = false, unique = true)
    private String idempotencyKey;

    @Column(nullable = false)
    private Instant holdUntil;

    @Column(columnDefinition = "TEXT")
    private String failureReason;

    protected BookingEntity() {
    }

    public BookingEntity(String userName, String hotelName, RoomType roomType, Integer nights,
                         BookingStatus status, String idempotencyKey) {
        this.userName = userName;
        this.hotelName = hotelName;
        this.roomType = roomType;
        this.nights = nights;
        this.status = status;
        this.idempotencyKey = idempotencyKey;
        this.holdUntil = Instant.now().plus(HOLD_DURATION);
    }

    // lifecycle hooks
    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // EXPIRY
    public boolean expireIfNeeded(Instant now) {
        if(isExpired(now)) {
            changeStatus(BookingStatus.EXPIRED);
            return true;
        }

        return false;
    }

    public boolean isExpired(Instant now) {
        return status == BookingStatus.CREATED && holdUntil.isBefore(now);
    }

    // STATE MACHINE: Lifecycle transition
    private void changeStatus(BookingStatus newStatus) {
        if(!isValidTransition(this.status, newStatus)) {
            throw new IllegalStateException("Invalid booking state transition: " + this.status +
                    " -> " + newStatus);
        }

        this.status = newStatus;
    }

    private boolean isValidTransition(BookingStatus from, BookingStatus to) {
        if(from == to)
            return true; // idempotent transition

        return switch (from) {

            case CREATED ->
                    to == BookingStatus.CONFIRMED
                            || to == BookingStatus.CANCELLED
                            || to == BookingStatus.EXPIRED
                            || to == BookingStatus.PAYMENT_FAILED;

            case CONFIRMED ->
                    to == BookingStatus.REFUND_PENDING;

            case REFUND_PENDING ->
                    to == BookingStatus.REFUNDED
                            || to == BookingStatus.REFUND_FAILED;

            case CANCELLED, EXPIRED, REFUNDED, PAYMENT_FAILED, REFUND_FAILED ->
                    false; // terminal states
        };
    }

    // PAYMENT REQUEST
    public void requestPayment(Instant now) {
        expireIfNeeded(now);

        if (status == BookingStatus.EXPIRED) {
            throw new IllegalStateException("Booking has expired");
        }

        if (status == BookingStatus.CONFIRMED) {
            return; // idempotent
        }

        if (status != BookingStatus.CREATED) {
            throw new IllegalStateException("Invalid state for payment request: " + status);
        }
    }

    // CANCEL
    public boolean cancel(Instant now) {

        expireIfNeeded(now);

        return switch (status) {

            case CREATED -> {
                changeStatus(BookingStatus.CANCELLED);
                yield false; // no refund needed
            }

            case CONFIRMED -> {
                changeStatus(BookingStatus.REFUND_PENDING);
                yield true; // refund required
            }

            case CANCELLED -> false; // idempotent

            case EXPIRED ->
                    throw new IllegalStateException("Booking has expired");

            case REFUNDED ->
                    throw new IllegalStateException("Booking already refunded");

            case REFUND_PENDING ->
                    throw new IllegalStateException("Refund already in progress");

            default ->
                    throw new IllegalStateException("Invalid state for cancellation: " + status);
        };
    }

    // PAYMENT RESULT
    public void markPaymentSucceeded(Instant now) {
        expireIfNeeded(now);

        if(status == BookingStatus.EXPIRED) {
            throw new IllegalStateException("Cannot confirm expired booking");
        }

        if(status == BookingStatus.CONFIRMED) return;
        changeStatus(BookingStatus.CONFIRMED);
    }

    public void markPaymentFailed(String reason) {
        if(status == BookingStatus.PAYMENT_FAILED) return;

        this.failureReason = reason;
        changeStatus(BookingStatus.PAYMENT_FAILED);
    }

    // REFUND RESULT
    public void markRefundSucceeded() {
        if(status == BookingStatus.REFUNDED) return;
        changeStatus(BookingStatus.REFUNDED);
    }

    public void markRefundFailed(String reason) {
        if(status == BookingStatus.REFUND_FAILED) return;

        this.failureReason = reason;
        changeStatus(BookingStatus.REFUND_FAILED);
    }

    // getters
    public Long getId() {
        return id;
    }
    public String getUserName() {
        return userName;
    }
    public String getHotelName() {
        return hotelName;
    }
    public RoomType getRoomType() {
        return roomType;
    }
    public Integer getNights() {
        return nights;
    }
    public BookingStatus getStatus() {
        return status;
    }
    public Instant getCreatedAt() {
        return createdAt;
    }
    public Instant getUpdatedAt() { return updatedAt; }
    public String getIdempotencyKey() {
        return idempotencyKey;
    }
    public Instant getHoldUntil() {
        return holdUntil;
    }
    public String getFailureReason() {
        return failureReason;
    }
}
