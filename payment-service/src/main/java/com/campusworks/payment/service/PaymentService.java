package com.campusworks.payment.service;

import com.campusworks.payment.domain.Payment;
import com.campusworks.payment.repo.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final RazorpayService razorpayService;
    private final NotificationServiceClient notificationServiceClient;

    @Value("${app.payment.platform.commission-percentage:5.0}")
    private double platformCommissionPercentage;

    public PaymentResponse initiatePayment(PaymentRequest request, Long payerId) {
        log.info("Initiating payment for task {} by user {}", request.getTaskId(), payerId);

        // Calculate amounts
        BigDecimal amount = request.getAmount();
        BigDecimal commission = amount.multiply(BigDecimal.valueOf(platformCommissionPercentage / 100))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal netAmount = amount.subtract(commission);

        // Create payment record
        Payment payment = Payment.builder()
                .taskId(request.getTaskId())
                .payerId(payerId)
                .payeeId(request.getPayeeId())
                .amount(amount)
                .platformCommission(commission)
                .netAmount(netAmount)
                .status(Payment.PaymentStatus.PENDING)
                .method(request.getMethod())
                .transactionId(UUID.randomUUID().toString())
                .build();

        Payment savedPayment = paymentRepository.save(payment);

        try {
            // Create order with payment gateway
            RazorpayOrderResponse gatewayOrder = razorpayService.createOrder(
                    amount, savedPayment.getTransactionId(), "Task Payment"
            );

            savedPayment.setGatewayOrderId(gatewayOrder.getId());
            paymentRepository.save(savedPayment);

            return PaymentResponse.builder()
                    .paymentId(savedPayment.getId())
                    .transactionId(savedPayment.getTransactionId())
                    .gatewayOrderId(gatewayOrder.getId())
                    .amount(amount)
                    .currency("INR")
                    .status(savedPayment.getStatus().toString())
                    .gatewayKey(razorpayService.getKey())
                    .build();

        } catch (Exception e) {
            log.error("Failed to create payment gateway order: {}", e.getMessage());
            savedPayment.setStatus(Payment.PaymentStatus.FAILED);
            savedPayment.setFailureReason("Gateway order creation failed");
            paymentRepository.save(savedPayment);
            throw new RuntimeException("Payment initiation failed");
        }
    }

    public PaymentResponse verifyPayment(PaymentVerificationRequest request) {
        log.info("Verifying payment for transaction: {}", request.getTransactionId());

        Payment payment = paymentRepository.findByTransactionId(request.getTransactionId())
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        try {
            // Verify payment with gateway
            boolean isVerified = razorpayService.verifyPayment(
                    payment.getGatewayOrderId(),
                    request.getGatewayPaymentId(),
                    request.getGatewaySignature()
            );

            if (isVerified) {
                payment.setStatus(Payment.PaymentStatus.COMPLETED);
                payment.setGatewayPaymentId(request.getGatewayPaymentId());
                payment.setGatewaySignature(request.getGatewaySignature());
                payment.setPaidAt(LocalDateTime.now());

                // Send payment confirmation notifications
                sendPaymentNotifications(payment, true);

                log.info("Payment verified successfully for transaction: {}", request.getTransactionId());
            } else {
                payment.setStatus(Payment.PaymentStatus.FAILED);
                payment.setFailureReason("Payment verification failed");

                sendPaymentNotifications(payment, false);

                log.warn("Payment verification failed for transaction: {}", request.getTransactionId());
            }

            paymentRepository.save(payment);

            return PaymentResponse.builder()
                    .paymentId(payment.getId())
                    .transactionId(payment.getTransactionId())
                    .amount(payment.getAmount())
                    .status(payment.getStatus().toString())
                    .build();

        } catch (Exception e) {
            log.error("Payment verification error: {}", e.getMessage());
            payment.setStatus(Payment.PaymentStatus.FAILED);
            payment.setFailureReason("Verification error: " + e.getMessage());
            paymentRepository.save(payment);
            throw new RuntimeException("Payment verification failed");
        }
    }

    public void createEscrowPayment(EscrowPaymentRequest request) {
        log.info("Creating escrow payment for task {}", request.getTaskId());

        // Check if payment already exists
        if (paymentRepository.existsByTaskId(request.getTaskId())) {
            log.warn("Payment already exists for task {}", request.getTaskId());
            return;
        }

        BigDecimal amount = request.getAmount();
        BigDecimal commission = amount.multiply(BigDecimal.valueOf(platformCommissionPercentage / 100))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal netAmount = amount.subtract(commission);

        Payment payment = Payment.builder()
                .taskId(request.getTaskId())
                .payerId(request.getPayerId())
                .payeeId(request.getPayeeId())
                .amount(amount)
                .platformCommission(commission)
                .netAmount(netAmount)
                .status(Payment.PaymentStatus.COMPLETED) // Assuming payment is already made during task assignment
                .method(Payment.PaymentMethod.UPI) // Default method
                .transactionId(UUID.randomUUID().toString())
                .paidAt(LocalDateTime.now())
                .build();

        paymentRepository.save(payment);
        log.info("Escrow payment created for task {}", request.getTaskId());
    }

    public void releaseEscrowPayment(Long taskId) {
        log.info("Releasing escrow payment for task {}", taskId);

        Payment payment = paymentRepository.findByTaskId(taskId)
                .orElseThrow(() -> new RuntimeException("Payment not found for task"));

        if (payment.getStatus() != Payment.PaymentStatus.COMPLETED) {
            throw new RuntimeException("Payment is not in completed state");
        }

        if (payment.getReleasedAt() != null) {
            log.warn("Payment already released for task {}", taskId);
            return;
        }

        try {
            // Here you would implement actual money transfer to payee
            // For now, we'll simulate it
            simulatePayeeTransfer(payment);

            payment.setStatus(Payment.PaymentStatus.RELEASED);
            payment.setReleasedAt(LocalDateTime.now());
            paymentRepository.save(payment);

            // Send release notifications
            sendReleaseNotifications(payment);

            log.info("Escrow payment released for task {}", taskId);

        } catch (Exception e) {
            log.error("Failed to release escrow payment for task {}: {}", taskId, e.getMessage());
            throw new RuntimeException("Failed to release payment");
        }
    }

    public void refundEscrowPayment(Long taskId) {
        log.info("Refunding escrow payment for task {}", taskId);

        Payment payment = paymentRepository.findByTaskId(taskId)
                .orElseThrow(() -> new RuntimeException("Payment not found for task"));

        if (payment.getStatus() == Payment.PaymentStatus.REFUNDED) {
            log.warn("Payment already refunded for task {}", taskId);
            return;
        }

        try {
            // Initiate refund with payment gateway
            if (payment.getGatewayPaymentId() != null) {
                razorpayService.refundPayment(payment.getGatewayPaymentId(), payment.getAmount());
            }

            payment.setStatus(Payment.PaymentStatus.REFUNDED);
            paymentRepository.save(payment);

            // Send refund notifications
            sendRefundNotifications(payment);

            log.info("Escrow payment refunded for task {}", taskId);

        } catch (Exception e) {
            log.error("Failed to refund escrow payment for task {}: {}", taskId, e.getMessage());
            throw new RuntimeException("Failed to refund payment");
        }
    }

    public List<PaymentResponse> getUserPayments(Long userId, String type) {
        List<Payment> payments;
        
        if ("sent".equalsIgnoreCase(type)) {
            payments = paymentRepository.findByPayerIdOrderByCreatedAtDesc(userId);
        } else if ("received".equalsIgnoreCase(type)) {
            payments = paymentRepository.findByPayeeIdOrderByCreatedAtDesc(userId);
        } else {
            payments = paymentRepository.findByPayerIdOrPayeeIdOrderByCreatedAtDesc(userId, userId);
        }
        
        return payments.stream().map(this::mapToResponse).toList();
    }

    public PaymentResponse getPaymentById(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        return mapToResponse(payment);
    }

    public PaymentResponse getPaymentByTaskId(Long taskId) {
        Payment payment = paymentRepository.findByTaskId(taskId)
                .orElseThrow(() -> new RuntimeException("Payment not found for task"));
        return mapToResponse(payment);
    }

    private void simulatePayeeTransfer(Payment payment) {
        // In a real implementation, this would transfer money to the payee's bank account
        // For now, we'll just log it
        log.info("Transferring ₹{} to user {} for task {}", 
                payment.getNetAmount(), payment.getPayeeId(), payment.getTaskId());
    }

    private void sendPaymentNotifications(Payment payment, boolean success) {
        try {
            String type = success ? "PAYMENT_COMPLETED" : "PAYMENT_FAILED";
            String title = success ? "Payment Successful" : "Payment Failed";
            String message = success ? 
                    String.format("Payment of ₹%.2f has been completed successfully", payment.getAmount()) :
                    "Payment processing failed. Please try again.";

            // Notify payer
            NotificationRequest payerNotification = NotificationRequest.builder()
                    .userId(payment.getPayerId())
                    .type(type)
                    .title(title)
                    .message(message)
                    .relatedEntityId(payment.getTaskId())
                    .relatedEntityType("PAYMENT")
                    .build();

            notificationServiceClient.sendNotification(payerNotification);

            if (success) {
                // Notify payee about payment being held in escrow
                NotificationRequest payeeNotification = NotificationRequest.builder()
                        .userId(payment.getPayeeId())
                        .type("PAYMENT_RECEIVED")
                        .title("Payment Received")
                        .message(String.format("Payment of ₹%.2f is being held in escrow for your task", 
                                payment.getAmount()))
                        .relatedEntityId(payment.getTaskId())
                        .relatedEntityType("PAYMENT")
                        .build();

                notificationServiceClient.sendNotification(payeeNotification);
            }

        } catch (Exception e) {
            log.error("Failed to send payment notifications: {}", e.getMessage());
        }
    }

    private void sendReleaseNotifications(Payment payment) {
        try {
            // Notify payee about payment release
            NotificationRequest payeeNotification = NotificationRequest.builder()
                    .userId(payment.getPayeeId())
                    .type("PAYMENT_RELEASED")
                    .title("Payment Released!")
                    .message(String.format("₹%.2f has been transferred to your account", 
                            payment.getNetAmount()))
                    .relatedEntityId(payment.getTaskId())
                    .relatedEntityType("PAYMENT")
                    .build();

            // Notify payer about payment completion
            NotificationRequest payerNotification = NotificationRequest.builder()
                    .userId(payment.getPayerId())
                    .type("PAYMENT_COMPLETED")
                    .title("Task Completed")
                    .message("Your task has been completed and payment has been released")
                    .relatedEntityId(payment.getTaskId())
                    .relatedEntityType("PAYMENT")
                    .build();

            notificationServiceClient.sendNotification(payeeNotification);
            notificationServiceClient.sendNotification(payerNotification);

        } catch (Exception e) {
            log.error("Failed to send release notifications: {}", e.getMessage());
        }
    }

    private void sendRefundNotifications(Payment payment) {
        try {
            NotificationRequest notification = NotificationRequest.builder()
                    .userId(payment.getPayerId())
                    .type("PAYMENT_REFUNDED")
                    .title("Payment Refunded")
                    .message(String.format("₹%.2f has been refunded to your account", 
                            payment.getAmount()))
                    .relatedEntityId(payment.getTaskId())
                    .relatedEntityType("PAYMENT")
                    .build();

            notificationServiceClient.sendNotification(notification);

        } catch (Exception e) {
            log.error("Failed to send refund notifications: {}", e.getMessage());
        }
    }

    private PaymentResponse mapToResponse(Payment payment) {
        return PaymentResponse.builder()
                .paymentId(payment.getId())
                .taskId(payment.getTaskId())
                .transactionId(payment.getTransactionId())
                .amount(payment.getAmount())
                .platformCommission(payment.getPlatformCommission())
                .netAmount(payment.getNetAmount())
                .status(payment.getStatus().toString())
                .method(payment.getMethod().toString())
                .paidAt(payment.getPaidAt())
                .releasedAt(payment.getReleasedAt())
                .createdAt(payment.getCreatedAt())
                .build();
    }

    // Feign client
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
    public static class PaymentRequest {
        private Long taskId;
        private Long payeeId;
        private BigDecimal amount;
        private Payment.PaymentMethod method;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class PaymentVerificationRequest {
        private String transactionId;
        private String gatewayPaymentId;
        private String gatewaySignature;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class EscrowPaymentRequest {
        private Long taskId;
        private Long payerId;
        private Long payeeId;
        private BigDecimal amount;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class PaymentResponse {
        private Long paymentId;
        private Long taskId;
        private String transactionId;
        private String gatewayOrderId;
        private BigDecimal amount;
        private BigDecimal platformCommission;
        private BigDecimal netAmount;
        private String currency;
        private String status;
        private String method;
        private String gatewayKey;
        private LocalDateTime paidAt;
        private LocalDateTime releasedAt;
        private LocalDateTime createdAt;
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
        private Long relatedEntityId;
        private String relatedEntityType;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class RazorpayOrderResponse {
        private String id;
        private String entity;
        private BigDecimal amount;
        private String currency;
        private String status;
    }
}