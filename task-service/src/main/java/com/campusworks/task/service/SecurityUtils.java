package com.campusworks.task.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtils {

    /**
     * Extract user ID from authentication context
     * @return User ID
     * @throws RuntimeException if user is not authenticated
     */
    public static Long extractUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new RuntimeException("User not authenticated");
        }
        return Long.valueOf(authentication.getName());
    }

    /**
     * Extract user ID from authentication context with a default value
     * @param defaultUserId Default user ID to return if not authenticated
     * @return User ID or default value
     */
    public static Long extractUserId(Long defaultUserId) {
        try {
            return extractUserId();
        } catch (RuntimeException e) {
            return defaultUserId;
        }
    }

    /**
     * Check if user is authenticated
     * @return true if user is authenticated, false otherwise
     */
    public static boolean isAuthenticated() {
        try {
            extractUserId();
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }
}