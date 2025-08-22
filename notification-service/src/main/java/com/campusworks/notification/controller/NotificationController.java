package com.campusworks.notification.controller;

import com.campusworks.notification.model.Notification;
import com.campusworks.notification.repo.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationRepository repository;

    @GetMapping
    public List<Notification> all() { return repository.findAll(); }

    @GetMapping("/user/{userId}")
    public List<Notification> byUser(@PathVariable Long userId) { return repository.findByUserId(userId); }

    @PostMapping
    public Notification create(@RequestBody Notification n) {
        n.setCreatedAt(Instant.now()); n.setDelivered(false);
        return repository.save(n);
    }

    @PostMapping("/{id}/deliver")
    public ResponseEntity<Notification> markDelivered(@PathVariable String id) {
        return repository.findById(id).map(ex -> { ex.setDelivered(true); return ResponseEntity.ok(repository.save(ex)); })
                .orElse(ResponseEntity.notFound().build());
    }
}


