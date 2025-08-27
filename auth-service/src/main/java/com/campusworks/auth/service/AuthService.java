package com.campusworks.auth.service;

import com.campusworks.auth.model.User;
import com.campusworks.auth.dto.mapper.UserMapper;
import com.campusworks.auth.dto.request.ChangePasswordRequest;
import com.campusworks.auth.dto.request.LoginRequest;
import com.campusworks.auth.dto.request.RegisterRequest;
import com.campusworks.auth.dto.request.ResetPasswordRequest;
import com.campusworks.auth.dto.response.AuthResponse;
import com.campusworks.auth.dto.response.UserResponse;
import com.campusworks.auth.repo.UserRepository;
import com.campusworks.auth.security.JwtService;
import com.campusworks.auth.exception.BusinessException;
import com.campusworks.auth.exception.ResourceNotFoundException;
import com.campusworks.auth.exception.AuthenticationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final UserMapper userMapper;
    private final JwtService jwtService;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Value("${app.frontend.verify-email-path}")
    private String verifyEmailPath;

    @Value("${app.frontend.reset-password-path}")
    private String resetPasswordPath;

    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user with email: {}", request.getEmail());
        
        try {
            // Check if user already exists
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new BusinessException("Email already registered");
            }

            // Validate password strength
            validatePassword(request.getPassword());

            // Create user
            User user = User.builder()
                    .email(request.getEmail().toLowerCase())
                    .passwordHash(passwordEncoder.encode(request.getPassword()))
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .phoneNumber(request.getPhoneNumber())
                    .college(request.getCollege())
                    .course(request.getCourse())
                    .yearOfStudy(request.getYearOfStudy())
                    .roles(Set.of("USER"))
                    .enabled(true)
                    .emailVerified(false)
                    .emailVerificationToken(UUID.randomUUID().toString())
                    .build();

            User savedUser = userRepository.save(user);

            // Send verification email
            sendVerificationEmail(savedUser);

            // Generate JWT token using enhanced JwtService
            String token = jwtService.generateUserToken(
                    savedUser.getId(),
                    savedUser.getEmail(),
                    savedUser.getFirstName(),
                    savedUser.getLastName(),
                    List.copyOf(savedUser.getRoles()),
                    savedUser.isEmailVerified()
            );

            log.info("User registered successfully with ID: {}", savedUser.getId());

            return AuthResponse.builder()
                    .token(token)
                    .tokenType("Bearer")
                    .expiresIn((int) jwtService.getExpirationTimeInSeconds())
                    .user(userMapper.toUserResponse(savedUser))
                    .message("Registration successful. Please verify your email.")
                    .build();
        } catch (Exception e) {
            log.error("Error during user registration for email: " + request.getEmail(), e);
            throw e;
        }
    }

    public AuthResponse login(LoginRequest request) {
        log.info("User login attempt for email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new AuthenticationException("Invalid email or password"));

        if (!user.isEnabled()) {
            throw new AuthenticationException("Account is disabled");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new AuthenticationException("Invalid email or password");
        }

        // Update last login
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        // Generate JWT token using enhanced JwtService
        String token = jwtService.generateUserToken(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                List.copyOf(user.getRoles()),
                user.isEmailVerified()
        );

        log.info("User logged in successfully: {}", user.getId());

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn((int) jwtService.getExpirationTimeInSeconds())
                .user(userMapper.toUserResponse(user))
                .message("Login successful")
                .build();
    }



    public void verifyEmail(String token) {
        log.info("Verifying email with token: {}", token);

        User user = userRepository.findByEmailVerificationToken(token)
                .orElseThrow(() -> new BusinessException("Invalid verification token"));

        user.setEmailVerified(true);
        user.setEmailVerificationToken(null);
        userRepository.save(user);

        log.info("Email verified for user: {}", user.getId());
    }

    public void resendVerificationEmail(String email) {
        log.info("Resending verification email to: {}", email);

        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.isEmailVerified()) {
            throw new BusinessException("Email already verified");
        }

        // Generate new verification token
        user.setEmailVerificationToken(UUID.randomUUID().toString());
        userRepository.save(user);

        sendVerificationEmail(user);
    }

    public void forgotPassword(String email) {
        log.info("Password reset requested for: {}", email);

        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Generate password reset token
        String resetToken = UUID.randomUUID().toString();
        user.setPasswordResetToken(resetToken);
        user.setPasswordResetTokenExpiry(LocalDateTime.now().plusHours(1));
        userRepository.save(user);

        sendPasswordResetEmail(user, resetToken);
    }

    public void resetPassword(ResetPasswordRequest request) {
        log.info("Resetting password with token: {}", request.getToken());

        User user = userRepository.findByPasswordResetToken(request.getToken())
                .orElseThrow(() -> new BusinessException("Invalid reset token"));

        if (user.getPasswordResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Reset token has expired");
        }

        // Validate new password
        validatePassword(request.getNewPassword());

        // Update password
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);
        userRepository.save(user);

        log.info("Password reset successfully for user: {}", user.getId());
    }

    public void changePassword(Long userId, ChangePasswordRequest request) {
        log.info("Changing password for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new AuthenticationException("Current password is incorrect");
        }

        validatePassword(request.getNewPassword());

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("Password changed successfully for user: {}", userId);
    }

    public UserResponse getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        return userMapper.toUserResponse(user);
    }





    private void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new BusinessException("Password must be at least 8 characters long");
        }
        
        if (!password.matches(".*[a-z].*")) {
            throw new BusinessException("Password must contain at least one lowercase letter");
        }
        
        if (!password.matches(".*[A-Z].*")) {
            throw new BusinessException("Password must contain at least one uppercase letter");
        }
        
        if (!password.matches(".*[0-9].*")) {
            throw new BusinessException("Password must contain at least one number");
        }
    }

    private void sendVerificationEmail(User user) {
        try {
            String verificationUrl = frontendUrl + verifyEmailPath + "?token=" + user.getEmailVerificationToken();
            String subject = "Verify Your CampusWorks Account";
            String body = String.format(
                "Hi %s,\\n\\nWelcome to CampusWorks! Please click the link below to verify your email address:\\n\\n%s\\n\\nIf you didn't create this account, please ignore this email.\\n\\nBest regards,\\nCampusWorks Team",
                user.getFirstName(), verificationUrl
            );
            
            emailService.sendEmail(user.getEmail(), subject, body);
        } catch (Exception e) {
            log.error("Failed to send verification email: {}", e.getMessage());
        }
    }

    private void sendPasswordResetEmail(User user, String resetToken) {
        try {
            String resetUrl = frontendUrl + resetPasswordPath + "?token=" + resetToken;
            String subject = "Reset Your CampusWorks Password";
            String body = String.format(
                "Hi %s,\\n\\nYou requested to reset your password. Please click the link below to reset it:\\n\\n%s\\n\\nThis link will expire in 1 hour. If you didn't request this reset, please ignore this email.\\n\\nBest regards,\\nCampusWorks Team",
                user.getFirstName(), resetUrl
            );
            
            emailService.sendEmail(user.getEmail(), subject, body);
        } catch (Exception e) {
            log.error("Failed to send password reset email: {}", e.getMessage());
        }
    }
}