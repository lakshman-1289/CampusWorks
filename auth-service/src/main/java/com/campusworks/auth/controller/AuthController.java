package com.campusworks.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.campusworks.auth.dto.request.ChangePasswordRequest;
import com.campusworks.auth.dto.request.LoginRequest;
import com.campusworks.auth.dto.request.RegisterRequest;
import com.campusworks.auth.dto.request.ResetPasswordRequest;
import com.campusworks.auth.dto.response.AuthResponse;
import com.campusworks.auth.dto.response.MessageResponse;
import com.campusworks.auth.dto.response.UserResponse;
import com.campusworks.auth.service.AuthService;
import com.campusworks.auth.security.JwtUtils;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Register request received for email: {}", request.getEmail());
        AuthResponse response = authService.register(request);
        log.info("Registration successful for email: {}", request.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login request received for email: {}", request.getEmail());
        AuthResponse response = authService.login(request);
        log.info("Login successful for email: {}", request.getEmail());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-email")
    public ResponseEntity<MessageResponse> verifyEmail(@RequestParam("token") String token) {
        log.info("Email verification request received for token: {}", token);
        authService.verifyEmail(token);
        return ResponseEntity.ok(MessageResponse.success("Email verified successfully"));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<MessageResponse> resendVerificationEmail(@RequestParam("email") String email) {
        log.info("Resend verification request received for email: {}", email);
        authService.resendVerificationEmail(email);
        return ResponseEntity.ok(MessageResponse.success("Verification email sent"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(@RequestParam("email") String email) {
        log.info("Forgot password request received for email: {}", email);
        authService.forgotPassword(email);
        return ResponseEntity.ok(MessageResponse.success("Password reset email sent"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("Reset password request received for token: {}", request.getToken());
        authService.resetPassword(request);
        return ResponseEntity.ok(MessageResponse.success("Password reset successfully"));
    }

    @PostMapping("/change-password")
    public ResponseEntity<MessageResponse> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication) {
        log.info("Change password request received");
        log.debug("Authentication object: {}", authentication);
        log.debug("Authentication principal: {}", authentication != null ? authentication.getPrincipal() : "null");
        
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("Change password request rejected - user not authenticated");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(MessageResponse.error("User not authenticated"));
        }
        
        Long userId = JwtUtils.extractUserId(authentication);
        log.info("Changing password for user ID: {}", userId);
        authService.changePassword(userId, request);
        return ResponseEntity.ok(MessageResponse.success("Password changed successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
        log.info("==== GET /me Endpoint Request ====");
        log.info("Authentication object present: {}", authentication != null);
        
        if (authentication == null) {
            log.warn("❌ NO AUTHENTICATION OBJECT - User not authenticated");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
        
        log.info("Authentication object: {}", authentication);
        log.info("Authentication principal: {}", authentication.getPrincipal());
        log.info("Authentication authenticated: {}", authentication.isAuthenticated());
        log.info("Authentication authorities: {}", authentication.getAuthorities());
        
        if (!authentication.isAuthenticated()) {
            log.warn("❌ AUTHENTICATION NOT VALID - User not authenticated");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
        
        try {
            Long userId = JwtUtils.extractUserId(authentication);
            log.info("Extracted user ID from authentication: {}", userId);
            UserResponse user = authService.getCurrentUser(userId);
            log.info("✅ Successfully retrieved user info for user ID: {}", userId);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            log.error("❌ Error retrieving user info", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
    }

    // Logout endpoint (client-side token removal)
    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout() {
        // In a stateless JWT system, logout is handled client-side by removing the token
        // For enhanced security, you could implement a token blacklist here
        return ResponseEntity.ok(MessageResponse.success("Logout successful"));
    }

    // Health check endpoint
    @GetMapping("/health")
    public ResponseEntity<MessageResponse> health() {
        return ResponseEntity.ok(MessageResponse.success("Auth service is healthy"));
    }
    
    // Simple test endpoint to diagnose issues
    @GetMapping("/test")
    public ResponseEntity<MessageResponse> test() {
        return ResponseEntity.ok(MessageResponse.success("Test endpoint working - no database required"));
    }
}