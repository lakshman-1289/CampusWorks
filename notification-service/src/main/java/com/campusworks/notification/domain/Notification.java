package com.campusworks.notification.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {
    @Id
    private String id;

    private Long userId; // Recipient user ID

    private String title;

    private String message;

    private NotificationType type;

    private Long relatedEntityId; // ID of related task, bid, payment, etc.

    private String relatedEntityType; // TASK, BID, PAYMENT, etc.

    private Map<String, Object> data; // Additional data for the notification

    private boolean isRead = false;

    private LocalDateTime readAt;

    private NotificationPriority priority = NotificationPriority.NORMAL;

    private boolean emailSent = false;

    private boolean pushSent = false;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime expiresAt; // Optional expiration date

    public enum NotificationType {
        // Task related
        TASK_CREATED,
        TASK_ASSIGNED,
        TASK_COMPLETED,
        TASK_CANCELLED,
        
        // Bidding related
        NEW_BID_RECEIVED,
        BID_WON,
        BID_LOST,
        BIDDING_ENDED,
        
        // Payment related
        PAYMENT_RECEIVED,
        PAYMENT_RELEASED,
        PAYMENT_FAILED,
        
        // Chat related
        NEW_MESSAGE,
        
        // System related
        ACCOUNT_VERIFIED,
        PROFILE_UPDATED,
        SYSTEM_MAINTENANCE,
        
        // General
        REMINDER,
        WARNING,
        INFO
    }

    public enum NotificationPriority {
        LOW,
        NORMAL,
        HIGH,
        URGENT
    }
}