package org.example.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PaymentDetailsDto(
        Long paymentId,
        BigDecimal amount,
        String description,
        String status,
        LocalDateTime requestedAt,
        LocalDateTime tenantPaidAt,
        LocalDateTime confirmedAt,

        Long bookingId,
        LocalDate bookingStartDate,
        LocalDate bookingEndDate,
        Integer monthlyRent,

        String tenantFullName,

        Long propertyId,
        String propertyAddress,
        String propertyDescription
) {
}

