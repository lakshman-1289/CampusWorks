package com.campusworks.task.service;

import com.campusworks.task.domain.Task;
import com.campusworks.task.dto.TaskDTOs.*;
import com.campusworks.task.exception.TaskException;
import com.campusworks.task.repo.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProfileServiceClient profileServiceClient;
    private final NotificationServiceClient notificationServiceClient;
    private final PaymentServiceClient paymentServiceClient;

    public TaskResponse createTask(CreateTaskRequest request, Long ownerId) {
        log.info("Creating task for user {}", ownerId);

        // Validate user profile
        UserProfileResponse profile = profileServiceClient.getUserProfile(ownerId);
        if (!profile.isProfileCompleted()) {
            throw new TaskException("Please complete your profile before posting tasks");
        }

        // Create task
        Task task = Task.builder()
                .ownerId(ownerId)
                .title(request.getTitle())
                .description(request.getDescription())
                .budget(request.getBudget())
                .requirements(request.getRequirements())
                .category(request.getCategory())
                .priority(request.getPriority())
                .deadline(request.getDeadline())
                .biddingDeadline(request.getBiddingDeadline())
                .status(Task.TaskStatus.DRAFT)
                .build();

        Task savedTask = taskRepository.save(task);
        
        log.info("Task created with ID: {}", savedTask.getId());
        return mapToResponse(savedTask);
    }

    public TaskResponse publishTask(Long taskId, Long ownerId) {
        log.info("Publishing task {} by user {}", taskId, ownerId);

        Task task = getTaskByIdAndOwner(taskId, ownerId);
        
        if (task.getStatus() != Task.TaskStatus.DRAFT) {
            throw new TaskException("Only draft tasks can be published");
        }

        if (task.getBiddingDeadline().isBefore(LocalDateTime.now().plusHours(1))) {
            throw new TaskException("Bidding deadline must be at least 1 hour from now");
        }

        task.setStatus(Task.TaskStatus.OPEN);
        Task savedTask = taskRepository.save(task);

        // Send notification about new task
        sendTaskNotification(savedTask, "TASK_CREATED");

        return mapToResponse(savedTask);
    }

    public TaskResponse updateTask(Long taskId, UpdateTaskRequest request, Long ownerId) {
        log.info("Updating task {} by user {}", taskId, ownerId);

        Task task = getTaskByIdAndOwner(taskId, ownerId);
        
        if (task.getStatus() != Task.TaskStatus.DRAFT && task.getStatus() != Task.TaskStatus.OPEN) {
            throw new TaskException("Cannot update task in current status");
        }

        // Update fields
        if (request.getTitle() != null) {
            task.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            task.setDescription(request.getDescription());
        }
        if (request.getRequirements() != null) {
            task.setRequirements(request.getRequirements());
        }
        if (request.getPriority() != null) {
            task.setPriority(request.getPriority());
        }
        if (request.getDeadline() != null) {
            task.setDeadline(request.getDeadline());
        }

        Task savedTask = taskRepository.save(task);
        return mapToResponse(savedTask);
    }

    public void cancelTask(Long taskId, Long ownerId) {
        log.info("Cancelling task {} by user {}", taskId, ownerId);

        Task task = getTaskByIdAndOwner(taskId, ownerId);
        
        if (task.getStatus() == Task.TaskStatus.COMPLETED || 
            task.getStatus() == Task.TaskStatus.CANCELLED) {
            throw new TaskException("Cannot cancel task in current status");
        }

        task.setStatus(Task.TaskStatus.CANCELLED);
        taskRepository.save(task);

        // Handle refunds if payment was made
        if (task.getStatus() == Task.TaskStatus.ASSIGNED || 
            task.getStatus() == Task.TaskStatus.IN_PROGRESS) {
            initiateRefund(task);
        }

        sendTaskNotification(task, "TASK_CANCELLED");
    }

    public TaskResponse assignTask(Long taskId, TaskAssignmentRequest request) {
        log.info("Assigning task {} to user {}", taskId, request.getAssignedUserId());

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskException("Task not found"));

        if (task.getStatus() != Task.TaskStatus.BIDDING_CLOSED) {
            throw new TaskException("Task is not ready for assignment");
        }

        task.setAssignedUserId(request.getAssignedUserId());
        task.setFinalPrice(request.getFinalPrice());
        task.setStatus(Task.TaskStatus.ASSIGNED);

        Task savedTask = taskRepository.save(task);

        // Initiate payment escrow
        initiateEscrowPayment(savedTask);

        // Send notifications
        sendTaskAssignmentNotifications(savedTask);

        return mapToResponse(savedTask);
    }

    public TaskResponse submitWork(Long taskId, WorkSubmissionRequest request, Long assigneeId) {
        log.info("Submitting work for task {} by user {}", taskId, assigneeId);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskException("Task not found"));

        if (!task.getAssignedUserId().equals(assigneeId)) {
            throw new TaskException("You are not assigned to this task");
        }

        if (task.getStatus() != Task.TaskStatus.ASSIGNED && 
            task.getStatus() != Task.TaskStatus.IN_PROGRESS) {
            throw new TaskException("Task is not in a submittable state");
        }

        task.setWorkSubmissionUrl(request.getWorkSubmissionUrl());
        task.setWorkSubmissionNotes(request.getWorkSubmissionNotes());
        task.setWorkSubmittedAt(LocalDateTime.now());
        task.setStatus(Task.TaskStatus.SUBMITTED);

        Task savedTask = taskRepository.save(task);

        // Notify task owner
        sendWorkSubmissionNotification(savedTask);

        return mapToResponse(savedTask);
    }

    public TaskResponse reviewWork(Long taskId, WorkAcceptanceRequest request, Long ownerId) {
        log.info("Reviewing work for task {} by user {}", taskId, ownerId);

        Task task = getTaskByIdAndOwner(taskId, ownerId);

        if (task.getStatus() != Task.TaskStatus.SUBMITTED) {
            throw new TaskException("No work has been submitted for this task");
        }

        if (request.isAccepted()) {
            task.setWorkAccepted(true);
            task.setWorkAcceptedAt(LocalDateTime.now());
            task.setStatus(Task.TaskStatus.COMPLETED);

            // Release payment to assignee
            releasePayment(task);

            // Update profile statistics
            updateProfileStatistics(task);

            sendWorkAcceptanceNotification(task, true);
        } else {
            task.setWorkAccepted(false);
            task.setRejectionReason(request.getRejectionReason());
            task.setStatus(Task.TaskStatus.REJECTED);

            sendWorkAcceptanceNotification(task, false);
        }

        Task savedTask = taskRepository.save(task);
        return mapToResponse(savedTask);
    }

    // Public browsing methods
    public Page<TaskResponse> getOpenTasks(int page, int size, String category, String subject) {
        Pageable pageable = PageRequest.of(page, size);
        LocalDateTime now = LocalDateTime.now();
        
        Page<Task> tasks;
        if (category != null) {
            tasks = taskRepository.findOpenTasksByCategory(now, category, pageable);
        } else if (subject != null) {
            tasks = taskRepository.findOpenTasksBySubject(now, subject, pageable);
        } else {
            tasks = taskRepository.findOpenTasksForBidding(now, pageable);
        }
        
        return tasks.map(this::mapToResponse);
    }

    public Page<TaskResponse> searchTasks(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        LocalDateTime now = LocalDateTime.now();
        Page<Task> tasks = taskRepository.searchOpenTasks(now, keyword, pageable);
        return tasks.map(this::mapToResponse);
    }

    public TaskResponse getTaskById(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskException("Task not found"));
        return mapToResponse(task);
    }

    public List<TaskResponse> getMyTasks(Long userId, String status) {
        List<Task> tasks;
        if (status != null) {
            Task.TaskStatus taskStatus = Task.TaskStatus.valueOf(status.toUpperCase());
            tasks = taskRepository.findByOwnerIdAndStatus(userId, taskStatus);
        } else {
            tasks = taskRepository.findByOwnerIdOrderByCreatedAtDesc(userId);
        }
        return tasks.stream().map(this::mapToResponse).toList();
    }

    public List<TaskResponse> getAssignedTasks(Long userId, String status) {
        List<Task> tasks;
        if (status != null) {
            Task.TaskStatus taskStatus = Task.TaskStatus.valueOf(status.toUpperCase());
            tasks = taskRepository.findByAssignedUserIdAndStatus(userId, taskStatus);
        } else {
            tasks = taskRepository.findByAssignedUserId(userId);
        }
        return tasks.stream().map(this::mapToResponse).toList();
    }

    // Scheduled tasks for automated workflow management
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void processBiddingDeadlines() {
        LocalDateTime now = LocalDateTime.now();
        List<Task> expiredTasks = taskRepository.findTasksWithExpiredBidding(now);
        
        for (Task task : expiredTasks) {
            task.setStatus(Task.TaskStatus.BIDDING_CLOSED);
            taskRepository.save(task);
            log.info("Bidding closed for task {}", task.getId());
        }
    }

    @Scheduled(fixedRate = 3600000) // Every hour
    public void checkOverdueTasks() {
        LocalDateTime now = LocalDateTime.now();
        List<Task> overdueTasks = taskRepository.findOverdueTasks(now);
        
        for (Task task : overdueTasks) {
            sendOverdueNotification(task);
        }
    }

    // Helper methods
    private Task getTaskByIdAndOwner(Long taskId, Long ownerId) {
        return taskRepository.findById(taskId)
                .filter(task -> task.getOwnerId().equals(ownerId))
                .orElseThrow(() -> new TaskException("Task not found or access denied"));
    }

    private TaskResponse mapToResponse(Task task) {
        // Get additional data for response
        String ownerName = getOwnerName(task.getOwnerId());
        String assignedUserName = task.getAssignedUserId() != null ? 
                getOwnerName(task.getAssignedUserId()) : null;

        return TaskResponse.builder()
                .id(task.getId())
                .ownerId(task.getOwnerId())
                .ownerName(ownerName)
                .title(task.getTitle())
                .description(task.getDescription())
                .budget(task.getBudget())
                .requirements(task.getRequirements())
                .status(task.getStatus())
                .category(task.getCategory())
                .priority(task.getPriority())
                .deadline(task.getDeadline())
                .biddingDeadline(task.getBiddingDeadline())
                .assignedUserId(task.getAssignedUserId())
                .assignedUserName(assignedUserName)
                .finalPrice(task.getFinalPrice())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }

    private String getOwnerName(Long userId) {
        try {
            UserProfileResponse profile = profileServiceClient.getUserProfile(userId);
            return profile.getFirstName() + " " + profile.getLastName();
        } catch (Exception e) {
            return "Unknown User";
        }
    }

    @Async
    private void sendTaskNotification(Task task, String type) {
        try {
            NotificationRequest notification = NotificationRequest.builder()
                    .userId(task.getOwnerId())
                    .type(type)
                    .title(getNotificationTitle(type))
                    .message(getNotificationMessage(type, task))
                    .relatedEntityId(task.getId())
                    .relatedEntityType("TASK")
                    .build();
            
            notificationServiceClient.sendNotification(notification);
        } catch (Exception e) {
            log.error("Failed to send task notification: {}", e.getMessage());
        }
    }

    @Async
    private void sendTaskAssignmentNotifications(Task task) {
        // Notify task owner
        NotificationRequest ownerNotification = NotificationRequest.builder()
                .userId(task.getOwnerId())
                .type("TASK_ASSIGNED")
                .title("Task Assigned")
                .message("Your task has been assigned and work will begin soon.")
                .relatedEntityId(task.getId())
                .relatedEntityType("TASK")
                .build();
        
        // Notify assignee
        NotificationRequest assigneeNotification = NotificationRequest.builder()
                .userId(task.getAssignedUserId())
                .type("TASK_ASSIGNED")
                .title("Task Assignment Confirmed")
                .message("You have been assigned a new task. Get started!")
                .relatedEntityId(task.getId())
                .relatedEntityType("TASK")
                .build();

        try {
            notificationServiceClient.sendNotification(ownerNotification);
            notificationServiceClient.sendNotification(assigneeNotification);
        } catch (Exception e) {
            log.error("Failed to send assignment notifications: {}", e.getMessage());
        }
    }

    @Async
    private void sendWorkSubmissionNotification(Task task) {
        try {
            NotificationRequest notification = NotificationRequest.builder()
                    .userId(task.getOwnerId())
                    .type("WORK_SUBMITTED")
                    .title("Work Submitted")
                    .message("Work has been submitted for your task. Please review it.")
                    .relatedEntityId(task.getId())
                    .relatedEntityType("TASK")
                    .build();
            
            notificationServiceClient.sendNotification(notification);
        } catch (Exception e) {
            log.error("Failed to send work submission notification: {}", e.getMessage());
        }
    }

    @Async
    private void sendWorkAcceptanceNotification(Task task, boolean accepted) {
        try {
            String type = accepted ? "WORK_ACCEPTED" : "WORK_REJECTED";
            String title = accepted ? "Work Accepted!" : "Work Needs Revision";
            String message = accepted ? 
                    "Your work has been accepted! Payment will be processed." :
                    "Your work needs revision. Please check the feedback.";

            NotificationRequest notification = NotificationRequest.builder()
                    .userId(task.getAssignedUserId())
                    .type(type)
                    .title(title)
                    .message(message)
                    .relatedEntityId(task.getId())
                    .relatedEntityType("TASK")
                    .build();
            
            notificationServiceClient.sendNotification(notification);
        } catch (Exception e) {
            log.error("Failed to send work acceptance notification: {}", e.getMessage());
        }
    }

    @Async
    private void sendOverdueNotification(Task task) {
        try {
            NotificationRequest notification = NotificationRequest.builder()
                    .userId(task.getAssignedUserId())
                    .type("TASK_OVERDUE")
                    .title("Task Overdue")
                    .message("Your assigned task is overdue. Please submit your work.")
                    .relatedEntityId(task.getId())
                    .relatedEntityType("TASK")
                    .build();
            
            notificationServiceClient.sendNotification(notification);
        } catch (Exception e) {
            log.error("Failed to send overdue notification: {}", e.getMessage());
        }
    }

    private void initiateEscrowPayment(Task task) {
        try {
            EscrowPaymentRequest paymentRequest = EscrowPaymentRequest.builder()
                    .taskId(task.getId())
                    .payerId(task.getOwnerId())
                    .payeeId(task.getAssignedUserId())
                    .amount(task.getFinalPrice())
                    .build();
            
            paymentServiceClient.createEscrowPayment(paymentRequest);
        } catch (Exception e) {
            log.error("Failed to initiate escrow payment: {}", e.getMessage());
        }
    }

    private void releasePayment(Task task) {
        try {
            paymentServiceClient.releaseEscrowPayment(task.getId());
        } catch (Exception e) {
            log.error("Failed to release payment: {}", e.getMessage());
        }
    }

    private void initiateRefund(Task task) {
        try {
            paymentServiceClient.refundEscrowPayment(task.getId());
        } catch (Exception e) {
            log.error("Failed to initiate refund: {}", e.getMessage());
        }
    }

    private void updateProfileStatistics(Task task) {
        try {
            profileServiceClient.incrementCompletedTasks(task.getAssignedUserId());
            profileServiceClient.addEarnings(task.getAssignedUserId(), task.getFinalPrice().intValue());
        } catch (Exception e) {
            log.error("Failed to update profile statistics: {}", e.getMessage());
        }
    }

    private String getNotificationTitle(String type) {
        return switch (type) {
            case "TASK_CREATED" -> "Task Published";
            case "TASK_CANCELLED" -> "Task Cancelled";
            default -> "Task Update";
        };
    }

    private String getNotificationMessage(String type, Task task) {
        return switch (type) {
            case "TASK_CREATED" -> "Your task '" + task.getTitle() + "' is now live and accepting bids.";
            case "TASK_CANCELLED" -> "Your task '" + task.getTitle() + "' has been cancelled.";
            default -> "Task update for: " + task.getTitle();
        };
    }

    // Feign clients
    @FeignClient(name = "profile-service")
    public interface ProfileServiceClient {
        @GetMapping("/profiles/user/{userId}")
        UserProfileResponse getUserProfile(@PathVariable Long userId);

        @PostMapping("/profiles/{userId}/increment-tasks")
        void incrementCompletedTasks(@PathVariable Long userId);

        @PostMapping("/profiles/{userId}/add-earnings")
        void addEarnings(@PathVariable Long userId, @RequestBody Integer amount);
    }

    @FeignClient(name = "notification-service")
    public interface NotificationServiceClient {
        @PostMapping("/notifications")
        void sendNotification(@RequestBody NotificationRequest request);
    }

    @FeignClient(name = "payment-service")
    public interface PaymentServiceClient {
        @PostMapping("/payments/escrow")
        void createEscrowPayment(@RequestBody EscrowPaymentRequest request);

        @PostMapping("/payments/escrow/{taskId}/release")
        void releaseEscrowPayment(@PathVariable Long taskId);

        @PostMapping("/payments/escrow/{taskId}/refund")
        void refundEscrowPayment(@PathVariable Long taskId);
    }

    // Additional DTOs
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class UserProfileResponse {
        private Long userId;
        private String firstName;
        private String lastName;
        private String email;
        private boolean profileCompleted;
        private boolean verified;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class TaskAssignmentRequest {
        private Long assignedUserId;
        private BigDecimal finalPrice;
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
    public static class EscrowPaymentRequest {
        private Long taskId;
        private Long payerId;
        private Long payeeId;
        private BigDecimal amount;
    }
}