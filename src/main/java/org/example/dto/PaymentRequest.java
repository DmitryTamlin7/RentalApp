package org.example.dto;

import java.math.BigDecimal;

public record PaymentRequest(
        Long bookingId,
        BigDecimal amount,
        String description
) {}