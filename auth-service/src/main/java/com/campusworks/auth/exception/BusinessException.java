package com.campusworks.auth.exception;

/**
 * Custom business exception for domain-specific errors in auth-service
 */
public class BusinessException extends RuntimeException {
    
    public BusinessException(String message) {
        super(message);
    }
    
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}