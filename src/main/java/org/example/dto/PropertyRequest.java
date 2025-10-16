package org.example.dto;


import lombok.Data;

@Data
public class PropertyRequest {
    private Long ownerId;
    private String address;
    private String description;
}
