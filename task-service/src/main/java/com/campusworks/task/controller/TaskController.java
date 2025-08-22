package com.campusworks.task.controller;

import com.campusworks.task.model.Task;
import com.campusworks.task.repo.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class TaskController {
    private final TaskRepository repository;

    @GetMapping
    public List<Task> all() { return repository.findAll(); }

    @GetMapping("/{id}")
    public ResponseEntity<Task> byId(@PathVariable Long id) {
        return repository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Task create(@RequestBody Task task) {
        task.setStatus("OPEN");
        return repository.save(task);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Task> update(@PathVariable Long id, @RequestBody Task t) {
        return repository.findById(id).map(ex -> {
            ex.setTitle(t.getTitle());
            ex.setDescription(t.getDescription());
            ex.setPrice(t.getPrice());
            ex.setStatus(t.getStatus());
            return ResponseEntity.ok(repository.save(ex));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!repository.existsById(id)) return ResponseEntity.notFound().build();
        repository.deleteById(id); return ResponseEntity.noContent().build();
    }
}


