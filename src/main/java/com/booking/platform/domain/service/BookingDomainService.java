package com.booking.platform.domain.service;

import com.booking.platform.domain.BookingStatus;
import com.booking.platform.entity.BookingEntity;
import com.booking.platform.exception.ResourceNotFoundException;
import com.booking.platform.repository.BookingRepository;
import com.booking.platform.service.AvailabilityService;
import org.springframework.stereotype.Service;

@Service
public class BookingDomainService {

    private final BookingRepository bookingRepository;
    private final AvailabilityService availabilityService;

    public BookingDomainService(BookingRepository bookingRepository,  AvailabilityService availabilityService) {
        this.bookingRepository = bookingRepository;
        this.availabilityService = availabilityService;
    }

    public void confirmFromPaymentEvent(Long bookingId) {

        BookingEntity booking = bookingRepository.findById(bookingId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Booking not found with id: " + bookingId));

        if (booking.getStatus() != BookingStatus.CREATED) {
            return; // idempotent guard
        }

        booking.changeStatus(BookingStatus.CONFIRMED);
    }

    public void markPaymentFailed(Long bookingId, String reason) {

        BookingEntity booking = bookingRepository.findById(bookingId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Booking not found with id: " + bookingId));

        if (booking.getStatus() != BookingStatus.CREATED) {
            return; // idempotent guard
        }

        booking.setFailureReason(reason);
        booking.changeStatus(BookingStatus.PAYMENT_FAILED);

        releaseAvailability(booking);
    }

    public void completeRefund(Long bookingId) {

        BookingEntity booking = bookingRepository.findById(bookingId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Booking not found with id: " + bookingId));

        if (booking.getStatus() != BookingStatus.REFUND_PENDING) {
            return; // idempotent guard
        }

        booking.changeStatus(BookingStatus.REFUNDED);
    }

    public void markRefundFailed(Long bookingId, String reason) {

        BookingEntity booking = bookingRepository.findById(bookingId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Booking not found with id: " + bookingId));

        if (booking.getStatus() != BookingStatus.REFUND_PENDING) {
            return;
        }

        booking.setFailureReason(reason);
        booking.changeStatus(BookingStatus.REFUND_FAILED);
    }

    private void releaseAvailability(BookingEntity booking) {

        availabilityService.release(
                booking.getHotelName(),
                booking.getRoomType(),
                booking.getCreatedAt()
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate()
        );
    }
}
