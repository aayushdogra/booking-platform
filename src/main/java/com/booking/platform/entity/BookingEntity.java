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

    protected BookingEntity() {
    }

    public BookingEntity(String userName, String hotelName, RoomType roomType, Integer nights,
                         BookingStatus status, Instant createdAt) {
        this.userName = userName;
        this.hotelName = hotelName;
        this.roomType = roomType;
        this.nights = nights;
        this.status = status;
        this.createdAt = createdAt;
    }

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
}
