package com.campusworks.chat.web;

import com.campusworks.chat.service.ChatService;
import com.campusworks.chat.service.ChatService.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class ChatController {

    private final ChatService chatService;

    // REST endpoints
    @PostMapping("/conversations/task/{taskId}")
    public ResponseEntity<ConversationResponse> createOrGetConversation(
            @PathVariable Long taskId,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        ConversationResponse response = chatService.createOrGetConversation(taskId, userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/messages")
    public ResponseEntity<MessageResponse> sendMessage(
            @Valid @RequestBody SendMessageRequest request,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        MessageResponse response = chatService.sendMessage(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<Page<MessageResponse>> getMessages(
            @PathVariable String conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        Page<MessageResponse> messages = chatService.getMessages(conversationId, userId, page, size);
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/conversations")
    public ResponseEntity<List<ConversationResponse>> getUserConversations(Authentication authentication) {
        Long userId = extractUserId(authentication);
        List<ConversationResponse> conversations = chatService.getUserConversations(userId);
        return ResponseEntity.ok(conversations);
    }

    @PostMapping("/conversations/{conversationId}/read")
    public ResponseEntity<Void> markMessagesAsRead(
            @PathVariable String conversationId,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        chatService.markMessagesAsRead(conversationId, userId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/messages/{messageId}")
    public ResponseEntity<MessageResponse> editMessage(
            @PathVariable String messageId,
            @RequestBody EditMessageRequest request,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        MessageResponse response = chatService.editMessage(messageId, request.getContent(), userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable String messageId,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        chatService.deleteMessage(messageId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/conversations/{conversationId}/participants")
    public ResponseEntity<ConversationResponse> addParticipant(
            @PathVariable String conversationId,
            @RequestBody AddParticipantRequest request,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        ConversationResponse response = chatService.addParticipant(
                conversationId, request.getParticipantId(), userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountResponse> getUnreadMessageCount(Authentication authentication) {
        Long userId = extractUserId(authentication);
        long count = chatService.getUnreadMessageCount(userId);
        return ResponseEntity.ok(new UnreadCountResponse(count));
    }

    // WebSocket endpoints
    @MessageMapping("/chat.send")
    public void handleWebSocketMessage(@Payload SendMessageRequest message, Principal principal) {
        try {
            Long userId = Long.valueOf(principal.getName());
            chatService.sendMessage(message, userId);
        } catch (Exception e) {
            // Log error and optionally send error message back to client
            // In a real implementation, you might want to send error responses via WebSocket
        }
    }

    @MessageMapping("/chat.typing")
    public void handleTyping(@Payload TypingIndicatorRequest request, Principal principal) {
        // Handle typing indicators
        // This would broadcast typing status to other conversation participants
        // Implementation depends on your real-time requirements
    }

    // File upload endpoint
    @PostMapping("/upload")
    public ResponseEntity<FileUploadResponse> uploadFile(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            Authentication authentication) {
        try {
            // In a real implementation, you would:
            // 1. Validate file type and size
            // 2. Upload to cloud storage (AWS S3, Google Cloud Storage, etc.)
            // 3. Return the file URL
            
            // For now, return a mock response
            FileUploadResponse response = FileUploadResponse.builder()
                    .fileUrl("https://example.com/files/" + file.getOriginalFilename())
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

    // Health check
    @GetMapping("/health")
    public ResponseEntity<MessageResponse> health() {
        return ResponseEntity.ok(MessageResponse.builder()
                .content("Chat service is healthy")
                .build());
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
    public static class EditMessageRequest {
        @jakarta.validation.constraints.NotBlank
        private String content;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class AddParticipantRequest {
        @jakarta.validation.constraints.NotNull
        private Long participantId;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class TypingIndicatorRequest {
        private String conversationId;
        private boolean isTyping;
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

    public record UnreadCountResponse(long count) {}

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ErrorResponse {
        private String message;
        private java.time.LocalDateTime timestamp;
    }
}