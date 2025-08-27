package com.campusworks.notification.service;

import com.campusworks.notification.domain.Notification;
import com.campusworks.notification.repo.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailNotificationService emailService;
    private final PushNotificationService pushService;
    private final WebSocketNotificationService webSocketService;
    private final ProfileServiceClient profileServiceClient;

    @Value("${app.notification.email.enabled:true}")
    private boolean emailEnabled;

    @Value("${app.notification.push.enabled:false}")
    private boolean pushEnabled;

    @Async
    public void sendNotification(NotificationRequest request) {
        log.info("Sending notification to user {}: {}", request.getUserId(), request.getTitle());

        try {
            // Create notification record
            Notification notification = Notification.builder()
                    .userId(request.getUserId())
                    .title(request.getTitle())
                    .message(request.getMessage())
                    .type(request.getType() != null ? 
                            Notification.NotificationType.valueOf(request.getType()) : 
                            Notification.NotificationType.INFO)
                    .relatedEntityId(request.getRelatedEntityId())
                    .relatedEntityType(request.getRelatedEntityType())
                    .data(request.getData())
                    .priority(request.getPriority() != null ? 
                            Notification.NotificationPriority.valueOf(request.getPriority()) : 
                            Notification.NotificationPriority.NORMAL)
                    .expiresAt(request.getExpiresAt())
                    .build();

            Notification savedNotification = notificationRepository.save(notification);

            // Send via different channels
            sendViaMultipleChannels(savedNotification);

            log.info("Notification sent successfully: {}", savedNotification.getId());

        } catch (Exception e) {
            log.error("Failed to send notification: {}", e.getMessage(), e);
        }
    }

    public void sendBulkNotifications(List<NotificationRequest> requests) {
        log.info("Sending {} bulk notifications", requests.size());

        for (NotificationRequest request : requests) {
            sendNotification(request);
        }
    }

    public Page<NotificationResponse> getUserNotifications(Long userId, int page, int size, Boolean unreadOnly) {
        log.info("Getting notifications for user {}, page {}, size {}, unreadOnly: {}", userId, page, size, unreadOnly);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Notification> notifications;

        if (Boolean.TRUE.equals(unreadOnly)) {
            notifications = notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId, pageable);
        } else {
            notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        }

        return notifications.map(this::mapToResponse);
    }

    public NotificationResponse markAsRead(String notificationId, Long userId) {
        log.info("Marking notification {} as read by user {}", notificationId, userId);

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        if (!notification.getUserId().equals(userId)) {
            throw new RuntimeException("Access denied");
        }

        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
            notification = notificationRepository.save(notification);
        }

        return mapToResponse(notification);
    }

    public void markAllAsRead(Long userId) {
        log.info("Marking all notifications as read for user {}", userId);

        List<Notification> unreadNotifications = notificationRepository.findByUserIdAndIsReadFalse(userId);
        
        for (Notification notification : unreadNotifications) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
        }

        if (!unreadNotifications.isEmpty()) {
            notificationRepository.saveAll(unreadNotifications);
        }
    }

    public void deleteNotification(String notificationId, Long userId) {
        log.info("Deleting notification {} by user {}", notificationId, userId);

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        if (!notification.getUserId().equals(userId)) {
            throw new RuntimeException("Access denied");
        }

        notificationRepository.delete(notification);
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    public NotificationStatsResponse getUserNotificationStats(Long userId) {
        long totalNotifications = notificationRepository.countByUserId(userId);
        long unreadNotifications = notificationRepository.countByUserIdAndIsReadFalse(userId);
        
        // Get last 30 days activity
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        long recentNotifications = notificationRepository.countByUserIdAndCreatedAtAfter(userId, thirtyDaysAgo);

        return NotificationStatsResponse.builder()
                .totalNotifications(totalNotifications)
                .unreadNotifications(unreadNotifications)
                .recentNotifications(recentNotifications)
                .readPercentage(totalNotifications > 0 ? 
                        (double) (totalNotifications - unreadNotifications) / totalNotifications * 100 : 0)
                .build();
    }

    // Specialized notification methods
    public void sendTaskNotification(Long userId, String type, Long taskId, String title, String message) {
        NotificationRequest request = NotificationRequest.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .message(message)
                .relatedEntityId(taskId)
                .relatedEntityType("TASK")
                .priority("NORMAL")
                .build();

        sendNotification(request);
    }

    public void sendBidNotification(Long userId, Long taskId, Long bidId, String message) {
        NotificationRequest request = NotificationRequest.builder()
                .userId(userId)
                .type("NEW_BID_RECEIVED")
                .title("New Bid Received")
                .message(message)
                .relatedEntityId(taskId)
                .relatedEntityType("BID")
                .data(Map.of("bidId", bidId))
                .priority("HIGH")
                .build();

        sendNotification(request);
    }

    public void sendPaymentNotification(Long userId, String type, Long taskId, String amount) {
        String title = switch (type) {
            case "PAYMENT_COMPLETED" -> "Payment Completed";
            case "PAYMENT_RECEIVED" -> "Payment Received";
            case "PAYMENT_RELEASED" -> "Payment Released";
            case "PAYMENT_REFUNDED" -> "Payment Refunded";
            default -> "Payment Update";
        };

        String message = switch (type) {
            case "PAYMENT_COMPLETED" -> "Payment of ₹" + amount + " has been completed successfully";
            case "PAYMENT_RECEIVED" -> "Payment of ₹" + amount + " is being held in escrow";
            case "PAYMENT_RELEASED" -> "₹" + amount + " has been transferred to your account";
            case "PAYMENT_REFUNDED" -> "₹" + amount + " has been refunded to your account";
            default -> "Payment status updated";
        };

        NotificationRequest request = NotificationRequest.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .message(message)
                .relatedEntityId(taskId)
                .relatedEntityType("PAYMENT")
                .priority("HIGH")
                .build();

        sendNotification(request);
    }

    public void sendSystemNotification(Long userId, String title, String message) {
        NotificationRequest request = NotificationRequest.builder()
                .userId(userId)
                .type("SYSTEM_NOTIFICATION")
                .title(title)
                .message(message)
                .relatedEntityType("SYSTEM")
                .priority("NORMAL")
                .build();

        sendNotification(request);
    }

    public void sendReminderNotification(Long userId, String title, String message, LocalDateTime expiresAt) {
        NotificationRequest request = NotificationRequest.builder()
                .userId(userId)
                .type("REMINDER")
                .title(title)
                .message(message)
                .relatedEntityType("REMINDER")
                .priority("NORMAL")
                .expiresAt(expiresAt)
                .build();

        sendNotification(request);
    }

    // Scheduled cleanup tasks
    @Scheduled(cron = "0 0 2 * * ?") // Daily at 2 AM
    public void cleanupExpiredNotifications() {
        log.info("Cleaning up expired notifications");

        LocalDateTime now = LocalDateTime.now();
        List<Notification> expiredNotifications = notificationRepository.findByExpiresAtBefore(now);

        if (!expiredNotifications.isEmpty()) {
            notificationRepository.deleteAll(expiredNotifications);
            log.info("Deleted {} expired notifications", expiredNotifications.size());
        }
    }

    @Scheduled(cron = "0 0 3 * * ?") // Daily at 3 AM
    public void cleanupOldReadNotifications() {
        log.info("Cleaning up old read notifications");

        // Delete read notifications older than 90 days
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(90);
        List<Notification> oldNotifications = notificationRepository.findByIsReadTrueAndCreatedAtBefore(cutoffDate);

        if (!oldNotifications.isEmpty()) {
            notificationRepository.deleteAll(oldNotifications);
            log.info("Deleted {} old read notifications", oldNotifications.size());
        }
    }

    // Private helper methods
    private void sendViaMultipleChannels(Notification notification) {
        try {
            // Always send real-time web notification
            webSocketService.sendRealTimeNotification(notification);

            // Get user preferences
            UserPreferences preferences = getUserPreferences(notification.getUserId());

            // Send email if enabled and user allows it
            if (emailEnabled && preferences.isEmailNotifications()) {
                emailService.sendNotificationEmail(notification, preferences);
            }

            // Send push notification if enabled and user allows it
            if (pushEnabled && preferences.isPushNotifications()) {
                pushService.sendPushNotification(notification, preferences);
            }

        } catch (Exception e) {
            log.error("Failed to send notification via multiple channels: {}", e.getMessage());
        }
    }

    private UserPreferences getUserPreferences(Long userId) {
        try {
            UserProfileResponse profile = profileServiceClient.getUserProfile(userId);
            return UserPreferences.builder()
                    .emailNotifications(true) // Default to true
                    .pushNotifications(false) // Default to false
                    .userEmail(profile.getEmail())
                    .userName(profile.getFirstName() + " " + profile.getLastName())
                    .build();
        } catch (Exception e) {
            log.error("Failed to get user preferences for user {}: {}", userId, e.getMessage());
            return UserPreferences.builder()
                    .emailNotifications(false)
                    .pushNotifications(false)
                    .build();
        }
    }

    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .type(notification.getType().toString())
                .relatedEntityId(notification.getRelatedEntityId())
                .relatedEntityType(notification.getRelatedEntityType())
                .data(notification.getData())
                .priority(notification.getPriority().toString())
                .isRead(notification.isRead())
                .readAt(notification.getReadAt())
                .createdAt(notification.getCreatedAt())
                .expiresAt(notification.getExpiresAt())
                .build();
    }

    // Feign client
    @FeignClient(name = "profile-service")
    public interface ProfileServiceClient {
        @GetMapping("/profiles/user/{userId}")
        UserProfileResponse getUserProfile(@PathVariable Long userId);
    }

    // DTOs
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class NotificationRequest {
        private Long userId;
        private String type;
        private String title;
        private String message;
        private Long relatedEntityId;
        private String relatedEntityType;
        private Map<String, Object> data;
        private String priority;
        private LocalDateTime expiresAt;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class NotificationResponse {
        private String id;
        private String title;
        private String message;
        private String type;
        private Long relatedEntityId;
        private String relatedEntityType;
        private Map<String, Object> data;
        private String priority;
        private boolean isRead;
        private LocalDateTime readAt;
        private LocalDateTime createdAt;
        private LocalDateTime expiresAt;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class NotificationStatsResponse {
        private long totalNotifications;
        private long unreadNotifications;
        private long recentNotifications;
        private double readPercentage;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class UserPreferences {
        private boolean emailNotifications;
        private boolean pushNotifications;
        private String userEmail;
        private String userName;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class UserProfileResponse {
        private Long userId;
        private String firstName;
        private String lastName;
        private String email;
    }
}