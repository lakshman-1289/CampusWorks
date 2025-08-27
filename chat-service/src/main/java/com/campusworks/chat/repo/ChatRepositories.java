package com.campusworks.chat.repo;

import com.campusworks.chat.domain.Conversation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends MongoRepository<Conversation, String> {
    
    // Task-based queries
    Optional<Conversation> findByTaskId(Long taskId);
    
    List<Conversation> findByTaskIdIn(List<Long> taskIds);
    
    // Participant-based queries
    List<Conversation> findByParticipantsContaining(Long userId);
    
    List<Conversation> findByParticipantsContainingOrderByLastMessageAtDesc(Long userId);
    
    List<Conversation> findByParticipantsContainingAndIsActiveTrue(Long userId);
    
    // Status-based queries
    List<Conversation> findByIsActive(boolean isActive);
    
    // Time-based queries
    List<Conversation> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    
    List<Conversation> findByLastMessageAtBetween(LocalDateTime start, LocalDateTime end);
    
    @Query("{'lastMessageAt': {$lt: ?0}}")
    List<Conversation> findInactiveConversationsBefore(LocalDateTime cutoffDate);
    
    // Conversation type queries
    List<Conversation> findByConversationType(String conversationType);
    
    // Complex queries
    @Query("{'participants': {$in: [?0]}, 'isActive': true}")
    List<Conversation> findActiveConversationsByUser(Long userId);
    
    @Query("{'participants': {$size: ?0}}")
    List<Conversation> findByParticipantCount(int count);
    
    // Statistics
    @Query(value = "{'participants': {$in: [?0]}}", count = true)
    long countByParticipantsContaining(Long userId);
    
    @Query("{'participants': ?0}")
    List<String> findConversationIdsByParticipant(Long userId);
    
    // Admin queries
    @Query("{'createdAt': {$gte: ?0}}")
    List<Conversation> findConversationsCreatedSince(LocalDateTime since);
    
    @Query("{'taskId': {$exists: true}}")
    List<Conversation> findTaskConversations();
}

package com.campusworks.chat.repo;

import com.campusworks.chat.domain.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MessageRepository extends MongoRepository<Message, String> {
    
    // Conversation-based queries
    List<Message> findByConversationId(String conversationId);
    
    Page<Message> findByConversationIdOrderBySentAtDesc(String conversationId, Pageable pageable);
    
    List<Message> findByConversationIdOrderBySentAtAsc(String conversationId);
    
    Optional<Message> findFirstByConversationIdOrderBySentAtDesc(String conversationId);
    
    long countByConversationId(String conversationId);
    
    // Sender-based queries
    List<Message> findBySenderIdOrderBySentAtDesc(Long senderId);
    
    List<Message> findByConversationIdAndSenderId(String conversationId, Long senderId);
    
    // Read status queries
    List<Message> findByConversationIdAndIsReadFalse(String conversationId);
    
    List<Message> findByConversationIdAndIsReadFalseAndSenderIdNot(String conversationId, Long userId);
    
    long countByConversationIdAndIsReadFalse(String conversationId);
    
    @Query("{'isRead': false, 'senderId': {$ne: ?0}, 'conversationId': {$in: ?1}}")
    long countByIsReadFalseAndSenderIdNotAndConversationIdIn(Long userId, List<String> conversationIds);
    
    // Message type queries
    List<Message> findByType(Message.MessageType type);
    
    List<Message> findByConversationIdAndType(String conversationId, Message.MessageType type);
    
    // Time-based queries
    List<Message> findBySentAtBetween(LocalDateTime start, LocalDateTime end);
    
    List<Message> findByConversationIdAndSentAtBetween(String conversationId, LocalDateTime start, LocalDateTime end);
    
    @Query("{'sentAt': {$gte: ?0}}")
    List<Message> findMessagesSince(LocalDateTime since);
    
    // File-related queries
    @Query("{'fileUrl': {$exists: true, $ne: null}}")
    List<Message> findMessagesWithFiles();
    
    List<Message> findByConversationIdAndFileUrlIsNotNull(String conversationId);
    
    // Search functionality
    @Query("{'conversationId': ?0, 'content': {$regex: ?1, $options: 'i'}}")
    List<Message> searchMessagesInConversation(String conversationId, String searchTerm);
    
    @Query("{'content': {$regex: ?0, $options: 'i'}}")
    List<Message> searchMessages(String searchTerm);
    
    // Edit status queries
    List<Message> findByIsEditedTrue();
    
    List<Message> findByConversationIdAndIsEditedTrue(String conversationId);
    
    // Recent activity
    @Query("{'conversationId': ?0, 'sentAt': {$gte: ?1}}")
    List<Message> findRecentMessagesInConversation(String conversationId, LocalDateTime since);
    
    // Statistics
    @Query(value = "{'senderId': ?0}", count = true)
    long countBySenderId(Long senderId);
    
    @Query(value = "{'conversationId': ?0, 'senderId': ?1}", count = true)
    long countByConversationIdAndSenderId(String conversationId, Long senderId);
    
    // Cleanup queries
    @Query("{'sentAt': {$lt: ?0}}")
    List<Message> findOldMessages(LocalDateTime cutoffDate);
    
    void deleteByConversationId(String conversationId);
}