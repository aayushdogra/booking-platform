package com.booking.platform.service.port;

public interface BookingEventHandler {

    void confirmBookingFromPaymentEvent(Long bookingId);
    void completeRefundFromRefundEvent(Long bookingId);
    void markPaymentFailed(Long bookingId, String reason);
    void markRefundFailed(Long bookingId, String reason);
}