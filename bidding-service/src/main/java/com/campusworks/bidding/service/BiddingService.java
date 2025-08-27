package com.campusworks.bidding.service;

import com.campusworks.bidding.domain.Bid;
import com.campusworks.bidding.dto.BiddingDTOs.*;
import com.campusworks.bidding.repo.BidRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BiddingService {

    private final BidRepository bidRepository;
    private final TaskServiceClient taskServiceClient;
    private final NotificationServiceClient notificationServiceClient;

    public BidResponse createBid(CreateBidRequest request, Long bidderId) {
        log.info("Creating bid for task {} by user {}", request.getTaskId(), bidderId);

        // Validate if user can bid on this task
        validateBidEligibility(request.getTaskId(), bidderId);

        // Check if user already has a bid for this task
        if (bidRepository.existsByTaskIdAndBidderId(request.getTaskId(), bidderId)) {
            throw new RuntimeException("You have already placed a bid for this task");
        }

        // Get task details to validate bidding window
        TaskDetailsResponse task = taskServiceClient.getTask(request.getTaskId());
        if (task.getBiddingDeadline().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Bidding deadline has passed for this task");
        }

        // Create the bid
        Bid bid = Bid.builder()
                .taskId(request.getTaskId())
                .bidderId(bidderId)
                .amount(request.getAmount())
                .message(request.getMessage())
                .estimatedDelivery(request.getEstimatedDelivery())
                .status(Bid.BidStatus.ACTIVE)
                .build();

        Bid savedBid = bidRepository.save(bid);

        // Send notification to task owner
        sendBidNotification(savedBid, task.getOwnerId());

        return mapToResponse(savedBid);
    }

    public BidResponse updateBid(Long bidId, UpdateBidRequest request, Long userId) {
        log.info("Updating bid {} by user {}", bidId, userId);

        Bid bid = bidRepository.findById(bidId)
                .orElseThrow(() -> new RuntimeException("Bid not found"));

        if (!bid.getBidderId().equals(userId)) {
            throw new RuntimeException("You can only update your own bids");
        }

        if (bid.getStatus() != Bid.BidStatus.ACTIVE) {
            throw new RuntimeException("Cannot update inactive bid");
        }

        // Check if bidding is still active
        TaskDetailsResponse task = taskServiceClient.getTask(bid.getTaskId());
        if (task.getBiddingDeadline().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Bidding deadline has passed");
        }

        // Update bid fields
        if (request.getAmount() != null) {
            bid.setAmount(request.getAmount());
        }
        if (request.getMessage() != null) {
            bid.setMessage(request.getMessage());
        }
        if (request.getEstimatedDelivery() != null) {
            bid.setEstimatedDelivery(request.getEstimatedDelivery());
        }

        Bid savedBid = bidRepository.save(bid);
        return mapToResponse(savedBid);
    }

    public void withdrawBid(Long bidId, Long userId) {
        log.info("Withdrawing bid {} by user {}", bidId, userId);

        Bid bid = bidRepository.findById(bidId)
                .orElseThrow(() -> new RuntimeException("Bid not found"));

        if (!bid.getBidderId().equals(userId)) {
            throw new RuntimeException("You can only withdraw your own bids");
        }

        if (bid.getStatus() != Bid.BidStatus.ACTIVE) {
            throw new RuntimeException("Cannot withdraw inactive bid");
        }

        bid.setStatus(Bid.BidStatus.WITHDRAWN);
        bidRepository.save(bid);
    }

    public List<BidResponse> getBidsForTask(Long taskId) {
        List<Bid> bids = bidRepository.findByTaskIdAndStatusOrderByAmountAsc(
                taskId, Bid.BidStatus.ACTIVE);
        return bids.stream().map(this::mapToResponse).toList();
    }

    public List<BidResponse> getBidsForUser(Long userId) {
        List<Bid> bids = bidRepository.findByBidderIdOrderByCreatedAtDesc(userId);
        return bids.stream().map(this::mapToResponse).toList();
    }

    public BidSummaryResponse getBidSummary(Long taskId) {
        List<Bid> activeBids = bidRepository.findByTaskIdAndStatusOrderByAmountAsc(
                taskId, Bid.BidStatus.ACTIVE);

        TaskDetailsResponse task = taskServiceClient.getTask(taskId);
        Optional<Bid> winningBid = bidRepository.findByTaskIdAndIsWinnerTrue(taskId);

        BigDecimal lowestBid = activeBids.isEmpty() ? null : activeBids.get(0).getAmount();
        BigDecimal highestBid = activeBids.isEmpty() ? null : 
                activeBids.get(activeBids.size() - 1).getAmount();
        
        double averageBid = activeBids.stream()
                .mapToDouble(bid -> bid.getAmount().doubleValue())
                .average()
                .orElse(0.0);

        return BidSummaryResponse.builder()
                .taskId(taskId)
                .totalBids(activeBids.size())
                .lowestBid(lowestBid)
                .highestBid(highestBid)
                .averageBid(BigDecimal.valueOf(averageBid))
                .biddingDeadline(task.getBiddingDeadline())
                .biddingActive(task.getBiddingDeadline().isAfter(LocalDateTime.now()))
                .winningBidId(winningBid.map(Bid::getId).orElse(null))
                .winnerUserId(winningBid.map(Bid::getBidderId).orElse(null))
                .build();
    }

    @Scheduled(fixedRate = 300000) // Run every 5 minutes
    public void processExpiredBiddings() {
        log.info("Processing expired biddings...");
        
        List<TaskDetailsResponse> expiredTasks = taskServiceClient.getTasksWithExpiredBidding();
        
        for (TaskDetailsResponse task : expiredTasks) {
            selectWinnerForTask(task.getId());
        }
    }

    @Async
    public void selectWinnerForTask(Long taskId) {
        log.info("Selecting winner for task {}", taskId);

        try {
            List<Bid> activeBids = bidRepository.findByTaskIdAndStatusOrderByAmountAsc(
                    taskId, Bid.BidStatus.ACTIVE);

            if (activeBids.isEmpty()) {
                log.warn("No active bids found for task {}", taskId);
                taskServiceClient.updateTaskStatus(taskId, "CANCELLED");
                return;
            }

            // Select the lowest bid as winner
            Bid winningBid = activeBids.get(0);
            winningBid.setIsWinner(true);
            winningBid.setStatus(Bid.BidStatus.WON);
            winningBid.setWinnerSelectedAt(LocalDateTime.now());
            bidRepository.save(winningBid);

            // Mark other bids as lost
            activeBids.stream()
                    .skip(1) // Skip the winning bid
                    .forEach(bid -> {
                        bid.setStatus(Bid.BidStatus.LOST);
                        bidRepository.save(bid);
                    });

            // Update task with winner
            TaskAssignmentRequest assignmentRequest = TaskAssignmentRequest.builder()
                    .assignedUserId(winningBid.getBidderId())
                    .finalPrice(winningBid.getAmount())
                    .build();
            
            taskServiceClient.assignTask(taskId, assignmentRequest);

            // Send notifications
            sendWinnerNotifications(winningBid, activeBids);

            log.info("Winner selected for task {}: User {} with bid amount {}", 
                    taskId, winningBid.getBidderId(), winningBid.getAmount());

        } catch (Exception e) {
            log.error("Error selecting winner for task {}: {}", taskId, e.getMessage(), e);
        }
    }

    private void validateBidEligibility(Long taskId, Long bidderId) {
        TaskDetailsResponse task = taskServiceClient.getTask(taskId);
        
        // Check if user is not the task owner
        if (task.getOwnerId().equals(bidderId)) {
            throw new RuntimeException("You cannot bid on your own task");
        }

        // Check task status
        if (!task.getStatus().equals("OPEN")) {
            throw new RuntimeException("Task is not open for bidding");
        }

        // Additional validations can be added here
    }

    private void sendBidNotification(Bid bid, Long taskOwnerId) {
        try {
            NotificationRequest notification = NotificationRequest.builder()
                    .userId(taskOwnerId)
                    .type("NEW_BID_RECEIVED")
                    .title("New Bid Received")
                    .message(String.format("You received a new bid of ₹%.2f for your task", 
                            bid.getAmount()))
                    .relatedEntityId(bid.getTaskId())
                    .relatedEntityType("TASK")
                    .build();
            
            notificationServiceClient.sendNotification(notification);
        } catch (Exception e) {
            log.error("Failed to send bid notification: {}", e.getMessage());
        }
    }

    private void sendWinnerNotifications(Bid winningBid, List<Bid> allBids) {
        try {
            // Notify winner
            NotificationRequest winnerNotification = NotificationRequest.builder()
                    .userId(winningBid.getBidderId())
                    .type("BID_WON")
                    .title("Congratulations! You Won the Bid")
                    .message(String.format("Your bid of ₹%.2f won the task. Get started!", 
                            winningBid.getAmount()))
                    .relatedEntityId(winningBid.getTaskId())
                    .relatedEntityType("TASK")
                    .build();
            
            notificationServiceClient.sendNotification(winnerNotification);

            // Notify losers
            allBids.stream()
                    .filter(bid -> bid.getStatus() == Bid.BidStatus.LOST)
                    .forEach(bid -> {
                        NotificationRequest loserNotification = NotificationRequest.builder()
                                .userId(bid.getBidderId())
                                .type("BID_LOST")
                                .title("Bid Result")
                                .message("Your bid was not selected. Keep trying!")
                                .relatedEntityId(bid.getTaskId())
                                .relatedEntityType("TASK")
                                .build();
                        
                        notificationServiceClient.sendNotification(loserNotification);
                    });

        } catch (Exception e) {
            log.error("Failed to send winner notifications: {}", e.getMessage());
        }
    }

    private BidResponse mapToResponse(Bid bid) {
        return BidResponse.builder()
                .id(bid.getId())
                .taskId(bid.getTaskId())
                .bidderId(bid.getBidderId())
                .amount(bid.getAmount())
                .message(bid.getMessage())
                .estimatedDelivery(bid.getEstimatedDelivery())
                .status(bid.getStatus())
                .isWinner(bid.isWinner())
                .winnerSelectedAt(bid.getWinnerSelectedAt())
                .createdAt(bid.getCreatedAt())
                .updatedAt(bid.getUpdatedAt())
                .build();
    }

    // Feign clients for inter-service communication
    @FeignClient(name = "task-service")
    public interface TaskServiceClient {
        @GetMapping("/tasks/{id}")
        TaskDetailsResponse getTask(@PathVariable Long id);

        @GetMapping("/tasks/expired-bidding")
        List<TaskDetailsResponse> getTasksWithExpiredBidding();

        @PostMapping("/tasks/{id}/assign")
        void assignTask(@PathVariable Long id, @RequestBody TaskAssignmentRequest request);

        @PostMapping("/tasks/{id}/status")
        void updateTaskStatus(@PathVariable Long id, @RequestBody String status);
    }

    @FeignClient(name = "notification-service")
    public interface NotificationServiceClient {
        @PostMapping("/notifications")
        void sendNotification(@RequestBody NotificationRequest request);
    }

    // DTOs for inter-service communication
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class TaskDetailsResponse {
        private Long id;
        private Long ownerId;
        private String title;
        private String status;
        private LocalDateTime biddingDeadline;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class TaskAssignmentRequest {
        private Long assignedUserId;
        private BigDecimal finalPrice;
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
}