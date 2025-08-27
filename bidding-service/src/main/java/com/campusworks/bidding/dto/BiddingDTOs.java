package com.campusworks.bidding.dto;

import com.campusworks.bidding.domain.Bid;
import lombok.*;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateBidRequest {
    @NotNull(message = "Task ID is required")
    private Long taskId;

    @NotNull(message = "Bid amount is required")
    @DecimalMin(value = "0.50", message = "Bid amount must be at least 0.50")
    @DecimalMax(value = "10000.0", message = "Bid amount must not exceed 10000.0")
    private BigDecimal amount;

    @Size(max = 500, message = "Message must not exceed 500 characters")
    private String message;

    @NotNull(message = "Estimated delivery is required")
    @Future(message = "Estimated delivery must be in the future")
    private LocalDateTime estimatedDelivery;
}

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BidResponse {
    private Long id;
    private Long taskId;
    private String taskTitle;
    private Long bidderId;
    private String bidderName;
    private BigDecimal amount;
    private String message;
    private LocalDateTime estimatedDelivery;
    private Bid.BidStatus status;
    private boolean isWinner;
    private LocalDateTime winnerSelectedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateBidRequest {
    @DecimalMin(value = "0.50", message = "Bid amount must be at least 0.50")
    @DecimalMax(value = "10000.0", message = "Bid amount must not exceed 10000.0")
    private BigDecimal amount;

    @Size(max = 500, message = "Message must not exceed 500 characters")
    private String message;

    @Future(message = "Estimated delivery must be in the future")
    private LocalDateTime estimatedDelivery;
}

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BidSummaryResponse {
    private Long taskId;
    private int totalBids;
    private BigDecimal lowestBid;
    private BigDecimal highestBid;
    private BigDecimal averageBid;
    private LocalDateTime biddingDeadline;
    private boolean biddingActive;
    private Long winningBidId;
    private Long winnerUserId;
    private String winnerName;
}