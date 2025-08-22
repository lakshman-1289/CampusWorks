package com.campusworks.notification.repo;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.campusworks.notification.model.Notification;

import java.util.List;

public interface NotificationRepository extends MongoRepository<Notification, String> {
    List<Notification> findByUserId(Long userId);
}


