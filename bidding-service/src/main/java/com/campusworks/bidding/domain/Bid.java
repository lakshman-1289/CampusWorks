package com.campusworks.bidding.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bids", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"taskId", "bidderId"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bid {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long taskId;

    @Column(nullable = false)
    private Long bidderId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;
    
    @Column(length = 500)
    private String message; // optional message from bidder
    
    @Column(nullable = false)
    private LocalDateTime estimatedDelivery; // when bidder can complete
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BidStatus status = BidStatus.ACTIVE;
    
    private boolean isWinner = false;
    
    private LocalDateTime winnerSelectedAt;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    // Enum for bid status
    public enum BidStatus {
        ACTIVE,     // Bid is active and valid
        WITHDRAWN,  // Bidder withdrew the bid
        EXPIRED,    // Bid expired due to deadline
        WON,        // This bid won the task
        LOST        // This bid lost to another bid
    }
}


