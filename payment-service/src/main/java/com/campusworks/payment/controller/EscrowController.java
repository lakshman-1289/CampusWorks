package com.campusworks.payment.controller;

import com.campusworks.payment.model.Escrow;
import com.campusworks.payment.repo.EscrowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/escrows")
@RequiredArgsConstructor
public class EscrowController {
    private final EscrowRepository repository;

    @GetMapping
    public List<Escrow> all() { return repository.findAll(); }

    @GetMapping("/{id}")
    public ResponseEntity<Escrow> byId(@PathVariable Long id) {
        return repository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Escrow hold(@RequestBody Escrow e) { e.setStatus("HELD"); return repository.save(e); }

    @PostMapping("/{id}/release")
    public ResponseEntity<Escrow> release(@PathVariable Long id) {
        return repository.findById(id).map(ex -> {
            ex.setStatus("RELEASED");
            return ResponseEntity.ok(repository.save(ex));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/refund")
    public ResponseEntity<Escrow> refund(@PathVariable Long id) {
        return repository.findById(id).map(ex -> {
            ex.setStatus("REFUNDED");
            return ResponseEntity.ok(repository.save(ex));
        }).orElse(ResponseEntity.notFound().build());
    }
}


