package org.example.controller;


import lombok.RequiredArgsConstructor;
import org.example.dto.BookingRequest;
import org.example.model.Booking;
import org.example.service.BookingService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PreAuthorize("hasRole('TENANT') or hasRole('ADMIN')")
    @PostMapping
    public Booking createBooking(@RequestBody BookingRequest request) {
        return bookingService.createBooking(
                request.getTenantId(),
                request.getPropertyId(),
                request.getStartDate(),
                request.getEndDate()
        );
    }

    @PreAuthorize("hasRole('TENANT') or hasRole('ADMIN')")
    @PutMapping("/{id}")
    public Booking updateBooking(@PathVariable Long id, @RequestBody BookingRequest request) {
        return bookingService.updateBooking(
                id,
                request.getStartDate(),
                request.getEndDate(),
                "active"
        );
    }

    @PreAuthorize("hasRole('TENANT') or hasRole('ADMIN')")
    @PutMapping("/{id}/cancel")
    public void cancel(@PathVariable Long id){
        bookingService.cancelBooking(id);
    }

    @PreAuthorize("hasRole('TENANT') or hasRole('ADMIN')")
    @GetMapping
    public List<Booking> getAll() {
        return bookingService.getAllBookings();
    }

    @PreAuthorize("hasRole('TENANT') or hasRole('ADMIN')")
    @GetMapping("/{id}")
    public Booking getById(@PathVariable Long id){
        return bookingService.getBookingById(id)
                .orElseThrow(() -> new RuntimeException("Booking not exist"));
    }

    @PreAuthorize("hasRole('TENANT') or hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id){
        bookingService.cancelBooking(id);
    }
}

