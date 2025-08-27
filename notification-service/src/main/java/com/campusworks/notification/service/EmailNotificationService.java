package com.campusworks.notification.service;

import com.campusworks.notification.domain.Notification;
import com.campusworks.notification.service.NotificationService.UserPreferences;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@campusworks.com}")
    private String fromEmail;

    public void sendNotificationEmail(Notification notification, UserPreferences preferences) {
        try {
            log.info("Sending email notification to: {}", preferences.getUserEmail());

            if (preferences.getUserEmail() == null || preferences.getUserEmail().isEmpty()) {
                log.warn("No email address found for user notification: {}", notification.getId());
                return;
            }

            String emailBody = createEmailBody(notification, preferences);
            
            if (isHtmlContent(notification)) {
                sendHtmlEmail(preferences.getUserEmail(), notification.getTitle(), emailBody);
            } else {
                sendSimpleEmail(preferences.getUserEmail(), notification.getTitle(), emailBody);
            }

            // Mark email as sent in notification
            notification.setEmailSent(true);
            log.info("Email notification sent successfully to: {}", preferences.getUserEmail());

        } catch (Exception e) {
            log.error("Failed to send email notification to {}: {}", preferences.getUserEmail(), e.getMessage());
        }
    }

    private void sendSimpleEmail(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        
        mailSender.send(message);
    }

    private void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send HTML email: {}", e.getMessage());
            // Fallback to simple email
            sendSimpleEmail(to, subject, stripHtml(htmlBody));
        }
    }

    private String createEmailBody(Notification notification, UserPreferences preferences) {
        String userName = preferences.getUserName() != null ? preferences.getUserName() : "User";

        return switch (notification.getType()) {
            case NEW_BID_RECEIVED -> createBidReceivedEmail(notification, userName);
            case BID_WON -> createBidWonEmail(notification, userName);
            case BID_LOST -> createBidLostEmail(notification, userName);
            case TASK_ASSIGNED -> createTaskAssignedEmail(notification, userName);
            case TASK_COMPLETED -> createTaskCompletedEmail(notification, userName);
            case PAYMENT_COMPLETED -> createPaymentCompletedEmail(notification, userName);
            case PAYMENT_RECEIVED -> createPaymentReceivedEmail(notification, userName);
            case PAYMENT_RELEASED -> createPaymentReleasedEmail(notification, userName);
            case PAYMENT_REFUNDED -> createPaymentRefundedEmail(notification, userName);
            case WORK_SUBMITTED -> createWorkSubmittedEmail(notification, userName);
            case WORK_ACCEPTED -> createWorkAcceptedEmail(notification, userName);
            case WORK_REJECTED -> createWorkRejectedEmail(notification, userName);
            case ACCOUNT_VERIFIED -> createAccountVerifiedEmail(notification, userName);
            case NEW_MESSAGE -> createNewMessageEmail(notification, userName);
            case REMINDER -> createReminderEmail(notification, userName);
            case SYSTEM_MAINTENANCE -> createSystemMaintenanceEmail(notification, userName);
            default -> createGenericEmail(notification, userName);
        };
    }

    private String createBidReceivedEmail(Notification notification, String userName) {
        return String.format("""
            Hi %s,
            
            Great news! You've received a new bid on your task.
            
            %s
            
            To view the bid details and manage your task, visit:
            http://localhost:3000/tasks/%s
            
            Best regards,
            CampusWorks Team
            """, userName, notification.getMessage(), notification.getRelatedEntityId());
    }

    private String createBidWonEmail(Notification notification, String userName) {
        return String.format("""
            Hi %s,
            
            Congratulations! Your bid has been selected as the winner.
            
            %s
            
            You can now start working on the task. View task details:
            http://localhost:3000/tasks/%s
            
            Best regards,
            CampusWorks Team
            """, userName, notification.getMessage(), notification.getRelatedEntityId());
    }

    private String createBidLostEmail(Notification notification, String userName) {
        return String.format("""
            Hi %s,
            
            Thank you for your interest in the task. Unfortunately, another bid was selected this time.
            
            %s
            
            Don't worry! There are many more opportunities available. Browse new tasks:
            http://localhost:3000/tasks
            
            Best regards,
            CampusWorks Team
            """, userName, notification.getMessage());
    }

    private String createTaskAssignedEmail(Notification notification, String userName) {
        return String.format("""
            Hi %s,
            
            Your task has been assigned to a qualified candidate.
            
            %s
            
            You can track the progress and communicate with the assignee:
            http://localhost:3000/tasks/%s
            
            Best regards,
            CampusWorks Team
            """, userName, notification.getMessage(), notification.getRelatedEntityId());
    }

    private String createPaymentCompletedEmail(Notification notification, String userName) {
        return String.format("""
            Hi %s,
            
            Your payment has been processed successfully.
            
            %s
            
            You can view the payment details in your account:
            http://localhost:3000/payments
            
            Best regards,
            CampusWorks Team
            """, userName, notification.getMessage());
    }

    private String createPaymentReleasedEmail(Notification notification, String userName) {
        return String.format("""
            Hi %s,
            
            Great news! Your payment has been released.
            
            %s
            
            The amount will be transferred to your registered bank account within 1-2 business days.
            
            Best regards,
            CampusWorks Team
            """, userName, notification.getMessage());
    }

    private String createWorkSubmittedEmail(Notification notification, String userName) {
        return String.format("""
            Hi %s,
            
            Work has been submitted for your task.
            
            %s
            
            Please review the submission and provide your feedback:
            http://localhost:3000/tasks/%s
            
            Best regards,
            CampusWorks Team
            """, userName, notification.getMessage(), notification.getRelatedEntityId());
    }

    private String createWorkAcceptedEmail(Notification notification, String userName) {
        return String.format("""
            Hi %s,
            
            Excellent! Your work has been accepted.
            
            %s
            
            Payment will be processed and released to your account shortly.
            
            Best regards,
            CampusWorks Team
            """, userName, notification.getMessage());
    }

    private String createAccountVerifiedEmail(Notification notification, String userName) {
        return String.format("""
            Hi %s,
            
            Welcome to CampusWorks! Your account has been successfully verified.
            
            You can now:
            • Post tasks for others to complete
            • Browse and bid on available tasks
            • Earn money by completing tasks
            • Communicate with other users
            
            Get started: http://localhost:3000
            
            Best regards,
            CampusWorks Team
            """, userName);
    }

    private String createNewMessageEmail(Notification notification, String userName) {
        return String.format("""
            Hi %s,
            
            You have a new message regarding your task.
            
            %s
            
            View the conversation:
            http://localhost:3000/chat
            
            Best regards,
            CampusWorks Team
            """, userName, notification.getMessage());
    }

    private String createReminderEmail(Notification notification, String userName) {
        return String.format("""
            Hi %s,
            
            This is a friendly reminder:
            
            %s
            
            Please take the necessary action at your earliest convenience.
            
            Best regards,
            CampusWorks Team
            """, userName, notification.getMessage());
    }

    private String createSystemMaintenanceEmail(Notification notification, String userName) {
        return String.format("""
            Hi %s,
            
            System Maintenance Notice:
            
            %s
            
            We apologize for any inconvenience this may cause.
            
            Best regards,
            CampusWorks Team
            """, userName, notification.getMessage());
    }

    private String createGenericEmail(Notification notification, String userName) {
        return String.format("""
            Hi %s,
            
            %s
            
            %s
            
            If you have any questions, please contact our support team.
            
            Best regards,
            CampusWorks Team
            """, userName, notification.getTitle(), notification.getMessage());
    }

    // Additional specialized email methods
    private String createTaskCompletedEmail(Notification notification, String userName) {
        return createGenericEmail(notification, userName);
    }

    private String createPaymentReceivedEmail(Notification notification, String userName) {
        return createGenericEmail(notification, userName);
    }

    private String createPaymentRefundedEmail(Notification notification, String userName) {
        return createGenericEmail(notification, userName);
    }

    private String createWorkRejectedEmail(Notification notification, String userName) {
        return createGenericEmail(notification, userName);
    }

    private boolean isHtmlContent(Notification notification) {
        // For now, all emails are plain text
        // You can enhance this to support HTML based on notification type or user preference
        return false;
    }

    private String stripHtml(String html) {
        return html.replaceAll("<[^>]+>", "");
    }
}