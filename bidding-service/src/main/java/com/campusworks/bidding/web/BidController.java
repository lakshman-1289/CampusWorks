package com.campusworks.bidding.web;

import com.campusworks.bidding.dto.BiddingDTOs.*;
import com.campusworks.bidding.service.BiddingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/bids")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class BidController {
    
    private final BiddingService biddingService;

    @PostMapping
    public ResponseEntity<BidResponse> createBid(
            @Valid @RequestBody CreateBidRequest request,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        BidResponse response = biddingService.createBid(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{bidId}")
    public ResponseEntity<BidResponse> updateBid(
            @PathVariable Long bidId,
            @Valid @RequestBody UpdateBidRequest request,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        BidResponse response = biddingService.updateBid(bidId, request, userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{bidId}")
    public ResponseEntity<Void> withdrawBid(
            @PathVariable Long bidId,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        biddingService.withdrawBid(bidId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/task/{taskId}")
    public ResponseEntity<List<BidResponse>> getBidsForTask(@PathVariable Long taskId) {
        List<BidResponse> bids = biddingService.getBidsForTask(taskId);
        return ResponseEntity.ok(bids);
    }

    @GetMapping("/my-bids")
    public ResponseEntity<List<BidResponse>> getMyBids(Authentication authentication) {
        Long userId = extractUserId(authentication);
        List<BidResponse> bids = biddingService.getBidsForUser(userId);
        return ResponseEntity.ok(bids);
    }

    @GetMapping("/task/{taskId}/summary")
    public ResponseEntity<BidSummaryResponse> getBidSummary(@PathVariable Long taskId) {
        BidSummaryResponse summary = biddingService.getBidSummary(taskId);
        return ResponseEntity.ok(summary);
    }

    @PostMapping("/task/{taskId}/select-winner")
    public ResponseEntity<Void> selectWinner(
            @PathVariable Long taskId,
            Authentication authentication) {
        // This endpoint can be used for manual winner selection if needed
        biddingService.selectWinnerForTask(taskId);
        return ResponseEntity.ok().build();
    }

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

    private Long extractUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new RuntimeException("User not authenticated");
        }
        return Long.valueOf(authentication.getName());
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ErrorResponse {
        private String message;
        private java.time.LocalDateTime timestamp;
    }
}


