package com.deepanshu.taskmanager.repository;

import com.deepanshu.taskmanager.model.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    @EntityGraph(attributePaths = {"project", "assignee"})
    Page<Task> findByProjectId(Long projectId, Pageable pageable);

    @EntityGraph(attributePaths = {"project", "assignee"})
    Page<Task> findByProjectIdAndStatus(Long projectId, Task.Status status, Pageable pageable);

    @EntityGraph(attributePaths = {"project", "assignee"})
    Page<Task> findByAssigneeId(Long assigneeId, Pageable pageable);

    @EntityGraph(attributePaths = {"project", "assignee"})
    Optional<Task> findByIdAndProjectId(Long id, Long projectId);

    @Query("SELECT t FROM Task t WHERE t.project.id = :projectId AND t.priority = :priority")
    List<Task> findByProjectIdAndPriority(Long projectId, Task.Priority priority);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.project.id = :projectId AND t.status = 'DONE'")
    long countCompletedByProject(Long projectId);

    @Query("SELECT t FROM Task t WHERE t.assignee.id = :userId AND t.status NOT IN ('DONE', 'CANCELLED') ORDER BY t.priority DESC, t.dueDate ASC")
    List<Task> findActivetasksByAssignee(Long userId);
}
