package com.booking.platform.controller;

import com.booking.platform.model.BookingResponse;
import com.booking.platform.model.CreateBookingRequest;
import com.booking.platform.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(@Valid @RequestBody CreateBookingRequest request) {
        BookingResponse response = bookingService.createBooking(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingResponse> getBookingById(@PathVariable Long id) {
        BookingResponse response = bookingService.getBookingById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<BookingResponse>> getBookingsByUser(@RequestParam String userName) {
        List<BookingResponse> response = bookingService.getBookingsByUser(userName);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<BookingResponse> cancelBooking(@PathVariable Long id) {
        BookingResponse response = bookingService.cancelBooking(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/confirm")
    public BookingResponse confirmBooking(@PathVariable Long id) {
        return bookingService.confirmBooking(id);
    }

}