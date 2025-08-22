package com.campusworks.bidding.web;

import com.campusworks.bidding.domain.Bid;
import com.campusworks.bidding.repo.BidRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bids")
@RequiredArgsConstructor
public class BidController {
    private final BidRepository repository;

    @GetMapping
    public List<Bid> all() { return repository.findAll(); }

    @GetMapping("/task/{taskId}")
    public List<Bid> byTask(@PathVariable Long taskId) { return repository.findByTaskIdOrderByAmountAsc(taskId); }

    @GetMapping("/{id}")
    public ResponseEntity<Bid> byId(@PathVariable Long id) {
        return repository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Bid create(@RequestBody Bid bid) { return repository.save(bid); }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!repository.existsById(id)) return ResponseEntity.notFound().build();
        repository.deleteById(id); return ResponseEntity.noContent().build();
    }
}


