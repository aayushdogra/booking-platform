package com.booking.platform.graphql;

import com.booking.platform.entity.BookingEntity;
import com.booking.platform.exception.ResourceNotFoundException;
import com.booking.platform.graphql.dto.BookingSummary;
import com.booking.platform.repository.BookingRepository;
import org.springframework.data.domain.Sort;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class BookingQueryResolver {

    private final BookingRepository bookingRepository;

    public BookingQueryResolver(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @QueryMapping
    public BookingSummary bookingById(@Argument Long id) {
        BookingEntity booking =  bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));

        return mapToSummary(booking);
    }

    @QueryMapping
    public List<BookingSummary> bookingsByUser(@Argument String userId) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");

        return bookingRepository.findByUserName(userId, sort)
                .stream()
                .map(this::mapToSummary)
                .toList();
    }

    private BookingSummary mapToSummary(BookingEntity booking) {
        return new BookingSummary(
                booking.getId(),
                booking.getStatus().name(),
                booking.getUserName(),
                booking.getCreatedAt().toString()
        );
    }
}
