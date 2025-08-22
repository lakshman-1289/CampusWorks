package com.campusworks.chat.controller;

import com.campusworks.chat.model.ChatMessage;
import com.campusworks.chat.repo.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {
    private final ChatMessageRepository repository;
    private final SimpMessagingTemplate messagingTemplate;

    @GetMapping("/task/{taskId}")
    public List<ChatMessage> history(@PathVariable Long taskId) { return repository.findByTaskIdOrderBySentAtAsc(taskId); }

    @PostMapping
    public ChatMessage send(@RequestBody ChatMessage msg) {
        msg.setSentAt(Instant.now());
        ChatMessage saved = repository.save(msg);
        messagingTemplate.convertAndSend("/topic/task." + saved.getTaskId(), saved);
        return saved;
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        if (!repository.existsById(id)) return ResponseEntity.notFound().build();
        repository.deleteById(id); return ResponseEntity.noContent().build();
    }
}


