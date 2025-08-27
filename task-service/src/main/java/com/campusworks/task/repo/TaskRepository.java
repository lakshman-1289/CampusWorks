package com.campusworks.task.repo;

import com.campusworks.task.domain.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {
    
    // Owner-based queries
    List<Task> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);
    
    Page<Task> findByOwnerIdOrderByCreatedAtDesc(Long ownerId, Pageable pageable);
    
    List<Task> findByOwnerIdAndStatus(Long ownerId, Task.TaskStatus status);
    
    // Assignee-based queries
    List<Task> findByAssignedUserId(Long assignedUserId);
    
    Page<Task> findByAssignedUserIdOrderByCreatedAtDesc(Long assignedUserId, Pageable pageable);
    
    List<Task> findByAssignedUserIdAndStatus(Long assignedUserId, Task.TaskStatus status);
    
    // Status-based queries
    List<Task> findByStatus(Task.TaskStatus status);
    
    Page<Task> findByStatusOrderByCreatedAtDesc(Task.TaskStatus status, Pageable pageable);
    
    List<Task> findByStatusIn(List<Task.TaskStatus> statuses);
    
    // Public task discovery
    @Query("SELECT t FROM Task t WHERE t.status = 'OPEN' AND t.biddingEndTime > :currentTime ORDER BY t.createdAt DESC")
    Page<Task> findOpenTasksForBidding(@Param("currentTime") LocalDateTime currentTime, Pageable pageable);
    
    @Query("SELECT t FROM Task t WHERE t.status = 'OPEN' AND t.biddingEndTime > :currentTime AND t.category = :category ORDER BY t.createdAt DESC")
    Page<Task> findOpenTasksByCategory(@Param("currentTime") LocalDateTime currentTime, @Param("category") String category, Pageable pageable);
    
    @Query("SELECT t FROM Task t WHERE t.status = 'OPEN' AND t.biddingEndTime > :currentTime AND t.subject = :subject ORDER BY t.createdAt DESC")
    Page<Task> findOpenTasksBySubject(@Param("currentTime") LocalDateTime currentTime, @Param("subject") String subject, Pageable pageable);
    
    // Search functionality
    @Query("SELECT t FROM Task t WHERE t.status = 'OPEN' AND t.biddingEndTime > :currentTime AND (t.title LIKE %:keyword% OR t.description LIKE %:keyword% OR t.subject LIKE %:keyword%) ORDER BY t.createdAt DESC")
    Page<Task> searchOpenTasks(@Param("currentTime") LocalDateTime currentTime, @Param("keyword") String keyword, Pageable pageable);
    
    // Bidding time-based queries
    @Query("SELECT t FROM Task t WHERE t.status = 'OPEN' AND t.biddingEndTime <= :currentTime")
    List<Task> findTasksWithExpiredBidding(@Param("currentTime") LocalDateTime currentTime);
    
    @Query("SELECT t FROM Task t WHERE t.status = 'OPEN' AND t.biddingEndTime BETWEEN :startTime AND :endTime")
    List<Task> findTasksExpiringBetween(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
    
    // Completion deadline queries
    @Query("SELECT t FROM Task t WHERE t.status = 'ASSIGNED' AND t.expectedCompletionDate <= :currentTime")
    List<Task> findOverdueTasks(@Param("currentTime") LocalDateTime currentTime);
    
    @Query("SELECT t FROM Task t WHERE t.status = 'ASSIGNED' AND t.expectedCompletionDate BETWEEN :startTime AND :endTime")
    List<Task> findTasksDueBetween(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
    
    // Statistical queries
    Long countByOwnerId(Long ownerId);
    
    Long countByOwnerIdAndStatus(Long ownerId, Task.TaskStatus status);
    
    Long countByAssignedUserId(Long assignedUserId);
    
    Long countByAssignedUserIdAndStatus(Long assignedUserId, Task.TaskStatus status);
    
    @Query("SELECT COUNT(t) FROM Task t WHERE t.status = 'OPEN' AND t.biddingEndTime > :currentTime")
    Long countOpenTasks(@Param("currentTime") LocalDateTime currentTime);
    
    // Recent activity
    @Query("SELECT t FROM Task t WHERE t.updatedAt >= :since ORDER BY t.updatedAt DESC")
    List<Task> findRecentlyUpdatedTasks(@Param("since") LocalDateTime since, Pageable pageable);
    
    // Category and subject statistics
    @Query("SELECT t.category, COUNT(t) FROM Task t WHERE t.status = 'OPEN' AND t.biddingEndTime > :currentTime GROUP BY t.category")
    List<Object[]> getTaskCountByCategory(@Param("currentTime") LocalDateTime currentTime);
    
    @Query("SELECT t.subject, COUNT(t) FROM Task t WHERE t.status = 'OPEN' AND t.biddingEndTime > :currentTime GROUP BY t.subject")
    List<Object[]> getTaskCountBySubject(@Param("currentTime") LocalDateTime currentTime);
    
    // Urgent tasks
    @Query("SELECT t FROM Task t WHERE t.urgent = true AND t.status IN ('OPEN', 'ASSIGNED') ORDER BY t.createdAt DESC")
    List<Task> findUrgentTasks();
    
    // Budget range queries
    @Query("SELECT t FROM Task t WHERE t.status = 'OPEN' AND t.minBudget >= :minAmount AND t.maxBudget <= :maxAmount AND t.biddingEndTime > :currentTime")
    Page<Task> findTasksInBudgetRange(@Param("minAmount") java.math.BigDecimal minAmount, @Param("maxAmount") java.math.BigDecimal maxAmount, @Param("currentTime") LocalDateTime currentTime, Pageable pageable);
    
    // Task validation queries
    boolean existsByIdAndOwnerId(Long id, Long ownerId);
    
    boolean existsByIdAndAssignedUserId(Long id, Long assignedUserId);
    
    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM Task t WHERE t.id = :id AND t.status = 'OPEN' AND t.biddingEndTime > :currentTime")
    boolean isTaskOpenForBidding(@Param("id") Long id, @Param("currentTime") LocalDateTime currentTime);
}


