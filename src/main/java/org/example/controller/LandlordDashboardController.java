package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.dto.PropertyCardDto;
import org.example.service.PropertyService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard/landlord")
@RequiredArgsConstructor
@PreAuthorize("hasRole('LANDLORD')")
public class LandlordDashboardController {

    private final PropertyService propertyService;

    @GetMapping("/properties")
    public ResponseEntity<List<PropertyCardDto>> getProperties(Principal principal) {
        List<PropertyCardDto> properties = propertyService.getLandlordProperties(principal);
        return ResponseEntity.ok(properties);
    }


    @GetMapping("/stats")
    public ResponseEntity<PropertyService.LandlordStats> getStats(Principal principal) {
        PropertyService.LandlordStats stats = propertyService.getStats(principal);
        return ResponseEntity.ok(stats);
    }


    @DeleteMapping("/properties/{id}")
    public ResponseEntity<Void> deleteProperty(@PathVariable Long id, Principal principal) {
        propertyService.deleteProperty(id, principal);
        return ResponseEntity.noContent().build();
    }
}