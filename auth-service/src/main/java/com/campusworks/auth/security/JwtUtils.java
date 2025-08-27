package com.campusworks.auth.security;

import com.campusworks.auth.exception.AuthenticationException;
import org.springframework.security.core.Authentication;
import lombok.extern.slf4j.Slf4j;

/**
 * JWT utility methods for auth-service
 */
@Slf4j
public class JwtUtils {

    /**
     * Extract user ID from Authentication object
     */
    public static Long extractUserId(Authentication authentication) {
        log.debug("Extracting user ID from Authentication object: {}", authentication);
        
        if (authentication == null) {
            log.warn("Authentication object is null");
            throw new AuthenticationException("User not authenticated - Authentication object is null");
        }
        
        if (authentication.getPrincipal() == null) {
            log.warn("Authentication principal is null");
            throw new AuthenticationException("User not authenticated - Principal is null");
        }
        
        try {
            String principal = authentication.getPrincipal().toString();
            log.debug("Authentication principal as string: {}", principal);
            Long userId = Long.valueOf(principal);
            log.debug("Converted user ID: {}", userId);
            return userId;
        } catch (NumberFormatException e) {
            log.error("Failed to convert principal to Long: {}", authentication.getPrincipal(), e);
            throw new AuthenticationException("Invalid user ID in authentication context: " + authentication.getPrincipal());
        }
    }
}