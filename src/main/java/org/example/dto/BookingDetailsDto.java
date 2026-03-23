package org.example.dto;

import java.time.LocalDate;

public record BookingDetailsDto(
        Long id,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        Integer monthlyRent,

        TenantSummaryDto tenant,
        PropertySummaryDto property
) {
    public record TenantSummaryDto(
            Long id,
            String fullName,
            String email
    ) {
    }

    public record PropertySummaryDto(
            Long id,
            String address,
            String description,
            Integer pricePerMonth
    ) {
    }
}

