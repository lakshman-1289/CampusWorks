package com.campusworks.profile.web;

import com.campusworks.profile.service.ProfileService;
import com.campusworks.profile.service.ProfileService.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/profiles")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class ProfileController {

    private final ProfileService profileService;

    // Profile management endpoints
    @PostMapping
    public ResponseEntity<ProfileResponse> createProfile(
            @Valid @RequestBody CreateProfileRequest request,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        ProfileResponse response = profileService.createProfile(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping
    public ResponseEntity<ProfileResponse> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        ProfileResponse response = profileService.updateProfile(userId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<ProfileResponse> getMyProfile(Authentication authentication) {
        Long userId = extractUserId(authentication);
        ProfileResponse response = profileService.getProfile(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ProfileResponse> getUserProfile(@PathVariable Long userId) {
        ProfileResponse response = profileService.getPublicProfile(userId);
        return ResponseEntity.ok(response);
    }

    // Public endpoints for browsing profiles
    @GetMapping("/search")
    public ResponseEntity<Page<ProfileResponse>> searchProfiles(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) List<String> skills,
            @RequestParam(required = false) List<String> subjects,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<ProfileResponse> profiles = profileService.searchProfiles(keyword, skills, subjects, page, size);
        return ResponseEntity.ok(profiles);
    }

    @GetMapping("/top-rated")
    public ResponseEntity<List<ProfileResponse>> getTopRatedProfiles(
            @RequestParam(defaultValue = "10") int limit) {
        List<ProfileResponse> profiles = profileService.getTopRatedProfiles(limit);
        return ResponseEntity.ok(profiles);
    }

    // Verification endpoints
    @PostMapping("/verification")
    public ResponseEntity<Void> submitVerification(
            @Valid @RequestBody VerificationRequest request,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        profileService.submitVerification(userId, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{userId}/verification/approve")
    public ResponseEntity<Void> approveVerification(
            @PathVariable Long userId,
            @RequestBody VerificationDecisionRequest request) {
        // This endpoint should be secured with ADMIN role
        profileService.approveVerification(userId, request.getNotes());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{userId}/verification/reject")
    public ResponseEntity<Void> rejectVerification(
            @PathVariable Long userId,
            @RequestBody VerificationDecisionRequest request) {
        // This endpoint should be secured with ADMIN role
        profileService.rejectVerification(userId, request.getReason());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/verification/pending")
    public ResponseEntity<List<ProfileResponse>> getPendingVerifications() {
        // This endpoint should be secured with ADMIN role
        List<ProfileResponse> profiles = profileService.getPendingVerifications();
        return ResponseEntity.ok(profiles);
    }

    // Statistics endpoints
    @GetMapping("/stats")
    public ResponseEntity<ProfileStatsResponse> getMyStats(Authentication authentication) {
        Long userId = extractUserId(authentication);
        ProfileStatsResponse stats = profileService.getProfileStats(userId);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/{userId}/stats")
    public ResponseEntity<ProfileStatsResponse> getUserStats(@PathVariable Long userId) {
        ProfileStatsResponse stats = profileService.getProfileStats(userId);
        return ResponseEntity.ok(stats);
    }

    // File upload endpoints
    @PostMapping("/upload/profile-image")
    public ResponseEntity<FileUploadResponse> uploadProfileImage(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        try {
            // In a real implementation, you would:
            // 1. Validate file type and size
            // 2. Upload to cloud storage (AWS S3, Google Cloud Storage, etc.)
            // 3. Update user profile with image URL
            
            // For now, return a mock response
            FileUploadResponse response = FileUploadResponse.builder()
                    .fileUrl("https://example.com/profile-images/" + file.getOriginalFilename())
                    .fileName(file.getOriginalFilename())
                    .fileSize(file.getSize())
                    .build();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(FileUploadResponse.builder()
                            .error("File upload failed: " + e.getMessage())
                            .build());
        }
    }

    @PostMapping("/upload/id-document")
    public ResponseEntity<FileUploadResponse> uploadIdDocument(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        try {
            // Similar implementation for ID document upload
            FileUploadResponse response = FileUploadResponse.builder()
                    .fileUrl("https://example.com/id-documents/" + file.getOriginalFilename())
                    .fileName(file.getOriginalFilename())
                    .fileSize(file.getSize())
                    .build();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(FileUploadResponse.builder()
                            .error("File upload failed: " + e.getMessage())
                            .build());
        }
    }

    // Inter-service communication endpoints
    @PostMapping("/{userId}/increment-tasks")
    public ResponseEntity<Void> incrementCompletedTasks(@PathVariable Long userId) {
        profileService.incrementCompletedTasks(userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{userId}/add-earnings")
    public ResponseEntity<Void> addEarnings(@PathVariable Long userId, @RequestBody Integer amount) {
        profileService.addEarnings(userId, amount);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{userId}/increment-posted")
    public ResponseEntity<Void> incrementTasksPosted(@PathVariable Long userId) {
        profileService.incrementTasksPosted(userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{userId}/increment-hires")
    public ResponseEntity<Void> incrementSuccessfulHires(@PathVariable Long userId) {
        profileService.incrementSuccessfulHires(userId);
        return ResponseEntity.ok().build();
    }

    // Skill and subject management
    @GetMapping("/skills/popular")
    public ResponseEntity<List<SkillStatsResponse>> getPopularSkills() {
        // Implementation for popular skills statistics
        return ResponseEntity.ok(List.of());
    }

    @GetMapping("/subjects/popular")
    public ResponseEntity<List<SubjectStatsResponse>> getPopularSubjects() {
        // Implementation for popular subjects statistics
        return ResponseEntity.ok(List.of());
    }

    // Health check
    @GetMapping("/health")
    public ResponseEntity<MessageResponse> health() {
        return ResponseEntity.ok(new MessageResponse("Profile service is healthy"));
    }

    // Exception handlers
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException e) {
        ErrorResponse error = ErrorResponse.builder()
                .message(e.getMessage())
                .timestamp(java.time.LocalDateTime.now())
                .build();
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            org.springframework.web.bind.MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((first, second) -> first + ", " + second)
                .orElse("Validation failed");
        
        ErrorResponse error = ErrorResponse.builder()
                .message(message)
                .timestamp(java.time.LocalDateTime.now())
                .build();
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            org.springframework.security.access.AccessDeniedException e) {
        ErrorResponse error = ErrorResponse.builder()
                .message("Access denied")
                .timestamp(java.time.LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new RuntimeException("User not authenticated");
        }
        return Long.valueOf(authentication.getName());
    }

    // Additional DTOs
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class VerificationDecisionRequest {
        private String notes;
        private String reason;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class FileUploadResponse {
        private String fileUrl;
        private String fileName;
        private Long fileSize;
        private String error;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SkillStatsResponse {
        private String skill;
        private Long count;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SubjectStatsResponse {
        private String subject;
        private Long count;
    }

    public record MessageResponse(String message) {}

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ErrorResponse {
        private String message;
        private java.time.LocalDateTime timestamp;
    }
}