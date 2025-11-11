package org.example.dto;

import lombok.Data;

@Data
public class PropertyCardDto {
    private Long id;
    private String address;
    private Integer pricePerMonth; // ← теперь есть
    private String tenantName;
    private String status;         // "rented" / "available"
    private String statusLabel;    // "Сдана" / "Свободна"
}