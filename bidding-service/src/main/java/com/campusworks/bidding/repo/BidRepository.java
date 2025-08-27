package com.campusworks.bidding.repo;

import com.campusworks.bidding.domain.Bid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BidRepository extends JpaRepository<Bid, Long> {
    
    List<Bid> findByTaskIdOrderByAmountAsc(Long taskId);
    
    List<Bid> findByTaskIdAndStatusOrderByAmountAsc(Long taskId, Bid.BidStatus status);
    
    List<Bid> findByBidderIdOrderByCreatedAtDesc(Long bidderId);
    
    List<Bid> findByBidderIdAndStatusOrderByCreatedAtDesc(Long bidderId, Bid.BidStatus status);
    
    Optional<Bid> findByTaskIdAndBidderId(Long taskId, Long bidderId);
    
    boolean existsByTaskIdAndBidderId(Long taskId, Long bidderId);
    
    Optional<Bid> findByTaskIdAndIsWinnerTrue(Long taskId);
    
    int countByTaskIdAndStatus(Long taskId, Bid.BidStatus status);
    
    @Query("SELECT MIN(b.amount) FROM Bid b WHERE b.taskId = :taskId AND b.status = :status")
    Optional<java.math.BigDecimal> findLowestBidAmountByTaskIdAndStatus(
        @Param("taskId") Long taskId, 
        @Param("status") Bid.BidStatus status
    );
    
    @Query("SELECT MAX(b.amount) FROM Bid b WHERE b.taskId = :taskId AND b.status = :status")
    Optional<java.math.BigDecimal> findHighestBidAmountByTaskIdAndStatus(
        @Param("taskId") Long taskId, 
        @Param("status") Bid.BidStatus status
    );
    
    @Query("SELECT AVG(b.amount) FROM Bid b WHERE b.taskId = :taskId AND b.status = :status")
    Optional<Double> findAverageBidAmountByTaskIdAndStatus(
        @Param("taskId") Long taskId, 
        @Param("status") Bid.BidStatus status
    );
    
    @Query("SELECT b FROM Bid b WHERE b.taskId = :taskId AND b.status = :status ORDER BY b.amount ASC")
    List<Bid> findTopBidsByTaskIdAndStatus(
        @Param("taskId") Long taskId, 
        @Param("status") Bid.BidStatus status,
        org.springframework.data.domain.Pageable pageable
    );
    
    List<Bid> findByStatusAndWinnerSelectedAtIsNull(Bid.BidStatus status);
}


