package com.booking.platform.service;

import com.booking.platform.domain.BookingStatus;
import com.booking.platform.entity.BookingEntity;
import com.booking.platform.exception.ResourceNotFoundException;
import com.booking.platform.repository.BookingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefundServiceImpl implements RefundService {

    private final BookingRepository bookingRepository;

    public RefundServiceImpl(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @Override
    @Transactional
    public void processRefund(Long bookingId) {

        BookingEntity booking = bookingRepository.findById(bookingId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Booking not found with id: " + bookingId)
                );

        BookingStatus status = booking.getStatus();

        // Idempotency guard
        if(status == BookingStatus.REFUNDED) {
            return;
        }

        // Refund only allowed from CANCELLED
        if(status != BookingStatus.CANCELLED) {
            return;
        }

        // Move to REFUND_PENDING
        booking.changeStatus(BookingStatus.REFUND_PENDING);

        // Simulate payment gateway refund
        simulateGatewayRefund();

        // Final state
        booking.changeStatus(BookingStatus.REFUNDED);
    }

    private void simulateGatewayRefund() {

    }
}
