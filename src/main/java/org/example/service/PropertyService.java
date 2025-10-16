package org.example.service;

import org.example.dto.PropertyRequest;
import org.example.model.Property;
import org.example.model.User;
import org.example.repository.PropertyRepository;
import org.example.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PropertyService {

    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;

    @Transactional
    public Property createProperty(PropertyRequest request) {
        User owner = userRepository.findById(request.getOwnerId())
                .orElseThrow(() -> new RuntimeException("User not found with id " + request.getOwnerId()));

        Property property = Property.builder()
                .owner(owner)
                .address(request.getAddress())
                .description(request.getDescription())
                .status("active")
                .created_at(LocalDateTime.now())
                .build();

        return propertyRepository.save(property);
    }

    public List<Property> getAllProperties() {
        return propertyRepository.findAll();
    }

    public Property getPropertyById(Long id) {
        return propertyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Property not found with id " + id));
    }

    @Transactional
    public void deleteProperty(Long id) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Property not found with id " + id));
        propertyRepository.delete(property);
    }
}
