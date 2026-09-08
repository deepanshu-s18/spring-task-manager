package com.deepanshu.taskmanager.repository;

import com.deepanshu.taskmanager.model.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    // Eagerly fetch owner + tasks to avoid N+1 queries
    @EntityGraph(attributePaths = {"owner", "tasks"})
    Page<Project> findByOwnerId(Long ownerId, Pageable pageable);

    @EntityGraph(attributePaths = {"owner", "tasks"})
    Optional<Project> findByIdAndOwnerId(Long id, Long ownerId);

    @EntityGraph(attributePaths = {"owner", "tasks"})
    Page<Project> findByOwnerIdAndStatus(Long ownerId, Project.Status status, Pageable pageable);

    boolean existsByIdAndOwnerId(Long id, Long ownerId);

    @Query("SELECT COUNT(p) FROM Project p WHERE p.owner.id = :ownerId AND p.status = 'ACTIVE'")
    long countActiveProjectsByOwner(Long ownerId);
}
