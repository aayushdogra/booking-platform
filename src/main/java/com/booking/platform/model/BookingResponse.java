package com.booking.platform.model;

public class BookingResponse {

    private String status;
    private String message;
    private String bookingId;

    public BookingResponse() {
    }

    public BookingResponse(String status, String message, String bookingId) {
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

    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }
}