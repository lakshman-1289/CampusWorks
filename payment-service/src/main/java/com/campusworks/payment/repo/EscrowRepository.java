package com.campusworks.payment.repo;

import com.campusworks.payment.domain.Escrow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EscrowRepository extends JpaRepository<Escrow, Long> {
    Optional<Escrow> findByTaskId(Long taskId);
}


