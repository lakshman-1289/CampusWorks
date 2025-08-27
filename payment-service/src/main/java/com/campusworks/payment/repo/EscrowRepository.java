package com.campusworks.payment.repo;

import com.campusworks.payment.domain.Escrow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EscrowRepository extends JpaRepository<Escrow, Long> {
    
    // Task-based queries
    Optional<Escrow> findByTaskId(Long taskId);
    
    List<Escrow> findByTaskIdIn(List<Long> taskIds);
    
    // User-based queries
    List<Escrow> findByPayerIdOrderByCreatedAtDesc(Long payerId);
    
    List<Escrow> findByPayeeIdOrderByCreatedAtDesc(Long payeeId);
    
    Page<Escrow> findByPayerIdOrderByCreatedAtDesc(Long payerId, Pageable pageable);
    
    Page<Escrow> findByPayeeIdOrderByCreatedAtDesc(Long payeeId, Pageable pageable);
    
    // Status-based queries
    List<Escrow> findByStatus(Escrow.EscrowStatus status);
    
    List<Escrow> findByStatusOrderByCreatedAtDesc(Escrow.EscrowStatus status);
    
    Page<Escrow> findByStatusOrderByCreatedAtDesc(Escrow.EscrowStatus status, Pageable pageable);
    
    List<Escrow> findByStatusIn(List<Escrow.EscrowStatus> statuses);
    
    // Amount-based queries
    @Query("SELECT e FROM Escrow e WHERE e.totalAmount >= :minAmount AND e.totalAmount <= :maxAmount AND e.status = :status")
    List<Escrow> findByAmountRangeAndStatus(@Param("minAmount") BigDecimal minAmount, 
                                           @Param("maxAmount") BigDecimal maxAmount, 
                                           @Param("status") Escrow.EscrowStatus status);
    
    // Time-based queries
    List<Escrow> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("SELECT e FROM Escrow e WHERE e.autoReleaseEnabled = true AND e.autoReleaseDate <= :currentTime AND e.status = 'HELD'")
    List<Escrow> findEscrowsReadyForAutoRelease(@Param("currentTime") LocalDateTime currentTime);
    
    @Query("SELECT e FROM Escrow e WHERE e.status = 'HELD' AND e.createdAt <= :cutoffTime")
    List<Escrow> findStaleHeldEscrows(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    // Statistical queries
    Long countByStatus(Escrow.EscrowStatus status);
    
    Long countByPayerId(Long payerId);
    
    Long countByPayeeId(Long payeeId);
    
    @Query("SELECT SUM(e.totalAmount) FROM Escrow e WHERE e.status = 'HELD'")
    Optional<BigDecimal> getTotalHeldAmount();
    
    @Query("SELECT SUM(e.totalAmount) FROM Escrow e WHERE e.status = 'RELEASED'")
    Optional<BigDecimal> getTotalReleasedAmount();
    
    @Query("SELECT SUM(e.platformFee) FROM Escrow e WHERE e.status = 'RELEASED'")
    Optional<BigDecimal> getTotalPlatformFeesEarned();
    
    // Payer/Payee specific statistics
    @Query("SELECT SUM(e.totalAmount) FROM Escrow e WHERE e.payerId = :payerId AND e.status IN ('HELD', 'RELEASED')")
    Optional<BigDecimal> getTotalAmountPaidByUser(@Param("payerId") Long payerId);
    
    @Query("SELECT SUM(e.netAmount) FROM Escrow e WHERE e.payeeId = :payeeId AND e.status = 'RELEASED'")
    Optional<BigDecimal> getTotalAmountEarnedByUser(@Param("payeeId") Long payeeId);
    
    // Dispute-related queries
    @Query("SELECT e FROM Escrow e WHERE e.status = 'DISPUTED' ORDER BY e.disputeRaisedAt DESC")
    List<Escrow> findDisputedEscrows();
    
    @Query("SELECT e FROM Escrow e WHERE e.disputeRaisedBy = :userId AND e.status = 'DISPUTED'")
    List<Escrow> findDisputesRaisedByUser(@Param("userId") Long userId);
    
    // Payment gateway related
    Optional<Escrow> findByPaymentGatewayTransactionId(String transactionId);
    
    List<Escrow> findByPaymentMethodAndStatus(String paymentMethod, Escrow.EscrowStatus status);
    
    // Recent activity
    @Query("SELECT e FROM Escrow e WHERE e.updatedAt >= :since ORDER BY e.updatedAt DESC")
    List<Escrow> findRecentlyUpdatedEscrows(@Param("since") LocalDateTime since, Pageable pageable);
    
    // Validation queries
    boolean existsByTaskId(Long taskId);
    
    boolean existsByTaskIdAndStatus(Long taskId, Escrow.EscrowStatus status);
}


