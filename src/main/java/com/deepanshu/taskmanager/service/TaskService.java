package com.deepanshu.taskmanager.service;

import com.deepanshu.taskmanager.dto.request.TaskRequest;
import com.deepanshu.taskmanager.dto.response.ApiResponse;
import com.deepanshu.taskmanager.exception.ResourceNotFoundException;
import com.deepanshu.taskmanager.model.Project;
import com.deepanshu.taskmanager.model.Task;
import com.deepanshu.taskmanager.model.User;
import com.deepanshu.taskmanager.repository.ProjectRepository;
import com.deepanshu.taskmanager.repository.TaskRepository;
import com.deepanshu.taskmanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    @Transactional
    public ApiResponse.TaskDetail createTask(Long projectId, TaskRequest.Create request) {
        User currentUser = getCurrentUser();

        Project project = projectRepository.findByIdAndOwnerId(projectId, currentUser.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        User assignee = null;
        if (request.getAssigneeId() != null) {
            assignee = userRepository.findById(request.getAssigneeId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getAssigneeId()));
        }

        Task task = Task.builder()
            .title(request.getTitle())
            .description(request.getDescription())
            .priority(request.getPriority() != null ? request.getPriority() : Task.Priority.MEDIUM)
            .dueDate(request.getDueDate())
            .estimatedHours(request.getEstimatedHours())
            .project(project)
            .assignee(assignee)
            .status(Task.Status.TODO)
            .build();

        task = taskRepository.save(task);
        log.info("Task '{}' created in project '{}'", task.getTitle(), project.getName());
        return ApiResponse.TaskDetail.from(task);
    }

    @Transactional(readOnly = true)
    public Page<ApiResponse.TaskDetail> getTasksByProject(Long projectId, Pageable pageable) {
        return taskRepository.findByProjectId(projectId, pageable)
            .map(ApiResponse.TaskDetail::from);
    }

    @Transactional(readOnly = true)
    public Page<ApiResponse.TaskDetail> getTasksByProjectAndStatus(Long projectId, Task.Status status, Pageable pageable) {
        return taskRepository.findByProjectIdAndStatus(projectId, status, pageable)
            .map(ApiResponse.TaskDetail::from);
    }

    @Transactional(readOnly = true)
    public ApiResponse.TaskDetail getTask(Long projectId, Long taskId) {
        Task task = taskRepository.findByIdAndProjectId(taskId, projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));
        return ApiResponse.TaskDetail.from(task);
    }

    @Transactional
    public ApiResponse.TaskDetail updateTask(Long projectId, Long taskId, TaskRequest.Update request) {
        Task task = taskRepository.findByIdAndProjectId(taskId, projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));

        if (request.getTitle() != null) task.setTitle(request.getTitle());
        if (request.getDescription() != null) task.setDescription(request.getDescription());
        if (request.getStatus() != null) task.setStatus(request.getStatus());
        if (request.getPriority() != null) task.setPriority(request.getPriority());
        if (request.getDueDate() != null) task.setDueDate(request.getDueDate());
        if (request.getEstimatedHours() != null) task.setEstimatedHours(request.getEstimatedHours());

        if (request.getAssigneeId() != null) {
            User assignee = userRepository.findById(request.getAssigneeId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getAssigneeId()));
            task.setAssignee(assignee);
        }

        task = taskRepository.save(task);
        return ApiResponse.TaskDetail.from(task);
    }

    @Transactional
    public ApiResponse.TaskDetail patchStatus(Long projectId, Long taskId, TaskRequest.StatusUpdate request) {
        Task task = taskRepository.findByIdAndProjectId(taskId, projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));
        task.setStatus(request.getStatus());
        task = taskRepository.save(task);
        log.info("Task '{}' status updated to {}", task.getTitle(), task.getStatus());
        return ApiResponse.TaskDetail.from(task);
    }

    @Transactional
    public void deleteTask(Long projectId, Long taskId) {
        Task task = taskRepository.findByIdAndProjectId(taskId, projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));
        taskRepository.delete(task);
        log.info("Task '{}' deleted", task.getTitle());
    }

    @Transactional(readOnly = true)
    public List<ApiResponse.TaskDetail> getMyActiveTasks() {
        User currentUser = getCurrentUser();
        return taskRepository.findActivetasksByAssignee(currentUser.getId())
            .stream()
            .map(ApiResponse.TaskDetail::from)
            .collect(Collectors.toList());
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
    }
}
