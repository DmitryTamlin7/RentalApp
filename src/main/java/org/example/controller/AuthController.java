package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.JwtResponse;
import org.example.dto.LoginRequest;
import org.example.dto.RegisterRequest;
import org.example.dto.TokenRefreshRequest;
import org.example.service.AuthService;
import org.example.model.User;
import org.example.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        JwtResponse response = authService.authenticateUser(loginRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
        String newAccessToken = authService.refreshAccessToken(request);
        return ResponseEntity.ok(Map.of(
                "accessToken", newAccessToken,
                "refreshToken", request.getRefreshToken()
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser(@RequestBody Map<String, Long> body) {
        authService.logout(body.get("userId"));
        return ResponseEntity.ok("Logged out successfully");
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest request) {
        String role = request.role() == null ? "USER" : request.role().toUpperCase();
        // Keep public registration minimal and safe.
        if (!role.equals("TENANT") && !role.equals("LANDLORD")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Only TENANT or LANDLORD roles are allowed"));
        }

        if (userRepository.findByEmail(request.email()).isPresent()) {
            return ResponseEntity.status(409).body(Map.of("error", "User already exists"));
        }

        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .role(role)
                .build();

        User saved = userRepository.save(user);
        return ResponseEntity.ok(Map.of(
                "message", "User registered successfully",
                "id", saved.getId(),
                "email", saved.getEmail(),
                "role", saved.getRole()
        ));
    }
}