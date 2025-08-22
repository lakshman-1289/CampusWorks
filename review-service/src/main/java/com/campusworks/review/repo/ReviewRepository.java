package com.campusworks.review.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.campusworks.review.model.Review;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByRevieweeId(Long revieweeId);
}


