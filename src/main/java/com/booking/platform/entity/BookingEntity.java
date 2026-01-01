package com.booking.platform.entity;

import com.booking.platform.domain.BookingStatus;
import com.booking.platform.domain.RoomType;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "bookings")
public class BookingEntity {
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

    // Lifecycle transition
    public void changeStatus(BookingStatus newStatus) {
        if(!isValidTransition(this.status, newStatus)) {
            throw new IllegalStateException("Invalid booking state transition: " + this.status +
                    " -> " + newStatus);
        }

        this.status = newStatus;
    }

    private boolean  isValidTransition(BookingStatus from, BookingStatus to) {
        return switch (from) {
            case CREATED -> to == BookingStatus.CONFIRMED || to == BookingStatus.CANCELLED || to == BookingStatus.EXPIRED;
            default -> false;
        };
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
}
