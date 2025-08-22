package com.campusworks.profile.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.campusworks.profile.model.StudentProfile;

import java.util.Optional;

public interface StudentProfileRepository extends JpaRepository<StudentProfile, Long> {
    Optional<StudentProfile> findByUserId(Long userId);
}


