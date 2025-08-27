package com.campusworks.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Standard response DTO for simple message responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {
    
    private String message;
    private boolean success;
    private long timestamp;
    
    public MessageResponse(String message) {
        this.message = message;
        this.success = true;
        this.timestamp = System.currentTimeMillis();
    }
    
    public static MessageResponse success(String message) {
        return MessageResponse.builder()
                .message(message)
                .success(true)
                .timestamp(System.currentTimeMillis())
                .build();
    }
    
    public static MessageResponse error(String message) {
        return MessageResponse.builder()
                .message(message)
                .success(false)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}