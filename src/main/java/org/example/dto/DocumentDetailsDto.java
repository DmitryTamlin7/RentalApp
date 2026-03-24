package org.example.dto;

import java.time.LocalDateTime;

public record DocumentDetailsDto(
        Long id,
        String title,
        String documentType,
        String originalFileName,
        String mimeType,
        Long size,
        LocalDateTime createdAt,

        Long bookingId,
        String bookingTenantName,
        String propertyAddress,
        Long propertyId
) {
}

