package com.campusworks.bidding.repo;

import com.campusworks.bidding.domain.Bid;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BidRepository extends JpaRepository<Bid, Long> {
    List<Bid> findByTaskIdOrderByAmountAsc(Long taskId);
}


