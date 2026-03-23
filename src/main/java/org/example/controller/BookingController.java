package org.example.controller;


import lombok.RequiredArgsConstructor;
import org.example.dto.BookingRequest;
import org.example.dto.BookingDetailsDto;
import org.example.model.Booking;
import org.example.model.Property;
import org.example.model.User;
import org.example.repository.BookingRepository;
import org.example.repository.PropertyRepository;
import org.example.repository.UserRepository;
import org.example.service.BookingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final BookingRepository bookingRepository;

    @PostMapping
    @PreAuthorize("hasRole('TENANT')")
    public ResponseEntity<?> createBooking(@RequestBody BookingRequest request, Principal principal) {
        String email = principal.getName();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));


        if (!request.getTenantId().equals(currentUser.getId())) {
            return ResponseEntity.status(403)
                    .body(Map.of("error", "You can only book for yourself"));
        }

        Property property = propertyRepository.findById(request.getPropertyId())
                .orElseThrow(() -> new RuntimeException("Property not found"));

        if (!"LANDLORD".equals(property.getOwner().getRole())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Property not available for booking"));
        }

        Booking booking = bookingService.createBooking(
                request.getTenantId(),
                request.getPropertyId(),
                request.getStartDate(),
                request.getEndDate()
        );

        return ResponseEntity.ok(Map.of("bookingId", booking.getId()));
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
    @PutMapping("/{id}/accept")
    public ResponseEntity<?> acceptBooking(@PathVariable Long id, Principal principal) {
        User currentUser = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Booking booking = bookingService.getBookingById(id)
                .orElseThrow(() -> new RuntimeException("Booking not exist"));

        // Tenant can accept only their own booking; ADMIN can accept any.
        if ("TENANT".equalsIgnoreCase(currentUser.getRole())
                && booking.getTenant() != null
                && !booking.getTenant().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(403).body(Map.of("error", "You can only accept your own booking"));
        }

        if (!"requested".equalsIgnoreCase(booking.getStatus())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Booking is not in requested status"
            ));
        }

        bookingService.updateBooking(id, null, null, "confirmed");
        return ResponseEntity.ok(Map.of("message", "Бронь подтверждена"));
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

    @GetMapping("/my")
    @PreAuthorize("hasRole('TENANT')")
    public ResponseEntity<List<BookingDetailsDto>> getMyBookings(Principal principal) {
        String email = principal.getName();
        User tenant = userRepository.findByEmail(email).orElseThrow();
        List<Booking> bookings = bookingRepository.findByTenantId(tenant.getId());
        List<BookingDetailsDto> result = bookings.stream()
                .map(bookingService::toDetailsDto)
                .toList();
        return ResponseEntity.ok(result);
    }
}

