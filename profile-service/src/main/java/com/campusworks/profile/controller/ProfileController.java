package com.campusworks.profile.controller;

import com.campusworks.profile.model.StudentProfile;
import com.campusworks.profile.repo.StudentProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/profiles")
@RequiredArgsConstructor
public class ProfileController {
    private final StudentProfileRepository repository;

    @GetMapping
    public List<StudentProfile> all() { return repository.findAll(); }

    @GetMapping("/{id}")
    public ResponseEntity<StudentProfile> byId(@PathVariable Long id) {
        return repository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public StudentProfile create(@RequestBody StudentProfile profile) { return repository.save(profile); }

    @PutMapping("/{id}")
    public ResponseEntity<StudentProfile> update(@PathVariable Long id, @RequestBody StudentProfile p) {
        return repository.findById(id).map(ex -> {
            ex.setFullName(p.getFullName());
            ex.setDepartment(p.getDepartment());
            ex.setBio(p.getBio());
            return ResponseEntity.ok(repository.save(ex));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!repository.existsById(id)) return ResponseEntity.notFound().build();
        repository.deleteById(id); return ResponseEntity.noContent().build();
    }
}


