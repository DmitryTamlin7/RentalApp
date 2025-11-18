package org.example.dto;


import lombok.Data;

@Data
public class PropertyRequest {
    private String address;
    private String description;
    private Integer pricePerMonth;
}
