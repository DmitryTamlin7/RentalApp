package org.example.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterRequest(
        @NotBlank(message = "Email обязателен")
        String email,

        @NotBlank(message = "Пароль обязателен")
        String password,

        @NotBlank(message = "Имя обязательно")
        String fullName,

        @NotNull(message = "Роль обязательна")
        String role
) {}