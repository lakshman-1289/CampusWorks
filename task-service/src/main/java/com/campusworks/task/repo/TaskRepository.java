package com.campusworks.task.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.campusworks.task.model.Task;

public interface TaskRepository extends JpaRepository<Task, Long> {
}


