package com.campusworks.chat.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "messages")
public class ChatMessage {
    @Id
    private String id;
    private Long taskId;
    private Long fromUserId;
    private Long toUserId;
    private String content;
    private Instant sentAt;
}


