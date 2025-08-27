package com.campusworks.task.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long ownerId; // creator user id

    @Column(nullable = false, length = 200)
    private String title;
    
    @Column(nullable = false, length = 2000)
    private String description;
    
    @Column(nullable = false)
    private BigDecimal budget; // maximum budget user is willing to pay
    
    @Column(length = 500)
    private String requirements; // specific requirements for the task
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status = TaskStatus.DRAFT;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskCategory category;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskPriority priority = TaskPriority.MEDIUM;
    
    @Column(nullable = false)
    private LocalDateTime deadline; // when task should be completed
    
    @Column(nullable = false)
    private LocalDateTime biddingDeadline; // when bidding ends
    
    private Long assignedUserId; // winner of bidding
    
    private BigDecimal finalPrice; // agreed price after bidding
    
    private String workSubmissionUrl; // link to submitted work
    
    private String workSubmissionNotes; // notes from assigned user
    
    private LocalDateTime workSubmittedAt;
    
    private LocalDateTime workAcceptedAt;
    
    private boolean workAccepted = false;
    
    private String rejectionReason; // if work is rejected
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    // Enums
    public enum TaskStatus {
        DRAFT,           // Task created but not published
        OPEN,            // Task published, accepting bids
        BIDDING_CLOSED,  // Bidding period ended, selecting winner
        ASSIGNED,        // Task assigned to winner
        IN_PROGRESS,     // Work in progress
        SUBMITTED,       // Work submitted by assignee
        COMPLETED,       // Work accepted and payment processed
        CANCELLED,       // Task cancelled
        REJECTED         // Work rejected
    }
    
    public enum TaskCategory {
        ASSIGNMENT,      // Academic assignments
        LAB_RECORD,      // Lab record writing
        NOTES,           // Note taking/making
        PROJECT,         // Small projects
        RESEARCH,        // Research work
        PRESENTATION,    // PPT creation
        OTHER           // Other tasks
    }
    
    public enum TaskPriority {
        LOW,
        MEDIUM,
        HIGH,
        URGENT
    }
}


