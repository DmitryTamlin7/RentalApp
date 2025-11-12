package org.example.dto;

import java.time.LocalDate;

public record DirectBookingRequest(
        String tenantEmail,
        Long propertyId,
        LocalDate startDate,
        LocalDate endDate
) {}