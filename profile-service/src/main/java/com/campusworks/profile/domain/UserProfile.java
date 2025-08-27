package com.campusworks.profile.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "user_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private Long userId; // Reference to auth service user

    @Column(length = 1000)
    private String bio;

    @ElementCollection
    @CollectionTable(name = "user_skills", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "skill")
    private List<String> skills;

    @ElementCollection
    @CollectionTable(name = "user_subjects", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "subject")
    private List<String> subjects; // Academic subjects expertise

    @Column(precision = 3, scale = 2)
    private BigDecimal rating = BigDecimal.ZERO; // Average rating (0-5)

    @Column(nullable = false)
    private Integer completedTasks = 0;

    @Column(nullable = false)
    private Integer totalEarnings = 0;

    @Column(nullable = false)
    private Integer tasksPosted = 0;

    @Column(nullable = false)
    private Integer successfulHires = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationStatus verificationStatus = VerificationStatus.UNVERIFIED;

    private String idVerificationUrl; // URL to uploaded ID document

    private LocalDateTime verificationSubmittedAt;

    private LocalDateTime verificationCompletedAt;

    private String verificationNotes;

    @Column(nullable = false)
    private boolean availableForTasks = true;

    @Column(length = 500)
    private String portfolioUrl;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public enum VerificationStatus {
        UNVERIFIED,
        PENDING,
        VERIFIED,
        REJECTED
    }

    // Utility methods
    public boolean isEligibleForTasks() {
        return availableForTasks && verificationStatus == VerificationStatus.VERIFIED;
    }

    public void incrementCompletedTasks() {
        this.completedTasks++;
    }

    public void addEarnings(int amount) {
        this.totalEarnings += amount;
    }
}