package com.campusworks.admin.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.campusworks.admin.model.Dispute;

public interface DisputeRepository extends JpaRepository<Dispute, Long> {
}


