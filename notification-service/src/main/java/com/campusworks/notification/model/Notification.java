package com.campusworks.notification.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "notifications")
public class Notification {
    @Id
    private String id;
    private Long userId;
    private String type; // EMAIL, OTP
    private String message;
    private boolean delivered;
    private Instant createdAt;
}


