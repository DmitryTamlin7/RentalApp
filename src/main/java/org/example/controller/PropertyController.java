package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.dto.PropertyCardDto;
import org.example.dto.PropertyRequest;
import org.example.model.Property;
import org.example.service.PropertyService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/properties")
@RequiredArgsConstructor
@PreAuthorize("hasRole('LANDLORD')")
public class PropertyController {

    private final PropertyService propertyService;


    @PostMapping
    public ResponseEntity<Property> createProperty(
            @RequestBody PropertyRequest request,
            Principal principal) {
        Property property = propertyService.createProperty(request, principal);
        return ResponseEntity.ok(property);
    }


    @GetMapping("/my")
    public ResponseEntity<List<PropertyCardDto>> getMyProperties(Principal principal) {
        List<PropertyCardDto> properties = propertyService.getLandlordProperties(principal);
        return ResponseEntity.ok(properties);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Property> getPropertyById(
            @PathVariable Long id,
            Principal principal) {
        Property property = propertyService.getPropertyById(id, principal);
        return ResponseEntity.ok(property);
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProperty(
            @PathVariable Long id,
            Principal principal) {
        propertyService.deleteProperty(id, principal);
        return ResponseEntity.noContent().build();
    }


    @GetMapping("/stats")
    public ResponseEntity<PropertyService.LandlordStats> getStats(Principal principal) {
        PropertyService.LandlordStats stats = propertyService.getStats(principal);
        return ResponseEntity.ok(stats);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PropertyCardDto> updateProperty(
            @PathVariable Long id,
            @RequestBody PropertyRequest request,
            Principal principal) {

        Property property = propertyService.updateProperty(id, request, principal);
        PropertyCardDto dto = propertyService.toCardDto(property);
        return ResponseEntity.ok(dto);
    }


}