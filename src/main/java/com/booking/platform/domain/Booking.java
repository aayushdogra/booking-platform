package com.booking.platform.domain;

import java.time.Instant;
import java.util.UUID;

public class Booking {
    private final UUID id;
    private final UUID userId;
    private final UUID hotelId;
    private final RoomType roomType;
    private final int nights;
    private final BookingStatus status;
    private final Instant createdAt;

    public Booking(UUID id, UUID userId, UUID hotelId, RoomType roomType, int nights, BookingStatus status, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.hotelId = hotelId;
        this.roomType = roomType;
        this.nights = nights;
        this.status = status;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getHotelId() {
        return hotelId;
    }

    public RoomType getRoomType() {
        return roomType;
    }

    public int getNights() {
        return nights;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}