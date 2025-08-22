package com.campusworks.chat.repo;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.campusworks.chat.model.ChatMessage;

import java.util.List;

public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {
    List<ChatMessage> findByTaskIdOrderBySentAtAsc(Long taskId);
}


