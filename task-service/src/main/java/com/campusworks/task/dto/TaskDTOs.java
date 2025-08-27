package com.campusworks.task.dto;

import com.campusworks.task.domain.Task;
import lombok.*;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTaskRequest {
    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    @NotBlank(message = "Description is required")
    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @NotNull(message = "Budget is required")
    @DecimalMin(value = "1.0", message = "Budget must be at least 1.0")
    @DecimalMax(value = "10000.0", message = "Budget must not exceed 10000.0")
    private BigDecimal budget;

    @Size(max = 500, message = "Requirements must not exceed 500 characters")
    private String requirements;

    @NotNull(message = "Category is required")
    private Task.TaskCategory category;

    private Task.TaskPriority priority = Task.TaskPriority.MEDIUM;

    @NotNull(message = "Deadline is required")
    @Future(message = "Deadline must be in the future")
    private LocalDateTime deadline;

    @NotNull(message = "Bidding deadline is required")
    @Future(message = "Bidding deadline must be in the future")
    private LocalDateTime biddingDeadline;
}

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskResponse {
    private Long id;
    private Long ownerId;
    private String ownerName;
    private String title;
    private String description;
    private BigDecimal budget;
    private String requirements;
    private Task.TaskStatus status;
    private Task.TaskCategory category;
    private Task.TaskPriority priority;
    private LocalDateTime deadline;
    private LocalDateTime biddingDeadline;
    private Long assignedUserId;
    private String assignedUserName;
    private BigDecimal finalPrice;
    private int bidCount;
    private BigDecimal lowestBid;
    private boolean canBid;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateTaskRequest {
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    private String requirements;

    private Task.TaskPriority priority;

    @Future(message = "Deadline must be in the future")
    private LocalDateTime deadline;
}

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkSubmissionRequest {
    @NotBlank(message = "Work submission URL is required")
    private String workSubmissionUrl;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String workSubmissionNotes;
}

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkAcceptanceRequest {
    @NotNull(message = "Acceptance decision is required")
    private boolean accepted;

    private String rejectionReason;
}