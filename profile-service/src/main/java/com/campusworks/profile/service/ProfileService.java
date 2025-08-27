package com.campusworks.profile.service;

import com.campusworks.profile.domain.UserProfile;
import com.campusworks.profile.repo.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProfileService {

    private final UserProfileRepository profileRepository;
    private final AuthServiceClient authServiceClient;
    private final NotificationServiceClient notificationServiceClient;

    public ProfileResponse createProfile(CreateProfileRequest request, Long userId) {
        log.info("Creating profile for user {}", userId);

        // Check if profile already exists
        if (profileRepository.existsByUserId(userId)) {
            throw new RuntimeException("Profile already exists for this user");
        }

        // Get user details from auth service
        UserDetailsResponse userDetails = authServiceClient.getUserDetails(userId);

        // Create profile
        UserProfile profile = UserProfile.builder()
                .userId(userId)
                .bio(request.getBio())
                .skills(request.getSkills())
                .subjects(request.getSubjects())
                .portfolioUrl(request.getPortfolioUrl())
                .verificationStatus(UserProfile.VerificationStatus.UNVERIFIED)
                .availableForTasks(true)
                .rating(BigDecimal.ZERO)
                .completedTasks(0)
                .totalEarnings(0)
                .tasksPosted(0)
                .successfulHires(0)
                .build();

        UserProfile savedProfile = profileRepository.save(profile);

        // Send welcome notification
        sendWelcomeNotification(userId, userDetails.getFirstName());

        log.info("Profile created successfully for user {}", userId);
        return mapToResponse(savedProfile, userDetails);
    }

    public ProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        log.info("Updating profile for user {}", userId);

        UserProfile profile = getProfileByUserId(userId);
        UserDetailsResponse userDetails = authServiceClient.getUserDetails(userId);

        // Update fields
        if (request.getBio() != null) {
            profile.setBio(request.getBio());
        }
        if (request.getSkills() != null) {
            profile.setSkills(request.getSkills());
        }
        if (request.getSubjects() != null) {
            profile.setSubjects(request.getSubjects());
        }
        if (request.getPortfolioUrl() != null) {
            profile.setPortfolioUrl(request.getPortfolioUrl());
        }
        if (request.getAvailableForTasks() != null) {
            profile.setAvailableForTasks(request.getAvailableForTasks());
        }

        UserProfile savedProfile = profileRepository.save(profile);
        return mapToResponse(savedProfile, userDetails);
    }

    public ProfileResponse getProfile(Long userId) {
        log.info("Getting profile for user {}", userId);

        UserProfile profile = getProfileByUserId(userId);
        UserDetailsResponse userDetails = authServiceClient.getUserDetails(userId);

        return mapToResponse(profile, userDetails);
    }

    public ProfileResponse getPublicProfile(Long userId) {
        log.info("Getting public profile for user {}", userId);

        UserProfile profile = getProfileByUserId(userId);
        UserDetailsResponse userDetails = authServiceClient.getUserDetails(userId);

        // Return limited information for public view
        return mapToPublicResponse(profile, userDetails);
    }

    public void submitVerification(Long userId, VerificationRequest request) {
        log.info("Submitting verification for user {}", userId);

        UserProfile profile = getProfileByUserId(userId);

        if (profile.getVerificationStatus() == UserProfile.VerificationStatus.VERIFIED) {
            throw new RuntimeException("Profile is already verified");
        }

        if (profile.getVerificationStatus() == UserProfile.VerificationStatus.PENDING) {
            throw new RuntimeException("Verification is already pending");
        }

        profile.setIdVerificationUrl(request.getIdDocumentUrl());
        profile.setVerificationStatus(UserProfile.VerificationStatus.PENDING);
        profile.setVerificationSubmittedAt(LocalDateTime.now());
        profile.setVerificationNotes(request.getNotes());

        profileRepository.save(profile);

        // Notify admins about new verification request
        sendVerificationNotification(userId);

        log.info("Verification submitted successfully for user {}", userId);
    }

    public void approveVerification(Long userId, String adminNotes) {
        log.info("Approving verification for user {}", userId);

        UserProfile profile = getProfileByUserId(userId);

        if (profile.getVerificationStatus() != UserProfile.VerificationStatus.PENDING) {
            throw new RuntimeException("No pending verification found");
        }

        profile.setVerificationStatus(UserProfile.VerificationStatus.VERIFIED);
        profile.setVerificationCompletedAt(LocalDateTime.now());
        profile.setVerificationNotes(adminNotes);

        profileRepository.save(profile);

        // Send verification success notification
        sendVerificationSuccessNotification(userId);

        log.info("Verification approved for user {}", userId);
    }

    public void rejectVerification(Long userId, String reason) {
        log.info("Rejecting verification for user {}", userId);

        UserProfile profile = getProfileByUserId(userId);

        if (profile.getVerificationStatus() != UserProfile.VerificationStatus.PENDING) {
            throw new RuntimeException("No pending verification found");
        }

        profile.setVerificationStatus(UserProfile.VerificationStatus.REJECTED);
        profile.setVerificationCompletedAt(LocalDateTime.now());
        profile.setVerificationNotes(reason);

        profileRepository.save(profile);

        // Send verification rejection notification
        sendVerificationRejectionNotification(userId, reason);

        log.info("Verification rejected for user {}", userId);
    }

    public void incrementCompletedTasks(Long userId) {
        log.info("Incrementing completed tasks for user {}", userId);

        UserProfile profile = getProfileByUserId(userId);
        profile.incrementCompletedTasks();
        profileRepository.save(profile);
    }

    public void addEarnings(Long userId, Integer amount) {
        log.info("Adding earnings {} for user {}", amount, userId);

        UserProfile profile = getProfileByUserId(userId);
        profile.addEarnings(amount);
        profileRepository.save(profile);
    }

    public void incrementTasksPosted(Long userId) {
        log.info("Incrementing tasks posted for user {}", userId);

        UserProfile profile = getProfileByUserId(userId);
        profile.setTasksPosted(profile.getTasksPosted() + 1);
        profileRepository.save(profile);
    }

    public void incrementSuccessfulHires(Long userId) {
        log.info("Incrementing successful hires for user {}", userId);

        UserProfile profile = getProfileByUserId(userId);
        profile.setSuccessfulHires(profile.getSuccessfulHires() + 1);
        profileRepository.save(profile);
    }

    public void updateRating(Long userId, BigDecimal newRating) {
        log.info("Updating rating to {} for user {}", newRating, userId);

        UserProfile profile = getProfileByUserId(userId);
        profile.setRating(newRating);
        profileRepository.save(profile);
    }

    public Page<ProfileResponse> searchProfiles(String keyword, List<String> skills, 
                                                List<String> subjects, int page, int size) {
        log.info("Searching profiles with keyword: {}, skills: {}, subjects: {}", keyword, skills, subjects);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "rating"));
        Page<UserProfile> profiles;

        if (keyword != null && !keyword.trim().isEmpty()) {
            profiles = profileRepository.searchProfiles(keyword, pageable);
        } else if (skills != null && !skills.isEmpty()) {
            profiles = profileRepository.findBySkillsInAndVerificationStatus(
                    skills, UserProfile.VerificationStatus.VERIFIED, pageable);
        } else if (subjects != null && !subjects.isEmpty()) {
            profiles = profileRepository.findBySubjectsInAndVerificationStatus(
                    subjects, UserProfile.VerificationStatus.VERIFIED, pageable);
        } else {
            profiles = profileRepository.findByVerificationStatusAndAvailableForTasksTrue(
                    UserProfile.VerificationStatus.VERIFIED, pageable);
        }

        return profiles.map(profile -> {
            try {
                UserDetailsResponse userDetails = authServiceClient.getUserDetails(profile.getUserId());
                return mapToPublicResponse(profile, userDetails);
            } catch (Exception e) {
                log.error("Failed to get user details for profile {}: {}", profile.getUserId(), e.getMessage());
                return mapToPublicResponse(profile, null);
            }
        });
    }

    public List<ProfileResponse> getTopRatedProfiles(int limit) {
        log.info("Getting top {} rated profiles", limit);

        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "rating"));
        Page<UserProfile> profiles = profileRepository.findByVerificationStatusAndAvailableForTasksTrue(
                UserProfile.VerificationStatus.VERIFIED, pageable);

        return profiles.getContent().stream()
                .map(profile -> {
                    try {
                        UserDetailsResponse userDetails = authServiceClient.getUserDetails(profile.getUserId());
                        return mapToPublicResponse(profile, userDetails);
                    } catch (Exception e) {
                        return mapToPublicResponse(profile, null);
                    }
                })
                .toList();
    }

    public ProfileStatsResponse getProfileStats(Long userId) {
        UserProfile profile = getProfileByUserId(userId);

        return ProfileStatsResponse.builder()
                .completedTasks(profile.getCompletedTasks())
                .totalEarnings(profile.getTotalEarnings())
                .tasksPosted(profile.getTasksPosted())
                .successfulHires(profile.getSuccessfulHires())
                .rating(profile.getRating())
                .verificationStatus(profile.getVerificationStatus().toString())
                .profileCompletionPercentage(calculateProfileCompletionPercentage(profile))
                .build();
    }

    public List<ProfileResponse> getPendingVerifications() {
        log.info("Getting pending verifications");

        List<UserProfile> pendingProfiles = profileRepository.findByVerificationStatusOrderByVerificationSubmittedAtAsc(
                UserProfile.VerificationStatus.PENDING);

        return pendingProfiles.stream()
                .map(profile -> {
                    try {
                        UserDetailsResponse userDetails = authServiceClient.getUserDetails(profile.getUserId());
                        return mapToResponse(profile, userDetails);
                    } catch (Exception e) {
                        return mapToResponse(profile, null);
                    }
                })
                .toList();
    }

    private UserProfile getProfileByUserId(Long userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Profile not found for user: " + userId));
    }

    private ProfileResponse mapToResponse(UserProfile profile, UserDetailsResponse userDetails) {
        return ProfileResponse.builder()
                .userId(profile.getUserId())
                .firstName(userDetails != null ? userDetails.getFirstName() : "Unknown")
                .lastName(userDetails != null ? userDetails.getLastName() : "User")
                .email(userDetails != null ? userDetails.getEmail() : "unknown@example.com")
                .phoneNumber(userDetails != null ? userDetails.getPhoneNumber() : null)
                .college(userDetails != null ? userDetails.getCollege() : null)
                .course(userDetails != null ? userDetails.getCourse() : null)
                .yearOfStudy(userDetails != null ? userDetails.getYearOfStudy() : null)
                .bio(profile.getBio())
                .skills(profile.getSkills())
                .subjects(profile.getSubjects())
                .rating(profile.getRating())
                .completedTasks(profile.getCompletedTasks())
                .totalEarnings(profile.getTotalEarnings())
                .tasksPosted(profile.getTasksPosted())
                .successfulHires(profile.getSuccessfulHires())
                .verificationStatus(profile.getVerificationStatus().toString())
                .availableForTasks(profile.isAvailableForTasks())
                .portfolioUrl(profile.getPortfolioUrl())
                .profileCompleted(calculateProfileCompletion(profile, userDetails))
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }

    private ProfileResponse mapToPublicResponse(UserProfile profile, UserDetailsResponse userDetails) {
        return ProfileResponse.builder()
                .userId(profile.getUserId())
                .firstName(userDetails != null ? userDetails.getFirstName() : "Unknown")
                .lastName(userDetails != null ? userDetails.getLastName() : "User")
                .college(userDetails != null ? userDetails.getCollege() : null)
                .course(userDetails != null ? userDetails.getCourse() : null)
                .bio(profile.getBio())
                .skills(profile.getSkills())
                .subjects(profile.getSubjects())
                .rating(profile.getRating())
                .completedTasks(profile.getCompletedTasks())
                .verificationStatus(profile.getVerificationStatus().toString())
                .availableForTasks(profile.isAvailableForTasks())
                .portfolioUrl(profile.getPortfolioUrl())
                .profileCompleted(calculateProfileCompletion(profile, userDetails))
                .build();
    }

    private boolean calculateProfileCompletion(UserProfile profile, UserDetailsResponse userDetails) {
        if (userDetails == null) return false;

        return userDetails.getFirstName() != null &&
               userDetails.getLastName() != null &&
               userDetails.getEmail() != null &&
               profile.getBio() != null &&
               profile.getSkills() != null && !profile.getSkills().isEmpty() &&
               profile.getSubjects() != null && !profile.getSubjects().isEmpty();
    }

    private int calculateProfileCompletionPercentage(UserProfile profile) {
        int totalFields = 6; // bio, skills, subjects, portfolio, college, course
        int completedFields = 0;

        if (profile.getBio() != null && !profile.getBio().trim().isEmpty()) completedFields++;
        if (profile.getSkills() != null && !profile.getSkills().isEmpty()) completedFields++;
        if (profile.getSubjects() != null && !profile.getSubjects().isEmpty()) completedFields++;
        if (profile.getPortfolioUrl() != null && !profile.getPortfolioUrl().trim().isEmpty()) completedFields++;

        return (completedFields * 100) / totalFields;
    }

    private void sendWelcomeNotification(Long userId, String firstName) {
        try {
            NotificationRequest notification = NotificationRequest.builder()
                    .userId(userId)
                    .type("PROFILE_CREATED")
                    .title("Welcome to CampusWorks!")
                    .message("Welcome " + firstName + "! Your profile has been created. Complete your profile to start earning.")
                    .relatedEntityType("PROFILE")
                    .build();

            notificationServiceClient.sendNotification(notification);
        } catch (Exception e) {
            log.error("Failed to send welcome notification: {}", e.getMessage());
        }
    }

    private void sendVerificationNotification(Long userId) {
        try {
            NotificationRequest notification = NotificationRequest.builder()
                    .userId(userId)
                    .type("VERIFICATION_SUBMITTED")
                    .title("Verification Submitted")
                    .message("Your verification documents have been submitted and are under review.")
                    .relatedEntityType("VERIFICATION")
                    .build();

            notificationServiceClient.sendNotification(notification);
        } catch (Exception e) {
            log.error("Failed to send verification notification: {}", e.getMessage());
        }
    }

    private void sendVerificationSuccessNotification(Long userId) {
        try {
            NotificationRequest notification = NotificationRequest.builder()
                    .userId(userId)
                    .type("ACCOUNT_VERIFIED")
                    .title("Account Verified!")
                    .message("Congratulations! Your account has been verified. You can now access all features.")
                    .relatedEntityType("VERIFICATION")
                    .build();

            notificationServiceClient.sendNotification(notification);
        } catch (Exception e) {
            log.error("Failed to send verification success notification: {}", e.getMessage());
        }
    }

    private void sendVerificationRejectionNotification(Long userId, String reason) {
        try {
            NotificationRequest notification = NotificationRequest.builder()
                    .userId(userId)
                    .type("VERIFICATION_REJECTED")
                    .title("Verification Rejected")
                    .message("Your verification was rejected. Reason: " + reason)
                    .relatedEntityType("VERIFICATION")
                    .build();

            notificationServiceClient.sendNotification(notification);
        } catch (Exception e) {
            log.error("Failed to send verification rejection notification: {}", e.getMessage());
        }
    }

    // Feign clients
    @FeignClient(name = "auth-service")
    public interface AuthServiceClient {
        @GetMapping("/auth/users/{userId}")
        UserDetailsResponse getUserDetails(@PathVariable Long userId);
    }

    @FeignClient(name = "notification-service")
    public interface NotificationServiceClient {
        @PostMapping("/notifications")
        void sendNotification(@RequestBody NotificationRequest request);
    }

    // DTOs
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CreateProfileRequest {
        private String bio;
        private List<String> skills;
        private List<String> subjects;
        private String portfolioUrl;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class UpdateProfileRequest {
        private String bio;
        private List<String> skills;
        private List<String> subjects;
        private String portfolioUrl;
        private Boolean availableForTasks;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class VerificationRequest {
        private String idDocumentUrl;
        private String notes;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ProfileResponse {
        private Long userId;
        private String firstName;
        private String lastName;
        private String email;
        private String phoneNumber;
        private String college;
        private String course;
        private Integer yearOfStudy;
        private String bio;
        private List<String> skills;
        private List<String> subjects;
        private BigDecimal rating;
        private Integer completedTasks;
        private Integer totalEarnings;
        private Integer tasksPosted;
        private Integer successfulHires;
        private String verificationStatus;
        private boolean availableForTasks;
        private String portfolioUrl;
        private boolean profileCompleted;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ProfileStatsResponse {
        private Integer completedTasks;
        private Integer totalEarnings;
        private Integer tasksPosted;
        private Integer successfulHires;
        private BigDecimal rating;
        private String verificationStatus;
        private int profileCompletionPercentage;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class UserDetailsResponse {
        private Long id;
        private String firstName;
        private String lastName;
        private String email;
        private String phoneNumber;
        private String college;
        private String course;
        private Integer yearOfStudy;
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
        private String relatedEntityType;
    }
}