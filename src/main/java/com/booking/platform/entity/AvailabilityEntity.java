package com.booking.platform.entity;

import com.booking.platform.domain.RoomType;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(
        name = "availability",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"hotel_name", "room_type", "date"})
        }
)
public class AvailabilityEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hotel_name", nullable = false)
    private String hotelName;

    @Enumerated(EnumType.STRING)
    @Column(name = "room_type", nullable = false)
    private RoomType roomType;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private Integer totalRooms;

    @Column(nullable = false)
    private Integer availableRooms;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected AvailabilityEntity() {}

    public AvailabilityEntity(String hotelName, RoomType roomType, LocalDate date,  Integer totalRooms, Integer availableRooms) {
        this.hotelName = hotelName;
        this.roomType = roomType;
        this.date = date;
        this.totalRooms = totalRooms;
        this.availableRooms = availableRooms;
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

    // Domain behaviour
    public boolean hasAvailability() {
        return availableRooms > 0;
    }

    public void decrement() {
        if (availableRooms <= 0) {
            throw new IllegalStateException("No rooms available");
        }

        this.availableRooms--;
    }

    // Getters
    public Long getId() { return id; }
    public String getHotelName() { return hotelName; }
    public RoomType getRoomType() { return roomType; }
    public LocalDate getDate() { return date; }
    public Integer getTotalRooms() { return totalRooms; }
    public Integer getAvailableRooms() { return availableRooms; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
