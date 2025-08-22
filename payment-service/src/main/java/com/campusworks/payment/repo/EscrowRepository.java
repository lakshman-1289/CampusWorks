package com.campusworks.payment.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.campusworks.payment.model.Escrow;

import java.util.Optional;

public interface EscrowRepository extends JpaRepository<Escrow, Long> {
    Optional<Escrow> findByTaskId(Long taskId);
}


