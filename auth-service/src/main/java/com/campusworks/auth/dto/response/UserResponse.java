package com.campusworks.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * DTO for user information in responses (excluding sensitive data)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String college;
    private String course;
    private Integer yearOfStudy;
    private String profileImageUrl;
    private Set<String> roles;
    private boolean emailVerified;
    private boolean phoneVerified;
    private boolean profileCompleted;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
    
    // Computed fields
    public String getFullName() {
        return firstName + " " + lastName;
    }
    
    public boolean isVerified() {
        return emailVerified && profileCompleted;
    }
}