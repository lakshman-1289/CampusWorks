package com.campusworks.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Value("${app.frontend.verify-email-path}")
    private String verifyEmailPath;

    @Value("${app.frontend.reset-password-path}")
    private String resetPasswordPath;

    public void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            
            mailSender.send(message);
            log.info("Email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
            throw new RuntimeException("Failed to send email");
        }
    }

    public void sendVerificationEmail(String to, String name, String verificationToken) {
        String subject = "Verify Your CampusWorks Account";
        String verificationUrl = frontendUrl + verifyEmailPath + "?token=" + verificationToken;
        
        String body = String.format(
            "Hi %s,\n\n" +
            "Welcome to CampusWorks! Please click the link below to verify your email address:\n\n" +
            "%s\n\n" +
            "If you didn't create this account, please ignore this email.\n\n" +
            "Best regards,\n" +
            "CampusWorks Team",
            name, verificationUrl
        );
        
        sendEmail(to, subject, body);
    }

    public void sendPasswordResetEmail(String to, String name, String resetToken) {
        String subject = "Reset Your CampusWorks Password";
        String resetUrl = frontendUrl + resetPasswordPath + "?token=" + resetToken;
        
        String body = String.format(
            "Hi %s,\n\n" +
            "You requested to reset your password. Please click the link below to reset it:\n\n" +
            "%s\n\n" +
            "This link will expire in 1 hour. If you didn't request this reset, please ignore this email.\n\n" +
            "Best regards,\n" +
            "CampusWorks Team",
            name, resetUrl
        );
        
        sendEmail(to, subject, body);
    }


}