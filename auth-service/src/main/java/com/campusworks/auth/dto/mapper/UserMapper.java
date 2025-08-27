package com.campusworks.auth.dto.mapper;

import com.campusworks.auth.model.User;
import com.campusworks.auth.dto.response.UserResponse;
import org.springframework.stereotype.Component;

/**
 * Utility class for mapping between User entity and DTOs
 * Follows clean architecture principles by separating mapping logic
 */
@Component
public class UserMapper {
    
    /**
     * Maps User entity to UserResponse DTO
     * Excludes sensitive information like password hash
     * 
     * @param user User entity
     * @return UserResponse DTO
     */
    public UserResponse toUserResponse(User user) {
        if (user == null) {
            return null;
        }
        
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .college(user.getCollege())
                .course(user.getCourse())
                .yearOfStudy(user.getYearOfStudy())
                .profileImageUrl(user.getProfileImageUrl())
                .roles(user.getRoles())
                .emailVerified(user.isEmailVerified())
                .phoneVerified(user.isPhoneVerified())
                .profileCompleted(user.isProfileCompleted())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }
    
    /**
     * Updates User entity from UserResponse DTO
     * Used for profile updates while preserving sensitive fields
     * 
     * @param user existing User entity
     * @param userResponse UserResponse DTO with updates
     * @return updated User entity
     */
    public User updateUserFromResponse(User user, UserResponse userResponse) {
        if (user == null || userResponse == null) {
            return user;
        }
        
        // Only update non-sensitive fields
        user.setFirstName(userResponse.getFirstName());
        user.setLastName(userResponse.getLastName());
        user.setPhoneNumber(userResponse.getPhoneNumber());
        user.setCollege(userResponse.getCollege());
        user.setCourse(userResponse.getCourse());
        user.setYearOfStudy(userResponse.getYearOfStudy());
        user.setProfileImageUrl(userResponse.getProfileImageUrl());
        
        return user;
    }
}