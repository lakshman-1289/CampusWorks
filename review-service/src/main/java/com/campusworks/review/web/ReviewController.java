package com.campusworks.review.web;

import com.campusworks.review.domain.Review;
import com.campusworks.review.repo.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewRepository repository;

    @GetMapping
    public List<Review> all() { return repository.findAll(); }

    @GetMapping("/user/{userId}")
    public List<Review> forUser(@PathVariable Long userId) { return repository.findByRevieweeId(userId); }

    @GetMapping("/{id}")
    public ResponseEntity<Review> byId(@PathVariable Long id) {
        return repository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Review create(@RequestBody Review r) { return repository.save(r); }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!repository.existsById(id)) return ResponseEntity.notFound().build();
        repository.deleteById(id); return ResponseEntity.noContent().build();
    }
}


