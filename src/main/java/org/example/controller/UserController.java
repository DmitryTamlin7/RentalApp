package org.example.controller;


import org.example.dto.UserRequest;
import org.example.dto.UserUpdateRequest;
import org.example.model.User;
import org.example.repository.UserRepository;
import org.example.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;


    @PostMapping
    public User createUser(@RequestBody UserRequest request) {
        User user = User.builder()
                .email(request.getEmail())
                .password(request.getPassword())
                .fullName(request.getFullName())
                .role(request.getRole() != null ? request.getRole() : "USER")
                .build();

        System.out.println(">>> Creating user: " + user);
        return userService.createUser(user);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/role")
    public ResponseEntity<?> updateRole(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String newRole = body.get("role");
        if (newRole == null) return ResponseEntity.badRequest().body(Map.of("error", "Role is missing"));

        User updatedUser = userService.changeUserRole(id, newRole);
        return ResponseEntity.ok(Map.of(
                "message", "Роль пользователя успешно изменена",
                "email", updatedUser.getEmail(),
                "newRole", updatedUser.getRole()
        ));
    }


    @GetMapping
    public List<User> getAll() {
        return userService.getAllUsers();
    }


    @GetMapping("/{id}")
    public User getById(@PathVariable Long id) {
        return userService.getUserById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/fullname")
    public User updateFullName(@PathVariable Long id, @RequestBody UserUpdateRequest dto) {
        return userService.updateUserFullName(id, dto.fullName());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public User updateUser(@PathVariable Long id, @RequestBody Map<String, Object> updates) {
        return userService.updateUser(id, updates);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        userService.deleteUser(id);
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(Principal principal) {
        String email = principal.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "fullName", user.getFullName(),
                "role", user.getRole()
        ));
    }
}
