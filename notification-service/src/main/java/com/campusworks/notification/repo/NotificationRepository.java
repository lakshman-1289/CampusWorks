package com.campusworks.notification.repo;

import com.campusworks.notification.domain.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {
    
    // User-based queries
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    List<Notification> findByUserIdAndIsReadFalse(Long userId);
    
    Page<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    List<Notification> findByUserIdAndIsReadTrue(Long userId);
    
    long countByUserId(Long userId);
    
    long countByUserIdAndIsReadFalse(Long userId);
    
    long countByUserIdAndCreatedAtAfter(Long userId, LocalDateTime after);
    
    // Type-based queries
    List<Notification> findByType(Notification.NotificationType type);
    
    List<Notification> findByUserIdAndType(Long userId, Notification.NotificationType type);
    
    List<Notification> findByTypeOrderByCreatedAtDesc(Notification.NotificationType type);
    
    // Priority-based queries
    List<Notification> findByPriority(Notification.NotificationPriority priority);
    
    List<Notification> findByUserIdAndPriority(Long userId, Notification.NotificationPriority priority);
    
    // Related entity queries
    List<Notification> findByRelatedEntityIdAndRelatedEntityType(Long entityId, String entityType);
    
    List<Notification> findByUserIdAndRelatedEntityIdAndRelatedEntityType(
            Long userId, Long entityId, String entityType);
    
    List<Notification> findByRelatedEntityType(String entityType);
    
    // Time-based queries
    List<Notification> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    
    List<Notification> findByCreatedAtAfter(LocalDateTime after);
    
    List<Notification> findByCreatedAtBefore(LocalDateTime before);
    
    // Expiration queries
    List<Notification> findByExpiresAtBefore(LocalDateTime now);
    
    List<Notification> findByExpiresAtBetween(LocalDateTime start, LocalDateTime end);
    
    @Query("{'expiresAt': {$exists: true, $ne: null}}")
    List<Notification> findNotificationsWithExpiry();
    
    // Read status and time queries
    List<Notification> findByIsReadTrueAndCreatedAtBefore(LocalDateTime cutoffDate);
    
    List<Notification> findByIsReadFalseAndCreatedAtBefore(LocalDateTime cutoffDate);
    
    List<Notification> findByReadAtBetween(LocalDateTime start, LocalDateTime end);
    
    // Email and push status queries
    List<Notification> findByEmailSentFalse();
    
    List<Notification> findByPushSentFalse();
    
    List<Notification> findByEmailSentFalseAndCreatedAtBefore(LocalDateTime cutoffTime);
    
    List<Notification> findByPushSentFalseAndCreatedAtBefore(LocalDateTime cutoffTime);
    
    // Complex queries
    @Query("{'userId': ?0, 'isRead': false, 'priority': {$in: ['HIGH', 'URGENT']}}")
    List<Notification> findHighPriorityUnreadByUser(Long userId);
    
    @Query("{'userId': ?0, 'createdAt': {$gte: ?1}, 'isRead': false}")
    List<Notification> findRecentUnreadByUser(Long userId, LocalDateTime since);
    
    @Query("{'type': {$in: ['PAYMENT_COMPLETED', 'PAYMENT_RECEIVED', 'PAYMENT_RELEASED']}}")
    List<Notification> findPaymentNotifications();
    
    @Query("{'type': {$in: ['TASK_ASSIGNED', 'TASK_COMPLETED', 'WORK_SUBMITTED']}}")
    List<Notification> findTaskNotifications();
    
    @Query("{'type': {$in: ['NEW_BID_RECEIVED', 'BID_WON', 'BID_LOST']}}")
    List<Notification> findBiddingNotifications();
    
    // Statistics queries
    @Query(value = "{'userId': ?0, 'type': ?1}", count = true)
    long countByUserIdAndType(Long userId, Notification.NotificationType type);
    
    @Query(value = "{'createdAt': {$gte: ?0}}", count = true)
    long countCreatedSince(LocalDateTime since);
    
    @Query(value = "{'isRead': true, 'readAt': {$gte: ?0}}", count = true)
    long countReadSince(LocalDateTime since);
    
    // Aggregation-like queries
    @Query("{'type': ?0, 'createdAt': {$gte: ?1, $lte: ?2}}")
    List<Notification> findByTypeAndCreatedAtBetween(
            Notification.NotificationType type, LocalDateTime start, LocalDateTime end);
    
    @Query("{'userId': ?0, 'priority': ?1, 'isRead': false}")
    List<Notification> findUnreadByUserAndPriority(Long userId, Notification.NotificationPriority priority);
    
    // Cleanup queries
    @Query("{'createdAt': {$lt: ?0}}")
    List<Notification> findOldNotifications(LocalDateTime cutoffDate);
    
    @Query("{'isRead': true, 'readAt': {$lt: ?0}}")
    List<Notification> findOldReadNotifications(LocalDateTime cutoffDate);
    
    // Batch operations support
    @Query("{'userId': {$in: ?0}}")
    List<Notification> findByUserIdIn(List<Long> userIds);
    
    @Query("{'relatedEntityId': {$in: ?0}, 'relatedEntityType': ?1}")
    List<Notification> findByRelatedEntityIdInAndRelatedEntityType(List<Long> entityIds, String entityType);
}