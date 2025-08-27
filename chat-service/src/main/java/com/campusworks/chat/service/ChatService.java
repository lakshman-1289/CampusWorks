package com.campusworks.chat.service;

import com.campusworks.chat.domain.Conversation;
import com.campusworks.chat.domain.Message;
import com.campusworks.chat.repo.ConversationRepository;
import com.campusworks.chat.repo.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationServiceClient notificationServiceClient;
    private final ProfileServiceClient profileServiceClient;

    public ConversationResponse createOrGetConversation(Long taskId, Long userId) {
        log.info("Creating or getting conversation for task {} and user {}", taskId, userId);

        // Check if conversation already exists for this task
        Optional<Conversation> existingConversation = conversationRepository.findByTaskId(taskId);
        
        if (existingConversation.isPresent()) {
            Conversation conversation = existingConversation.get();
            
            // Add user to participants if not already present
            if (!conversation.getParticipants().contains(userId)) {
                conversation.getParticipants().add(userId);
                conversation = conversationRepository.save(conversation);
            }
            
            return mapToConversationResponse(conversation);
        }

        // Create new conversation
        Conversation conversation = Conversation.builder()
                .taskId(taskId)
                .participants(List.of(userId)) // Will be expanded when task owner joins
                .isActive(true)
                .conversationType("TASK_DISCUSSION")
                .build();

        Conversation savedConversation = conversationRepository.save(conversation);
        
        log.info("Created new conversation: {}", savedConversation.getId());
        return mapToConversationResponse(savedConversation);
    }

    public MessageResponse sendMessage(SendMessageRequest request, Long senderId) {
        log.info("Sending message in conversation {} by user {}", request.getConversationId(), senderId);

        // Validate conversation and user access
        Conversation conversation = conversationRepository.findById(request.getConversationId())
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        if (!conversation.getParticipants().contains(senderId)) {
            throw new RuntimeException("User not authorized to send messages in this conversation");
        }

        // Create message
        Message message = Message.builder()
                .conversationId(request.getConversationId())
                .senderId(senderId)
                .content(request.getContent())
                .type(request.getType() != null ? request.getType() : Message.MessageType.TEXT)
                .fileUrl(request.getFileUrl())
                .fileName(request.getFileName())
                .fileSize(request.getFileSize())
                .build();

        Message savedMessage = messageRepository.save(message);

        // Update conversation last message time
        conversation.setLastMessageAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        // Send real-time message to participants
        MessageResponse messageResponse = mapToMessageResponse(savedMessage);
        sendRealTimeMessage(conversation, messageResponse);

        // Send notifications to other participants
        sendMessageNotifications(conversation, savedMessage);

        log.info("Message sent successfully: {}", savedMessage.getId());
        return messageResponse;
    }

    public Page<MessageResponse> getMessages(String conversationId, Long userId, int page, int size) {
        log.info("Getting messages for conversation {} by user {}", conversationId, userId);

        // Validate user access
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        if (!conversation.getParticipants().contains(userId)) {
            throw new RuntimeException("User not authorized to view this conversation");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "sentAt"));
        Page<Message> messages = messageRepository.findByConversationIdOrderBySentAtDesc(conversationId, pageable);

        return messages.map(this::mapToMessageResponse);
    }

    public List<ConversationResponse> getUserConversations(Long userId) {
        log.info("Getting conversations for user {}", userId);

        List<Conversation> conversations = conversationRepository.findByParticipantsContainingOrderByLastMessageAtDesc(userId);
        return conversations.stream()
                .map(this::mapToConversationResponse)
                .toList();
    }

    public void markMessagesAsRead(String conversationId, Long userId) {
        log.info("Marking messages as read in conversation {} by user {}", conversationId, userId);

        // Validate user access
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        if (!conversation.getParticipants().contains(userId)) {
            throw new RuntimeException("User not authorized to access this conversation");
        }

        // Mark unread messages as read
        List<Message> unreadMessages = messageRepository.findByConversationIdAndIsReadFalseAndSenderIdNot(
                conversationId, userId);

        for (Message message : unreadMessages) {
            message.setRead(true);
            message.setReadAt(LocalDateTime.now());
        }

        if (!unreadMessages.isEmpty()) {
            messageRepository.saveAll(unreadMessages);
        }
    }

    public MessageResponse editMessage(String messageId, String newContent, Long userId) {
        log.info("Editing message {} by user {}", messageId, userId);

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        if (!message.getSenderId().equals(userId)) {
            throw new RuntimeException("Can only edit your own messages");
        }

        // Check if message is not too old (e.g., 15 minutes)
        if (message.getSentAt().isBefore(LocalDateTime.now().minusMinutes(15))) {
            throw new RuntimeException("Cannot edit messages older than 15 minutes");
        }

        message.setContent(newContent);
        message.setEdited(true);
        message.setEditedAt(LocalDateTime.now());

        Message savedMessage = messageRepository.save(message);
        
        // Send real-time update
        MessageResponse messageResponse = mapToMessageResponse(savedMessage);
        sendRealTimeMessageUpdate(message.getConversationId(), messageResponse);

        return messageResponse;
    }

    public void deleteMessage(String messageId, Long userId) {
        log.info("Deleting message {} by user {}", messageId, userId);

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        if (!message.getSenderId().equals(userId)) {
            throw new RuntimeException("Can only delete your own messages");
        }

        messageRepository.delete(message);

        // Send real-time deletion update
        sendRealTimeMessageDeletion(message.getConversationId(), messageId);
    }

    public ConversationResponse addParticipant(String conversationId, Long participantId, Long requesterId) {
        log.info("Adding participant {} to conversation {} by user {}", participantId, conversationId, requesterId);

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        if (!conversation.getParticipants().contains(requesterId)) {
            throw new RuntimeException("Only participants can add new members");
        }

        if (!conversation.getParticipants().contains(participantId)) {
            conversation.getParticipants().add(participantId);
            conversation = conversationRepository.save(conversation);

            // Send system message about new participant
            sendSystemMessage(conversation, requesterId + " added " + participantId + " to the conversation");
        }

        return mapToConversationResponse(conversation);
    }

    public long getUnreadMessageCount(Long userId) {
        return messageRepository.countByIsReadFalseAndSenderIdNotAndConversationIdIn(
                userId, conversationRepository.findConversationIdsByParticipant(userId));
    }

    private void sendRealTimeMessage(Conversation conversation, MessageResponse message) {
        try {
            for (Long participantId : conversation.getParticipants()) {
                messagingTemplate.convertAndSendToUser(
                        participantId.toString(),
                        "/queue/messages",
                        message
                );
            }
        } catch (Exception e) {
            log.error("Failed to send real-time message: {}", e.getMessage());
        }
    }

    private void sendRealTimeMessageUpdate(String conversationId, MessageResponse message) {
        try {
            messagingTemplate.convertAndSend("/topic/conversation/" + conversationId + "/update", message);
        } catch (Exception e) {
            log.error("Failed to send real-time message update: {}", e.getMessage());
        }
    }

    private void sendRealTimeMessageDeletion(String conversationId, String messageId) {
        try {
            messagingTemplate.convertAndSend("/topic/conversation/" + conversationId + "/delete", messageId);
        } catch (Exception e) {
            log.error("Failed to send real-time message deletion: {}", e.getMessage());
        }
    }

    private void sendSystemMessage(Conversation conversation, String content) {
        Message systemMessage = Message.builder()
                .conversationId(conversation.getId())
                .senderId(0L) // System user ID
                .content(content)
                .type(Message.MessageType.SYSTEM)
                .build();

        Message savedMessage = messageRepository.save(systemMessage);
        MessageResponse messageResponse = mapToMessageResponse(savedMessage);
        sendRealTimeMessage(conversation, messageResponse);
    }

    private void sendMessageNotifications(Conversation conversation, Message message) {
        try {
            // Get sender name
            String senderName = getUserName(message.getSenderId());

            // Notify all participants except sender
            for (Long participantId : conversation.getParticipants()) {
                if (!participantId.equals(message.getSenderId())) {
                    NotificationRequest notification = NotificationRequest.builder()
                            .userId(participantId)
                            .type("NEW_MESSAGE")
                            .title("New Message from " + senderName)
                            .message(message.getContent().length() > 50 ? 
                                    message.getContent().substring(0, 50) + "..." : 
                                    message.getContent())
                            .relatedEntityId(conversation.getTaskId())
                            .relatedEntityType("CHAT")
                            .build();

                    notificationServiceClient.sendNotification(notification);
                }
            }
        } catch (Exception e) {
            log.error("Failed to send message notifications: {}", e.getMessage());
        }
    }

    private String getUserName(Long userId) {
        try {
            if (userId == 0L) return "System";
            UserProfileResponse profile = profileServiceClient.getUserProfile(userId);
            return profile.getFirstName() + " " + profile.getLastName();
        } catch (Exception e) {
            return "Unknown User";
        }
    }

    private ConversationResponse mapToConversationResponse(Conversation conversation) {
        // Get participants info
        List<UserInfo> participantInfos = conversation.getParticipants().stream()
                .map(this::getUserInfo)
                .toList();

        // Get last message
        Optional<Message> lastMessage = messageRepository.findFirstByConversationIdOrderBySentAtDesc(conversation.getId());

        return ConversationResponse.builder()
                .id(conversation.getId())
                .taskId(conversation.getTaskId())
                .participants(participantInfos)
                .lastMessage(lastMessage.map(this::mapToMessageResponse).orElse(null))
                .lastMessageAt(conversation.getLastMessageAt())
                .isActive(conversation.isActive())
                .createdAt(conversation.getCreatedAt())
                .build();
    }

    private MessageResponse mapToMessageResponse(Message message) {
        String senderName = getUserName(message.getSenderId());

        return MessageResponse.builder()
                .id(message.getId())
                .conversationId(message.getConversationId())
                .senderId(message.getSenderId())
                .senderName(senderName)
                .content(message.getContent())
                .type(message.getType().toString())
                .fileUrl(message.getFileUrl())
                .fileName(message.getFileName())
                .fileSize(message.getFileSize())
                .sentAt(message.getSentAt())
                .isRead(message.isRead())
                .readAt(message.getReadAt())
                .isEdited(message.isEdited())
                .editedAt(message.getEditedAt())
                .build();
    }

    private UserInfo getUserInfo(Long userId) {
        try {
            if (userId == 0L) {
                return UserInfo.builder()
                        .id(0L)
                        .name("System")
                        .email("system@campusworks.com")
                        .build();
            }

            UserProfileResponse profile = profileServiceClient.getUserProfile(userId);
            return UserInfo.builder()
                    .id(userId)
                    .name(profile.getFirstName() + " " + profile.getLastName())
                    .email(profile.getEmail())
                    .build();
        } catch (Exception e) {
            return UserInfo.builder()
                    .id(userId)
                    .name("Unknown User")
                    .email("unknown@example.com")
                    .build();
        }
    }

    // Feign clients
    @FeignClient(name = "notification-service")
    public interface NotificationServiceClient {
        @PostMapping("/notifications")
        void sendNotification(@RequestBody NotificationRequest request);
    }

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
    public static class SendMessageRequest {
        private String conversationId;
        private String content;
        private Message.MessageType type;
        private String fileUrl;
        private String fileName;
        private Long fileSize;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ConversationResponse {
        private String id;
        private Long taskId;
        private List<UserInfo> participants;
        private MessageResponse lastMessage;
        private LocalDateTime lastMessageAt;
        private boolean isActive;
        private LocalDateTime createdAt;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class MessageResponse {
        private String id;
        private String conversationId;
        private Long senderId;
        private String senderName;
        private String content;
        private String type;
        private String fileUrl;
        private String fileName;
        private Long fileSize;
        private LocalDateTime sentAt;
        private boolean isRead;
        private LocalDateTime readAt;
        private boolean isEdited;
        private LocalDateTime editedAt;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class UserInfo {
        private Long id;
        private String name;
        private String email;
    }

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