package com.campusworks.task.web;

import com.campusworks.task.dto.TaskDTOs.*;
import com.campusworks.task.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class TaskController {

    private final TaskService taskService;

    // Public endpoints for browsing tasks
    @GetMapping("/public")
    public ResponseEntity<Page<TaskResponse>> getOpenTasks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String subject) {
        Page<TaskResponse> tasks = taskService.getOpenTasks(page, size, category, subject);
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/public/search")
    public ResponseEntity<Page<TaskResponse>> searchTasks(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<TaskResponse> tasks = taskService.searchTasks(keyword, page, size);
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/public/{id}")
    public ResponseEntity<TaskResponse> getTaskById(@PathVariable Long id) {
        TaskResponse task = taskService.getTaskById(id);
        return ResponseEntity.ok(task);
    }

    // Authenticated endpoints for task management
    @PostMapping
    public ResponseEntity<TaskResponse> createTask(
            @Valid @RequestBody CreateTaskRequest request,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        TaskResponse response = taskService.createTask(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<TaskResponse> publishTask(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        TaskResponse response = taskService.publishTask(id, userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTaskRequest request,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        TaskResponse response = taskService.updateTask(id, request, userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelTask(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        taskService.cancelTask(id, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/my-tasks")
    public ResponseEntity<List<TaskResponse>> getMyTasks(
            @RequestParam(required = false) String status,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        List<TaskResponse> tasks = taskService.getMyTasks(userId, status);
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/assigned-to-me")
    public ResponseEntity<List<TaskResponse>> getAssignedTasks(
            @RequestParam(required = false) String status,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        List<TaskResponse> tasks = taskService.getAssignedTasks(userId, status);
        return ResponseEntity.ok(tasks);
    }

    // Task assignment endpoint (called by bidding service)
    @PostMapping("/{id}/assign")
    public ResponseEntity<TaskResponse> assignTask(
            @PathVariable Long id,
            @RequestBody TaskService.TaskAssignmentRequest request) {
        TaskResponse response = taskService.assignTask(id, request);
        return ResponseEntity.ok(response);
    }

    // Work submission endpoint
    @PostMapping("/{id}/submit-work")
    public ResponseEntity<TaskResponse> submitWork(
            @PathVariable Long id,
            @Valid @RequestBody WorkSubmissionRequest request,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        TaskResponse response = taskService.submitWork(id, request, userId);
        return ResponseEntity.ok(response);
    }

    // Work review endpoint
    @PostMapping("/{id}/review-work")
    public ResponseEntity<TaskResponse> reviewWork(
            @PathVariable Long id,
            @Valid @RequestBody WorkAcceptanceRequest request,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        TaskResponse response = taskService.reviewWork(id, request, userId);
        return ResponseEntity.ok(response);
    }

    // Utility endpoints
    @GetMapping("/expired-bidding")
    public ResponseEntity<List<TaskResponse>> getTasksWithExpiredBidding() {
        // This endpoint is for internal service communication
        // In a real implementation, you'd add service-to-service authentication
        // For now, we'll simulate this functionality
        return ResponseEntity.ok(List.of()); // Placeholder
    }

    @PostMapping("/{id}/status")
    public ResponseEntity<Void> updateTaskStatus(
            @PathVariable Long id,
            @RequestBody String status) {
        // This endpoint is for internal service communication
        // Implementation would update task status directly
        return ResponseEntity.ok().build();
    }

    // Statistics endpoints
    @GetMapping("/stats/categories")
    public ResponseEntity<Object> getTaskStatsByCategory() {
        // Implementation for task statistics by category
        return ResponseEntity.ok("{}");
    }

    @GetMapping("/stats/my-stats")
    public ResponseEntity<Object> getMyTaskStats(Authentication authentication) {
        // Implementation for user's task statistics
        return ResponseEntity.ok("{}");
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new RuntimeException("User not authenticated");
        }
        return Long.valueOf(authentication.getName());
    }
}