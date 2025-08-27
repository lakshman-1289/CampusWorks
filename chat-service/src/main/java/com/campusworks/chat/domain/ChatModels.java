package com.campusworks.chat.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "conversations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conversation {
    @Id
    private String id;

    private Long taskId; // Reference to the task this conversation is about

    private List<Long> participants; // User IDs participating in conversation

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime lastMessageAt;

    private boolean isActive = true;

    private String conversationType = "TASK_DISCUSSION"; // TASK_DISCUSSION, SUPPORT, etc.
}

@Document(collection = "messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {
    @Id
    private String id;

    private String conversationId;

    private Long senderId;

    private String content;

    private MessageType type = MessageType.TEXT;

    private String fileUrl; // For file attachments

    private String fileName;

    private Long fileSize;

    private LocalDateTime sentAt = LocalDateTime.now();

    private boolean isRead = false;

    private LocalDateTime readAt;

    private boolean isEdited = false;

    private LocalDateTime editedAt;

    public enum MessageType {
        TEXT,
        FILE,
        IMAGE,
        SYSTEM // For system-generated messages
    }
}