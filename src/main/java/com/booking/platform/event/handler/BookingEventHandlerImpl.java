package com.booking.platform.event.handler;

import com.booking.platform.service.port.BookingEventHandler;
import com.booking.platform.domain.service.BookingDomainService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BookingEventHandlerImpl implements BookingEventHandler {

    private final BookingDomainService bookingDomainService;

    public BookingEventHandlerImpl(BookingDomainService bookingDomainService) {
        this.bookingDomainService = bookingDomainService;
    }

    @Override
    @Transactional
    public void confirmBookingFromPaymentEvent(Long bookingId) {
        bookingDomainService.confirmFromPaymentEvent(bookingId);
    }

    @Override
    @Transactional
    public void completeRefundFromRefundEvent(Long bookingId) {
        bookingDomainService.completeRefund(bookingId);
    }

    @Override
    @Transactional
    public void markPaymentFailed(Long bookingId, String reason) {
        bookingDomainService.markPaymentFailed(bookingId, reason);
    }

    @Override
    @Transactional
    public void markRefundFailed(Long bookingId, String reason) {
        bookingDomainService.markRefundFailed(bookingId, reason);
    }
}
