package com.campusworks.payment.repo;

import com.campusworks.payment.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    
    // Basic lookups
    Optional<Payment> findByTransactionId(String transactionId);
    
    Optional<Payment> findByTaskId(Long taskId);
    
    boolean existsByTaskId(Long taskId);
    
    Optional<Payment> findByGatewayOrderId(String gatewayOrderId);
    
    Optional<Payment> findByGatewayPaymentId(String gatewayPaymentId);
    
    // User-based queries
    List<Payment> findByPayerIdOrderByCreatedAtDesc(Long payerId);
    
    List<Payment> findByPayeeIdOrderByCreatedAtDesc(Long payeeId);
    
    List<Payment> findByPayerIdOrPayeeIdOrderByCreatedAtDesc(Long payerId, Long payeeId);
    
    List<Payment> findByPayerIdAndStatusOrderByCreatedAtDesc(Long payerId, Payment.PaymentStatus status);
    
    List<Payment> findByPayeeIdAndStatusOrderByCreatedAtDesc(Long payeeId, Payment.PaymentStatus status);
    
    // Status-based queries
    List<Payment> findByStatus(Payment.PaymentStatus status);
    
    List<Payment> findByStatusOrderByCreatedAtDesc(Payment.PaymentStatus status);
    
    List<Payment> findByStatusInOrderByCreatedAtDesc(List<Payment.PaymentStatus> statuses);
    
    // Time-based queries
    List<Payment> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    
    List<Payment> findByPaidAtBetween(LocalDateTime start, LocalDateTime end);
    
    List<Payment> findByReleasedAtBetween(LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT p FROM Payment p WHERE p.status = 'COMPLETED' AND p.releasedAt IS NULL")
    List<Payment> findPendingReleasePayments();
    
    @Query("SELECT p FROM Payment p WHERE p.status = 'PENDING' AND p.createdAt < :cutoffTime")
    List<Payment> findExpiredPendingPayments(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    // Amount-based queries
    @Query("SELECT p FROM Payment p WHERE p.amount >= :minAmount AND p.amount <= :maxAmount")
    List<Payment> findByAmountRange(@Param("minAmount") BigDecimal minAmount, @Param("maxAmount") BigDecimal maxAmount);
    
    // Statistical queries
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.payerId = :userId AND p.status = :status")
    Long countByPayerIdAndStatus(@Param("userId") Long userId, @Param("status") Payment.PaymentStatus status);
    
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.payeeId = :userId AND p.status = :status")
    Long countByPayeeIdAndStatus(@Param("userId") Long userId, @Param("status") Payment.PaymentStatus status);
    
    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.payerId = :userId AND p.status = :status")
    Optional<BigDecimal> sumAmountByPayerIdAndStatus(@Param("userId") Long userId, @Param("status") Payment.PaymentStatus status);
    
    @Query("SELECT SUM(p.netAmount) FROM Payment p WHERE p.payeeId = :userId AND p.status = :status")
    Optional<BigDecimal> sumNetAmountByPayeeIdAndStatus(@Param("userId") Long userId, @Param("status") Payment.PaymentStatus status);
    
    @Query("SELECT SUM(p.platformCommission) FROM Payment p WHERE p.status = 'RELEASED' AND p.releasedAt BETWEEN :start AND :end")
    Optional<BigDecimal> sumPlatformCommissionBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    // Method-based queries
    List<Payment> findByMethodOrderByCreatedAtDesc(Payment.PaymentMethod method);
    
    @Query("SELECT p.method, COUNT(p) FROM Payment p WHERE p.status = 'COMPLETED' GROUP BY p.method")
    List<Object[]> getPaymentCountByMethod();
    
    // Daily/Monthly statistics
    @Query("SELECT DATE(p.createdAt), COUNT(p), SUM(p.amount) FROM Payment p WHERE p.createdAt >= :startDate GROUP BY DATE(p.createdAt) ORDER BY DATE(p.createdAt)")
    List<Object[]> getDailyPaymentStats(@Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT YEAR(p.createdAt), MONTH(p.createdAt), COUNT(p), SUM(p.amount) FROM Payment p GROUP BY YEAR(p.createdAt), MONTH(p.createdAt) ORDER BY YEAR(p.createdAt), MONTH(p.createdAt)")
    List<Object[]> getMonthlyPaymentStats();
    
    // Failed payment analysis
    @Query("SELECT p.failureReason, COUNT(p) FROM Payment p WHERE p.status = 'FAILED' AND p.failureReason IS NOT NULL GROUP BY p.failureReason")
    List<Object[]> getFailureReasonStats();
}