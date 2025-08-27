package com.campusworks.profile.repo;

import com.campusworks.profile.domain.UserProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    
    // Basic user profile queries
    Optional<UserProfile> findByUserId(Long userId);
    
    boolean existsByUserId(Long userId);
    
    // Verification status queries
    List<UserProfile> findByVerificationStatus(UserProfile.VerificationStatus status);
    
    List<UserProfile> findByVerificationStatusOrderByVerificationSubmittedAtAsc(UserProfile.VerificationStatus status);
    
    Page<UserProfile> findByVerificationStatusAndAvailableForTasksTrue(UserProfile.VerificationStatus status, Pageable pageable);
    
    long countByVerificationStatus(UserProfile.VerificationStatus status);
    
    // Skills-based queries
    @Query("SELECT p FROM UserProfile p WHERE p.skills LIKE %:skill% AND p.verificationStatus = :status")
    List<UserProfile> findBySkillsContainingAndVerificationStatus(@Param("skill") String skill, @Param("status") UserProfile.VerificationStatus status);
    
    @Query("SELECT p FROM UserProfile p WHERE EXISTS (SELECT s FROM p.skills s WHERE s IN :skills) AND p.verificationStatus = :status")
    Page<UserProfile> findBySkillsInAndVerificationStatus(@Param("skills") List<String> skills, @Param("status") UserProfile.VerificationStatus status, Pageable pageable);
    
    // Subjects-based queries
    @Query("SELECT p FROM UserProfile p WHERE EXISTS (SELECT s FROM p.subjects s WHERE s IN :subjects) AND p.verificationStatus = :status")
    Page<UserProfile> findBySubjectsInAndVerificationStatus(@Param("subjects") List<String> subjects, @Param("status") UserProfile.VerificationStatus status, Pageable pageable);
    
    // Search functionality
    @Query("SELECT p FROM UserProfile p WHERE " +
           "(LOWER(p.bio) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "EXISTS (SELECT s FROM p.skills s WHERE LOWER(s) LIKE LOWER(CONCAT('%', :keyword, '%'))) OR " +
           "EXISTS (SELECT sub FROM p.subjects sub WHERE LOWER(sub) LIKE LOWER(CONCAT('%', :keyword, '%')))) AND " +
           "p.verificationStatus = 'VERIFIED' AND p.availableForTasks = true")
    Page<UserProfile> searchProfiles(@Param("keyword") String keyword, Pageable pageable);
    
    // Rating-based queries
    List<UserProfile> findByRatingGreaterThanEqualOrderByRatingDesc(BigDecimal minRating);
    
    @Query("SELECT p FROM UserProfile p WHERE p.rating >= :minRating AND p.verificationStatus = 'VERIFIED' ORDER BY p.rating DESC")
    List<UserProfile> findTopRatedProfiles(@Param("minRating") BigDecimal minRating, Pageable pageable);
    
    // Performance-based queries
    List<UserProfile> findByCompletedTasksGreaterThanEqualOrderByCompletedTasksDesc(Integer minTasks);
    
    List<UserProfile> findByTotalEarningsGreaterThanEqualOrderByTotalEarningsDesc(Integer minEarnings);
    
    @Query("SELECT p FROM UserProfile p WHERE p.completedTasks >= :minTasks AND p.rating >= :minRating AND p.verificationStatus = 'VERIFIED'")
    List<UserProfile> findExperiencedProfiles(@Param("minTasks") Integer minTasks, @Param("minRating") BigDecimal minRating);
    
    // Availability queries
    List<UserProfile> findByAvailableForTasksTrue();
    
    List<UserProfile> findByAvailableForTasksTrueAndVerificationStatus(UserProfile.VerificationStatus status);
    
    // Time-based queries
    List<UserProfile> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    
    List<UserProfile> findByUpdatedAtAfter(LocalDateTime after);
    
    List<UserProfile> findByVerificationSubmittedAtBetween(LocalDateTime start, LocalDateTime end);
    
    List<UserProfile> findByVerificationCompletedAtBetween(LocalDateTime start, LocalDateTime end);
    
    // Portfolio queries
    @Query("SELECT p FROM UserProfile p WHERE p.portfolioUrl IS NOT NULL AND p.portfolioUrl != ''")
    List<UserProfile> findProfilesWithPortfolio();
    
    // Statistics queries
    @Query("SELECT AVG(p.rating) FROM UserProfile p WHERE p.verificationStatus = 'VERIFIED' AND p.completedTasks > 0")
    Optional<Double> findAverageRatingOfActiveProfiles();
    
    @Query("SELECT SUM(p.completedTasks) FROM UserProfile p WHERE p.verificationStatus = 'VERIFIED'")
    Optional<Long> findTotalCompletedTasksByVerifiedUsers();
    
    @Query("SELECT SUM(p.totalEarnings) FROM UserProfile p WHERE p.verificationStatus = 'VERIFIED'")
    Optional<Long> findTotalEarningsByVerifiedUsers();
    
    // Skill analysis
    @Query("SELECT skill, COUNT(p) FROM UserProfile p JOIN p.skills skill WHERE p.verificationStatus = 'VERIFIED' GROUP BY skill ORDER BY COUNT(p) DESC")
    List<Object[]> findMostPopularSkills();
    
    @Query("SELECT subject, COUNT(p) FROM UserProfile p JOIN p.subjects subject WHERE p.verificationStatus = 'VERIFIED' GROUP BY subject ORDER BY COUNT(p) DESC")
    List<Object[]> findMostPopularSubjects();
    
    // Admin queries
    @Query("SELECT p FROM UserProfile p WHERE p.verificationStatus = 'PENDING' AND p.verificationSubmittedAt < :cutoffDate")
    List<UserProfile> findPendingVerificationsOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    @Query("SELECT p FROM UserProfile p WHERE p.verificationStatus = 'VERIFIED' AND p.completedTasks = 0 AND p.createdAt < :cutoffDate")
    List<UserProfile> findInactiveVerifiedProfiles(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    // Complex queries for recommendations
    @Query("SELECT p FROM UserProfile p WHERE " +
           "p.verificationStatus = 'VERIFIED' AND p.availableForTasks = true AND " +
           "EXISTS (SELECT s FROM p.skills s WHERE s IN :requiredSkills) AND " +
           "p.rating >= :minRating AND p.completedTasks >= :minTasks " +
           "ORDER BY p.rating DESC, p.completedTasks DESC")
    List<UserProfile> findRecommendedProfiles(
            @Param("requiredSkills") List<String> requiredSkills,
            @Param("minRating") BigDecimal minRating,
            @Param("minTasks") Integer minTasks,
            Pageable pageable
    );
    
    // Recent activity
    @Query("SELECT p FROM UserProfile p WHERE p.updatedAt >= :since ORDER BY p.updatedAt DESC")
    List<UserProfile> findRecentlyUpdatedProfiles(@Param("since") LocalDateTime since);
    
    // Verification workflow
    @Query("SELECT COUNT(p) FROM UserProfile p WHERE p.verificationStatus = 'PENDING' AND DATE(p.verificationSubmittedAt) = CURRENT_DATE")
    long countTodaysPendingVerifications();
    
    @Query("SELECT COUNT(p) FROM UserProfile p WHERE p.verificationStatus = 'VERIFIED' AND DATE(p.verificationCompletedAt) = CURRENT_DATE")
    long countTodaysApprovedVerifications();
}