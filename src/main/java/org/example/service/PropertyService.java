package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.dto.PropertyCardDto;
import org.example.dto.PropertyRequest;
import org.example.model.Booking;
import org.example.model.Property;
import org.example.model.User;
import org.example.repository.BookingRepository;
import org.example.repository.PropertyRepository;
import org.example.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PropertyService {

    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;

    // === СОЗДАНИЕ ОБЪЕКТА ===
    @Transactional
    public Property createProperty(PropertyRequest request, Principal principal) {
        User owner = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Property property = Property.builder()
                .owner(owner)
                .address(request.getAddress())
                .description(request.getDescription())
                .pricePerMonth(request.getPricePerMonth())
                .status("active")
                .created_at(LocalDateTime.now())
                .build();

        return propertyRepository.save(property);
    }

    // === СПИСОК ДЛЯ ДАШБОРДА (ТОЛЬКО СВОИ ОБЪЕКТЫ) ===
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('LANDLORD')")
    public List<PropertyCardDto> getLandlordProperties(Principal principal) {
        User landlord = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return propertyRepository.findByOwner(landlord).stream()
                .map(this::toCardDto)
                .toList();
    }

    // === DTO ПРЕОБРАЗОВАНИЕ ===
    private PropertyCardDto toCardDto(Property property) {
        PropertyCardDto dto = new PropertyCardDto();
        dto.setId(property.getId());
        dto.setAddress(property.getAddress());
        dto.setPricePerMonth(property.getPricePerMonth());

        // Ищем активную бронь
        Booking activeBooking = bookingRepository.findActiveByPropertyId(property.getId()).orElse(null);
        if (activeBooking != null && activeBooking.getTenant() != null) {
            String fullName = activeBooking.getTenant().getFullName();
            if (fullName != null && !fullName.isBlank()) {
                String[] parts = fullName.trim().split("\\s+");
                String shortName = parts[0] + " " + parts[1].charAt(0) + ".";
                if (parts.length > 2) {
                    shortName += " " + parts[2].charAt(0) + ".";
                }
                dto.setTenantName(shortName);
            }
            dto.setStatus("rented");
            dto.setStatusLabel("Сдана");
        } else {
            dto.setTenantName(null);
            dto.setStatus("available");
            dto.setStatusLabel("Свободна");
        }

        return dto;
    }

    @Transactional
    @PreAuthorize("hasRole('LANDLORD')")
    public void deleteProperty(Long id, Principal principal) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Property not found with id " + id));

        if (!property.getOwner().getEmail().equals(principal.getName())) {
            throw new RuntimeException("You can only delete your own properties");
        }

        propertyRepository.delete(property);
    }

    // === ПОЛУЧЕНИЕ ПО ID (для редактирования) ===
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('LANDLORD')")
    public Property getPropertyById(Long id, Principal principal) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Property not found"));

        if (!property.getOwner().getEmail().equals(principal.getName())) {
            throw new RuntimeException("Access denied");
        }

        return property;
    }

    // === СТАТИСТИКА (опционально) ===
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('LANDLORD')")
    public LandlordStats getStats(Principal principal) {
        String email = principal.getName();
        long total = propertyRepository.countByOwnerEmail(email);
        long rented = propertyRepository.countRentedByOwnerEmail(email);
        long available = total - rented;

        return new LandlordStats(total, rented, available);
    }

    // === Внутренний класс для статистики ===
    public record LandlordStats(long total, long rented, long available) {}
}