package com.campusworks.payment.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long taskId;

    @Column(nullable = false)
    private Long payerId; // task owner who pays

    @Column(nullable = false)
    private Long payeeId; // task assignee who receives payment

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal platformCommission;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal netAmount; // amount after commission

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod method;

    @Column(unique = true)
    private String transactionId; // from payment gateway

    @Column(unique = true)
    private String gatewayOrderId; // order ID from payment gateway

    private String gatewayPaymentId; // payment ID from gateway

    private String gatewaySignature; // signature for verification

    private String failureReason;

    private LocalDateTime paidAt;

    private LocalDateTime releasedAt; // when payment is released to assignee

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public enum PaymentStatus {
        PENDING,        // Payment initiated but not completed
        PROCESSING,     // Payment being processed by gateway
        COMPLETED,      // Payment successful and held in escrow
        RELEASED,       // Payment released to assignee
        REFUNDED,       // Payment refunded to payer
        FAILED,         // Payment failed
        CANCELLED       // Payment cancelled
    }

    public enum PaymentMethod {
        CREDIT_CARD,
        DEBIT_CARD,
        UPI,
        NET_BANKING,
        WALLET,
        OTHER
    }
}