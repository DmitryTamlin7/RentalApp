package org.example.controller;


import lombok.RequiredArgsConstructor;
import org.example.dto.BookingRequest;
import org.example.model.Booking;
import org.example.service.BookingService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public Booking createBooking(@RequestBody BookingRequest request) {
        return bookingService.createBooking(
                request.getTenantId(),
                request.getPropertyId(),
                request.getStartDate(),
                request.getEndDate()
        );
    }

    @PutMapping("/{id}")
    public Booking updateBooking(@PathVariable Long id, @RequestBody BookingRequest request) {
        return bookingService.updateBooking(
                id,
                request.getStartDate(),
                request.getEndDate(),
                "active"
        );
    }

    @PutMapping("/{id}/cancel")
    public void cancel(@PathVariable Long id){
        bookingService.cancelBooking(id);
    }

    @GetMapping
    public List<Booking> getAll() {
        return bookingService.getAllBookings();
    }

    @GetMapping("/{id}")
    public Booking getById(@PathVariable Long id){
        return bookingService.getBookingById(id)
                .orElseThrow(() -> new RuntimeException("Booking not exist"));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id){
        bookingService.cancelBooking(id);
    }
}

