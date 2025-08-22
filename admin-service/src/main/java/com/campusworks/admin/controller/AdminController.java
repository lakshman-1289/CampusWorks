package com.campusworks.admin.controller;

import com.campusworks.admin.model.Dispute;
import com.campusworks.admin.repo.DisputeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {
    private final DisputeRepository repository;

    @GetMapping("/disputes")
    public List<Dispute> all() { return repository.findAll(); }

    @PostMapping("/disputes")
    public Dispute create(@RequestBody Dispute d) { d.setStatus("OPEN"); return repository.save(d); }

    @PostMapping("/disputes/{id}/resolve")
    public ResponseEntity<Dispute> resolve(@PathVariable Long id) {
        return repository.findById(id).map(ex -> { ex.setStatus("RESOLVED"); return ResponseEntity.ok(repository.save(ex)); })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/disputes/{id}/reject")
    public ResponseEntity<Dispute> reject(@PathVariable Long id) {
        return repository.findById(id).map(ex -> { ex.setStatus("REJECTED"); return ResponseEntity.ok(repository.save(ex)); })
                .orElse(ResponseEntity.notFound().build());
    }
}


