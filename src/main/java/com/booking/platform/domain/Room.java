package com.booking.platform.domain;

import java.math.BigDecimal;
import java.util.UUID;

public class Room {
    private final UUID id;
    private final UUID hotelId;
    private final RoomType type;
    private final BigDecimal pricePerNight;

    public Room(UUID id, UUID hotelId, RoomType type, BigDecimal pricePerNight) {
        this.id = id;
        this.hotelId = hotelId;
        this.type = type;
        this.pricePerNight = pricePerNight;
    }

    public UUID getId() {
        return id;
    }

    public UUID getHotelId() {
        return hotelId;
    }

    public RoomType getType() {
        return type;
    }

    public BigDecimal getPricePerNight() {
        return pricePerNight;
    }
}