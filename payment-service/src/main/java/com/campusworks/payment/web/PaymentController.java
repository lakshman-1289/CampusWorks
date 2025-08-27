package com.campusworks.payment.web;

import com.campusworks.payment.service.PaymentService;
import com.campusworks.payment.service.PaymentService.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/initiate")
    public ResponseEntity<PaymentResponse> initiatePayment(
            @Valid @RequestBody PaymentRequest request,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        PaymentResponse response = paymentService.initiatePayment(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/verify")
    public ResponseEntity<PaymentResponse> verifyPayment(
            @Valid @RequestBody PaymentVerificationRequest request) {
        PaymentResponse response = paymentService.verifyPayment(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/escrow")
    public ResponseEntity<Void> createEscrowPayment(@RequestBody EscrowPaymentRequest request) {
        // This endpoint is for inter-service communication
        paymentService.createEscrowPayment(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/escrow/{taskId}/release")
    public ResponseEntity<Void> releaseEscrowPayment(@PathVariable Long taskId) {
        // This endpoint is for inter-service communication
        paymentService.releaseEscrowPayment(taskId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/escrow/{taskId}/refund")
    public ResponseEntity<Void> refundEscrowPayment(@PathVariable Long taskId) {
        // This endpoint is for inter-service communication
        paymentService.refundEscrowPayment(taskId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/my-payments")
    public ResponseEntity<List<PaymentResponse>> getMyPayments(
            @RequestParam(required = false) String type,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        List<PaymentResponse> payments = paymentService.getUserPayments(userId, type);
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> getPaymentById(
            @PathVariable Long paymentId,
            Authentication authentication) {
        // In a real implementation, you'd verify the user has access to this payment
        PaymentResponse payment = paymentService.getPaymentById(paymentId);
        return ResponseEntity.ok(payment);
    }

    @GetMapping("/task/{taskId}")
    public ResponseEntity<PaymentResponse> getPaymentByTaskId(
            @PathVariable Long taskId,
            Authentication authentication) {
        PaymentResponse payment = paymentService.getPaymentByTaskId(taskId);
        return ResponseEntity.ok(payment);
    }

    // Webhook endpoint for payment gateway
    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Razorpay-Signature") String signature) {
        try {
            // In a real implementation, you'd verify the webhook signature
            // and process the payment status update
            // For now, we'll just return success
            return ResponseEntity.ok("Webhook processed");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Webhook processing failed");
        }
    }

    // Statistics endpoints
    @GetMapping("/stats/user")
    public ResponseEntity<UserPaymentStats> getUserPaymentStats(Authentication authentication) {
        Long userId = extractUserId(authentication);
        // Implementation for user payment statistics
        UserPaymentStats stats = UserPaymentStats.builder()
                .totalPaid(java.math.BigDecimal.ZERO)
                .totalReceived(java.math.BigDecimal.ZERO)
                .totalTransactions(0L)
                .build();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/health")
    public ResponseEntity<MessageResponse> health() {
        return ResponseEntity.ok(new MessageResponse("Payment service is healthy"));
    }

    // Exception handlers
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException e) {
        ErrorResponse error = ErrorResponse.builder()
                .message(e.getMessage())
                .timestamp(java.time.LocalDateTime.now())
                .build();
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            org.springframework.web.bind.MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((first, second) -> first + ", " + second)
                .orElse("Validation failed");
        
        ErrorResponse error = ErrorResponse.builder()
                .message(message)
                .timestamp(java.time.LocalDateTime.now())
                .build();
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            org.springframework.security.access.AccessDeniedException e) {
        ErrorResponse error = ErrorResponse.builder()
                .message("Access denied")
                .timestamp(java.time.LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new RuntimeException("User not authenticated");
        }
        return Long.valueOf(authentication.getName());
    }

    // Response DTOs
    public record MessageResponse(String message) {}

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ErrorResponse {
        private String message;
        private java.time.LocalDateTime timestamp;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class UserPaymentStats {
        private java.math.BigDecimal totalPaid;
        private java.math.BigDecimal totalReceived;
        private Long totalTransactions;
        private java.time.LocalDateTime lastPaymentDate;
    }
}