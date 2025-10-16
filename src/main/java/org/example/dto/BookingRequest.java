package org.example.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class BookingRequest {
    private Long tenantId;
    private Long propertyId;
    private LocalDate startDate;
    private LocalDate endDate;
}
