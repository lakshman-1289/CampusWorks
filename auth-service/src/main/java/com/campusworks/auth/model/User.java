package com.campusworks.auth.model;

import java.time.LocalDateTime;
import java.util.Set;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String passwordHash;
    
    @Column(nullable = false, length = 100)
    private String firstName;
    
    @Column(nullable = false, length = 100)
    private String lastName;
    
    @Column(length = 15)
    private String phoneNumber;
    
    @Column(length = 200)
    private String college;
    
    @Column(length = 100)
    private String course;
    
    private Integer yearOfStudy;
    
    private String profileImageUrl;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    private Set<String> roles;

    @Column(nullable = false)
    private boolean enabled = true;
    
    private boolean emailVerified = false;
    
    private boolean phoneVerified = false;
    
    private boolean profileCompleted = false;
    
    private String emailVerificationToken;
    
    private String phoneVerificationToken;
    
    private String passwordResetToken;
    
    private LocalDateTime passwordResetTokenExpiry;
    
    private LocalDateTime lastLoginAt;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    // Utility methods
    public String getFullName() {
        return firstName + " " + lastName;
    }
    
    public boolean isVerified() {
        return emailVerified && profileCompleted;
    }
}