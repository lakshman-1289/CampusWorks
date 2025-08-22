package com.campusworks.auth.web;

import com.campusworks.auth.domain.User;
import com.campusworks.auth.service.AuthService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        User u = authService.register(request.getEmail(), request.getPassword());
        return ResponseEntity.ok(Map.of("id", u.getId(), "email", u.getEmail()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        return authService.login(request.getEmail(), request.getPassword())
                .<ResponseEntity<?>>map(t -> ResponseEntity.ok(Map.of("token", t)))
                .orElseGet(() -> ResponseEntity.status(401).body(Map.of("error", "Invalid credentials")));
    }

    @GetMapping("/validate")
    public ResponseEntity<?> validate() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @Data
    public static class RegisterRequest { private String email; private String password; }
    @Data
    public static class LoginRequest { private String email; private String password; }
}


