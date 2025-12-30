package com.booking.platform.model;

public class BookingResponse {

    private String status;
    private String message;
    private Long bookingId;

    public BookingResponse() {
    }

    public BookingResponse(String status, String message, Long bookingId) {
        this.status = status;
        this.message = message;
        this.bookingId = bookingId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }
}