package com.deepanshu.taskmanager.service;

import com.deepanshu.taskmanager.config.CacheConfig;
import com.deepanshu.taskmanager.dto.request.ProjectRequest;
import com.deepanshu.taskmanager.dto.response.ApiResponse;
import com.deepanshu.taskmanager.exception.ResourceNotFoundException;
import com.deepanshu.taskmanager.model.Project;
import com.deepanshu.taskmanager.model.User;
import com.deepanshu.taskmanager.repository.ProjectRepository;
import com.deepanshu.taskmanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_PROJECTS, allEntries = true)
    public ApiResponse.ProjectDetail create(ProjectRequest.Create request) {
        User currentUser = getCurrentUser();

        Project project = Project.builder()
            .name(request.getName())
            .description(request.getDescription())
            .dueDate(request.getDueDate())
            .owner(currentUser)
            .status(Project.Status.ACTIVE)
            .build();

        project = projectRepository.save(project);
        log.info("User '{}' created project '{}'", currentUser.getUsername(), project.getName());
        return ApiResponse.ProjectDetail.from(project);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.CACHE_PROJECTS, key = "#root.methodName + ':' + T(org.springframework.security.core.context.SecurityContextHolder).getContext().getAuthentication().getName() + ':' + #pageable.pageNumber")
    public Page<ApiResponse.ProjectDetail> getMyProjects(Pageable pageable) {
        User currentUser = getCurrentUser();
        return projectRepository.findByOwnerId(currentUser.getId(), pageable)
            .map(ApiResponse.ProjectDetail::from);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.CACHE_PROJECTS, key = "'status:' + #status + ':' + T(org.springframework.security.core.context.SecurityContextHolder).getContext().getAuthentication().getName() + ':' + #pageable.pageNumber")
    public Page<ApiResponse.ProjectDetail> getMyProjectsByStatus(Project.Status status, Pageable pageable) {
        User currentUser = getCurrentUser();
        return projectRepository.findByOwnerIdAndStatus(currentUser.getId(), status, pageable)
            .map(ApiResponse.ProjectDetail::from);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.CACHE_PROJECTS, key = "'id:' + #id")
    public ApiResponse.ProjectDetail getById(Long id) {
        User currentUser = getCurrentUser();
        Project project = findProjectOwnedByUser(id, currentUser.getId());
        return ApiResponse.ProjectDetail.from(project);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.CACHE_PROJECTS, key = "'id:' + #id"),
        @CacheEvict(value = CacheConfig.CACHE_PROJECTS, allEntries = true)
    })
    public ApiResponse.ProjectDetail update(Long id, ProjectRequest.Update request) {
        User currentUser = getCurrentUser();
        Project project = findProjectOwnedByUser(id, currentUser.getId());

        if (request.getName() != null) project.setName(request.getName());
        if (request.getDescription() != null) project.setDescription(request.getDescription());
        if (request.getStatus() != null) project.setStatus(request.getStatus());
        if (request.getDueDate() != null) project.setDueDate(request.getDueDate());

        project = projectRepository.save(project);
        log.info("User '{}' updated project '{}'", currentUser.getUsername(), project.getName());
        return ApiResponse.ProjectDetail.from(project);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.CACHE_PROJECTS, key = "'id:' + #id"),
        @CacheEvict(value = CacheConfig.CACHE_PROJECTS, allEntries = true),
        @CacheEvict(value = CacheConfig.CACHE_TASKS, allEntries = true)
    })
    public void delete(Long id) {
        User currentUser = getCurrentUser();
        Project project = findProjectOwnedByUser(id, currentUser.getId());
        projectRepository.delete(project);
        log.info("User '{}' deleted project '{}'", currentUser.getUsername(), project.getName());
    }

    private Project findProjectOwnedByUser(Long projectId, Long userId) {
        return projectRepository.findByIdAndOwnerId(projectId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
    }
}
