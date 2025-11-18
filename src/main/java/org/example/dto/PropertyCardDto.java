package org.example.dto;

import lombok.Data;

@Data
public class PropertyCardDto {
    private Long id;
    private String address;
    private Integer pricePerMonth;
    private String tenantName;
    private String status;
    private String statusLabel;
}