package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.dto.DirectBookingRequest;
import org.example.dto.BookingDetailsDto;
import org.example.dto.PropertyCardDto;
import org.example.model.Booking;
import org.example.model.Property;
import org.example.model.User;
import org.example.repository.BookingRepository;
import org.example.repository.PropertyRepository;
import org.example.repository.UserRepository;
import org.example.service.BookingService;
import org.example.service.PropertyService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard/landlord")
@RequiredArgsConstructor
@PreAuthorize("hasRole('LANDLORD')")
public class LandlordDashboardController {

    private final PropertyService propertyService;
    private final BookingService bookingService;
    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final BookingRepository bookingRepository;

    @GetMapping("/properties")
    public ResponseEntity<List<PropertyCardDto>> getProperties(Principal principal) {
        return ResponseEntity.ok(propertyService.getLandlordProperties(principal));
    }

    @GetMapping("/stats")
    public ResponseEntity<PropertyService.LandlordStats> getStats(Principal principal) {
        return ResponseEntity.ok(propertyService.getStats(principal));
    }

    @DeleteMapping("/properties/{id}")
    public ResponseEntity<Void> deleteProperty(@PathVariable Long id, Principal principal) {
        propertyService.deleteProperty(id, principal);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/bookings")
    public ResponseEntity<List<BookingDetailsDto>> getBookings(Principal principal) {
        Long landlordId = getCurrentUserId(principal);
        List<Booking> bookings = bookingService.getBookingsByLandlordId(landlordId);
        List<BookingDetailsDto> result = bookings.stream()
                .map(bookingService::toDetailsDto)
                .toList();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/bookings/{id}/confirm")
    public ResponseEntity<String> confirmBooking(@PathVariable Long id, Principal principal) {
        validateBookingOwnership(id, getCurrentUserId(principal));
        bookingService.updateBooking(id, null, null, "confirmed");
        return ResponseEntity.ok("Бронь подтверждена");
    }

    @PostMapping("/bookings/{id}/reject")
    public ResponseEntity<String> rejectBooking(@PathVariable Long id, Principal principal) {
        validateBookingOwnership(id, getCurrentUserId(principal));
        bookingService.updateBooking(id, null, null, "rejected");
        return ResponseEntity.ok("Бронь отклонена");
    }

    @PostMapping("/bookings/{id}/cancel")
    public ResponseEntity<String> cancelBookingRequest(@PathVariable Long id, Principal principal) {
        validateBookingOwnership(id, getCurrentUserId(principal));
        bookingService.cancelBooking(id);
        return ResponseEntity.ok("Запрос на бронь отменен");
    }

    @PostMapping("/bookings/direct")
    public ResponseEntity<Map<String, Object>> createDirectBooking(
            @RequestBody DirectBookingRequest request,
            Principal principal) {

        User landlord = getCurrentUser(principal);

        User tenant = userRepository.findByEmail(request.tenantEmail())
                .orElseThrow(() -> new RuntimeException("Арендатор не найден: " + request.tenantEmail()));

        if (!"TENANT".equals(tenant.getRole())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Пользователь не является арендатором: " + request.tenantEmail()));
        }

        Property property = propertyRepository.findById(request.propertyId())
                .orElseThrow(() -> new RuntimeException("Объект не найден"));

        if (!property.getOwner().getId().equals(landlord.getId())) {
            return ResponseEntity.status(403)
                    .body(Map.of("error", "Это не ваш объект"));
        }

        Booking booking = bookingService.createBooking(
                tenant.getId(),
                request.propertyId(),
                request.startDate(),
                request.endDate()
        );

        return ResponseEntity.ok(Map.of(
                "message", "Бронь создана для " + tenant.getFullName(),
                "bookingId", booking.getId()
        ));
    }

    private Long getCurrentUserId(Principal principal) {
        return getCurrentUser(principal).getId();
    }

    private User getCurrentUser(Principal principal) {
        return userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
    }

    private void validateBookingOwnership(Long bookingId, Long landlordId) {
        Booking booking = bookingService.getBookingById(bookingId)
                .orElseThrow(() -> new RuntimeException("Бронь не найдена"));

        if (!booking.getProperty().getOwner().getId().equals(landlordId)) {
            throw new RuntimeException("Это не ваша бронь");
        }
    }
}