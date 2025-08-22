package com.campusworks.bidding.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.campusworks.bidding.model.Bid;

import java.util.List;

public interface BidRepository extends JpaRepository<Bid, Long> {
    List<Bid> findByTaskIdOrderByAmountAsc(Long taskId);
}


