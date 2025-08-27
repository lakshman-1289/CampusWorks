package com.campusworks.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for error responses with detailed error information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    
    private String message;
    private String errorCode;
    private long timestamp;
    private String path;
    private int status;
    private List<ValidationError> validationErrors;
    
    public ErrorResponse(String message, long timestamp) {
        this.message = message;
        this.timestamp = timestamp;
    }
    
    public ErrorResponse(String message, String errorCode, int status, String path) {
        this.message = message;
        this.errorCode = errorCode;
        this.status = status;
        this.path = path;
        this.timestamp = System.currentTimeMillis();
    }
    
    public static ErrorResponse badRequest(String message) {
        return ErrorResponse.builder()
                .message(message)
                .errorCode("BAD_REQUEST")
                .status(400)
                .timestamp(System.currentTimeMillis())
                .build();
    }
    
    public static ErrorResponse unauthorized(String message) {
        return ErrorResponse.builder()
                .message(message)
                .errorCode("UNAUTHORIZED")
                .status(401)
                .timestamp(System.currentTimeMillis())
                .build();
    }
    
    public static ErrorResponse forbidden(String message) {
        return ErrorResponse.builder()
                .message(message)
                .errorCode("FORBIDDEN")
                .status(403)
                .timestamp(System.currentTimeMillis())
                .build();
    }
    
    public static ErrorResponse internalError(String message) {
        return ErrorResponse.builder()
                .message(message)
                .errorCode("INTERNAL_SERVER_ERROR")
                .status(500)
                .timestamp(System.currentTimeMillis())
                .build();
    }
    
    /**
     * Nested class for field validation errors
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationError {
        private String field;
        private String message;
        private Object rejectedValue;
    }
}