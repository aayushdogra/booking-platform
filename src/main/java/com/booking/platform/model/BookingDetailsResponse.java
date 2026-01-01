package com.booking.platform.model;

import com.booking.platform.domain.BookingStatus;
import com.booking.platform.domain.RoomType;

import java.time.Instant;

public class BookingDetailsResponse {
    private Long id;
    private String userName;
    private String hotelName;
    private RoomType roomType;
    private Integer nights;
    private BookingStatus status;
    private Instant createdAt;
}
