package org.example.testutil;

import org.example.model.User;
import org.example.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

public final class TestUsers {
    private TestUsers() {}

    public static User ensureUser(UserRepository repo, PasswordEncoder encoder, String email, String rawPassword, String fullName, String role) {
        return repo.findByEmail(email).orElseGet(() -> {
            User u = User.builder()
                    .email(email)
                    .password(encoder.encode(rawPassword))
                    .fullName(fullName)
                    .role(role)
                    .build();
            return repo.save(u);
        });
    }
}

