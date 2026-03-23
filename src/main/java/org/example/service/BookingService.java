package org.example.service;

import org.example.dto.BookingDetailsDto;
import org.example.model.Booking;
import org.example.model.Property;
import org.example.model.User;
import org.example.repository.BookingRepository;
import org.example.repository.PropertyRepository;
import org.example.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class BookingService {

    private final BookingRepository bookingRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;

    public Booking createBooking(Long tenantId, Long propertyId, LocalDate start, LocalDate end) {
        User tenant = userRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new RuntimeException("Property not found"));

        Booking booking = Booking.builder()
                .tenant(tenant)
                .property(property)
                .startDate(start)
                .endDate(end)
                .monthlyRent(property.getPricePerMonth())
                // New workflow: landlord creates a request; tenant later accepts it.
                .status("requested")
                .build();

        return bookingRepository.save(booking);
    }

    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    public Optional<Booking> getBookingById(Long id) {
        return bookingRepository.findById(id);
    }

    public Booking updateBooking(Long id, LocalDate start, LocalDate end, String status) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (start != null) booking.setStartDate(start);
        if (end != null) booking.setEndDate(end);
        if (status != null) booking.setStatus(status);

        return bookingRepository.save(booking);
    }

    public void cancelBooking(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        booking.setStatus("cancelled");
        bookingRepository.save(booking);
    }

    public List<Booking> getBookingsByLandlordId(Long landlordId) {
        return bookingRepository.findByPropertyOwnerId(landlordId);
    }

    public BookingDetailsDto toDetailsDto(Booking booking) {
        BookingDetailsDto.TenantSummaryDto tenantDto = null;
        if (booking.getTenant() != null) {
            tenantDto = new BookingDetailsDto.TenantSummaryDto(
                    booking.getTenant().getId(),
                    booking.getTenant().getFullName(),
                    booking.getTenant().getEmail()
            );
        }

        BookingDetailsDto.PropertySummaryDto propertyDto = null;
        if (booking.getProperty() != null) {
            propertyDto = new BookingDetailsDto.PropertySummaryDto(
                    booking.getProperty().getId(),
                    booking.getProperty().getAddress(),
                    booking.getProperty().getDescription(),
                    booking.getProperty().getPricePerMonth()
            );
        }

        return new BookingDetailsDto(
                booking.getId(),
                booking.getStartDate(),
                booking.getEndDate(),
                booking.getStatus(),
                booking.getMonthlyRent(),
                tenantDto,
                propertyDto
        );
    }
}
