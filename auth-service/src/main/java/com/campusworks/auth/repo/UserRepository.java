package com.campusworks.auth.repo;

import com.campusworks.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // Basic user lookup
    Optional<User> findByEmail(String email);
    
    boolean existsByEmail(String email);
    
    // Verification tokens
    Optional<User> findByEmailVerificationToken(String token);
    
    Optional<User> findByPhoneVerificationToken(String token);
    
    Optional<User> findByPasswordResetToken(String token);
    
    // Status-based queries
    List<User> findByEnabled(boolean enabled);
    
    List<User> findByEmailVerified(boolean emailVerified);
    
    List<User> findByPhoneVerified(boolean phoneVerified);
    
    List<User> findByProfileCompleted(boolean profileCompleted);
    
    // Complex queries
    @Query("SELECT u FROM User u WHERE u.emailVerified = true AND u.profileCompleted = true AND u.enabled = true")
    List<User> findVerifiedUsers();
    
    @Query("SELECT u FROM User u WHERE u.emailVerified = false AND u.createdAt < :cutoffDate")
    List<User> findUnverifiedUsersOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    @Query("SELECT u FROM User u WHERE u.lastLoginAt < :cutoffDate")
    List<User> findInactiveUsersSince(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    // Statistics
    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :startDate")
    Long countUsersRegisteredSince(@Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.emailVerified = true")
    Long countVerifiedUsers();
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.lastLoginAt >= :startDate")
    Long countActiveUsersSince(@Param("startDate") LocalDateTime startDate);
    
    // College-based queries
    List<User> findByCollegeIgnoreCase(String college);
    
    @Query("SELECT u.college, COUNT(u) FROM User u WHERE u.college IS NOT NULL GROUP BY u.college ORDER BY COUNT(u) DESC")
    List<Object[]> getUserCountByCollege();
    
    // Course-based queries
    List<User> findByCourseIgnoreCase(String course);
    
    @Query("SELECT u.course, COUNT(u) FROM User u WHERE u.course IS NOT NULL GROUP BY u.course ORDER BY COUNT(u) DESC")
    List<Object[]> getUserCountByCourse();
    
    // Role-based queries
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r = :role")
    List<User> findByRole(@Param("role") String role);
    
    // Search functionality
    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.college) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<User> searchUsers(@Param("keyword") String keyword);
}