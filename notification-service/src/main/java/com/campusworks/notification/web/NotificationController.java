package com.campusworks.notification.web;

import com.campusworks.notification.service.NotificationService;
import com.campusworks.notification.service.NotificationService.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class NotificationController {

    private final NotificationService notificationService;

    // Public endpoint for inter-service communication
    @PostMapping
    public ResponseEntity<Void> sendNotification(@Valid @RequestBody NotificationRequest request) {
        notificationService.sendNotification(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/bulk")
    public ResponseEntity<Void> sendBulkNotifications(@Valid @RequestBody List<NotificationRequest> requests) {
        notificationService.sendBulkNotifications(requests);
        return ResponseEntity.ok().build();
    }

    // User-specific endpoints
    @GetMapping("/my-notifications")
    public ResponseEntity<Page<NotificationResponse>> getMyNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Boolean unreadOnly,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        Page<NotificationResponse> notifications = notificationService.getUserNotifications(
                userId, page, size, unreadOnly);
        return ResponseEntity.ok(notifications);
    }

    @PostMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponse> markAsRead(
            @PathVariable String notificationId,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        NotificationResponse response = notificationService.markAsRead(notificationId, userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/mark-all-read")
    public ResponseEntity<Void> markAllAsRead(Authentication authentication) {
        Long userId = extractUserId(authentication);
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> deleteNotification(
            @PathVariable String notificationId,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        notificationService.deleteNotification(notificationId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountResponse> getUnreadCount(Authentication authentication) {
        Long userId = extractUserId(authentication);
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(new UnreadCountResponse(count));
    }

    @GetMapping("/stats")
    public ResponseEntity<NotificationStatsResponse> getNotificationStats(Authentication authentication) {
        Long userId = extractUserId(authentication);
        NotificationStatsResponse stats = notificationService.getUserNotificationStats(userId);
        return ResponseEntity.ok(stats);
    }

    // Specialized notification endpoints
    @PostMapping("/task")
    public ResponseEntity<Void> sendTaskNotification(@RequestBody TaskNotificationRequest request) {
        notificationService.sendTaskNotification(
                request.getUserId(),
                request.getType(),
                request.getTaskId(),
                request.getTitle(),
                request.getMessage()
        );
        return ResponseEntity.ok().build();
    }

    @PostMapping("/bid")
    public ResponseEntity<Void> sendBidNotification(@RequestBody BidNotificationRequest request) {
        notificationService.sendBidNotification(
                request.getUserId(),
                request.getTaskId(),
                request.getBidId(),
                request.getMessage()
        );
        return ResponseEntity.ok().build();
    }

    @PostMapping("/payment")
    public ResponseEntity<Void> sendPaymentNotification(@RequestBody PaymentNotificationRequest request) {
        notificationService.sendPaymentNotification(
                request.getUserId(),
                request.getType(),
                request.getTaskId(),
                request.getAmount()
        );
        return ResponseEntity.ok().build();
    }

    @PostMapping("/system")
    public ResponseEntity<Void> sendSystemNotification(@RequestBody SystemNotificationRequest request) {
        notificationService.sendSystemNotification(
                request.getUserId(),
                request.getTitle(),
                request.getMessage()
        );
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reminder")
    public ResponseEntity<Void> sendReminderNotification(@RequestBody ReminderNotificationRequest request) {
        notificationService.sendReminderNotification(
                request.getUserId(),
                request.getTitle(),
                request.getMessage(),
                request.getExpiresAt()
        );
        return ResponseEntity.ok().build();
    }

    // Admin endpoints (would typically be secured with admin role)
    @GetMapping("/admin/stats")
    public ResponseEntity<AdminStatsResponse> getAdminStats() {
        // Implementation for admin statistics
        AdminStatsResponse stats = AdminStatsResponse.builder()
                .totalNotifications(0L)
                .totalUnread(0L)
                .totalEmailsSent(0L)
                .totalPushSent(0L)
                .build();
        return ResponseEntity.ok(stats);
    }

    // Health check
    @GetMapping("/health")
    public ResponseEntity<MessageResponse> health() {
        return ResponseEntity.ok(new MessageResponse("Notification service is healthy"));
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
    public static class TaskNotificationRequest {
        private Long userId;
        private String type;
        private Long taskId;
        private String title;
        private String message;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class BidNotificationRequest {
        private Long userId;
        private Long taskId;
        private Long bidId;
        private String message;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class PaymentNotificationRequest {
        private Long userId;
        private String type;
        private Long taskId;
        private String amount;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SystemNotificationRequest {
        private Long userId;
        private String title;
        private String message;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ReminderNotificationRequest {
        private Long userId;
        private String title;
        private String message;
        private java.time.LocalDateTime expiresAt;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class AdminStatsResponse {
        private Long totalNotifications;
        private Long totalUnread;
        private Long totalEmailsSent;
        private Long totalPushSent;
    }

    public record MessageResponse(String message) {}
    
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