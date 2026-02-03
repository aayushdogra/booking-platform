package com.booking.platform.domain.service;

import com.booking.platform.entity.BookingEntity;
import com.booking.platform.exception.ResourceNotFoundException;
import com.booking.platform.repository.BookingRepository;
import com.booking.platform.service.AvailabilityService;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class BookingDomainService {

    private final BookingRepository bookingRepository;
    private final AvailabilityService availabilityService;

    public BookingDomainService(BookingRepository bookingRepository,  AvailabilityService availabilityService) {
        this.bookingRepository = bookingRepository;
        this.availabilityService = availabilityService;
    }

    public void confirmFromPaymentEvent(Long bookingId) {
        BookingEntity booking = load(bookingId);

        booking.markPaymentSucceeded(Instant.now());
    }

    public void markPaymentFailed(Long bookingId, String reason) {

        BookingEntity booking = load(bookingId);

        booking.markPaymentFailed(reason);

        releaseAvailability(booking);
    }

    public void completeRefund(Long bookingId) {

        BookingEntity booking = load(bookingId);

        booking.markRefundSucceeded();
    }

    public void markRefundFailed(Long bookingId, String reason) {

        BookingEntity booking = load(bookingId);

        booking.markRefundFailed(reason);
    }

    private BookingEntity load(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Booking not found with id: " + bookingId));
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
