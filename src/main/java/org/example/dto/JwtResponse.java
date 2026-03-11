package org.example.dto;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JwtResponse {
    private String accessToken;
    private String refreshToken;
    private Long id;
    private String email;
    private String role;
    @Builder.Default
    private String type = "Bearer";
}