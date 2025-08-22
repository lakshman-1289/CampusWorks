package com.campusworks.chat.repo;

import com.campusworks.chat.domain.ChatMessage;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {
    List<ChatMessage> findByTaskIdOrderBySentAtAsc(Long taskId);
}


