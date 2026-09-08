package com.deepanshu.taskmanager.dto.response;

import com.deepanshu.taskmanager.model.Project;
import com.deepanshu.taskmanager.model.Task;
import com.deepanshu.taskmanager.model.User;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Aggregated response DTOs — avoid exposing JPA entities directly.
 * All response objects are immutable records or @Value classes.
 */
public class ApiResponse {

    @Value
    @Builder
    public static class AuthTokens {
        String accessToken;
        String refreshToken;
        String tokenType = "Bearer";
        Long expiresIn;
        UserSummary user;
    }

    @Value
    @Builder
    public static class UserSummary {
        Long id;
        String username;
        String email;
        String fullName;
        User.Role role;
        LocalDateTime createdAt;

        public static UserSummary from(User user) {
            return UserSummary.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
        }
    }

    @Value
    @Builder
    public static class ProjectDetail {
        Long id;
        String name;
        String description;
        Project.Status status;
        UserSummary owner;
        int totalTasks;
        long completedTasks;
        int progressPercent;
        LocalDateTime dueDate;
        LocalDateTime createdAt;
        LocalDateTime updatedAt;

        public static ProjectDetail from(Project project) {
            return ProjectDetail.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .status(project.getStatus())
                .owner(UserSummary.from(project.getOwner()))
                .totalTasks(project.getTasks().size())
                .completedTasks(project.getCompletedTaskCount())
                .progressPercent(project.getProgress())
                .dueDate(project.getDueDate())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
        }
    }

    @Value
    @Builder
    public static class TaskDetail {
        Long id;
        String title;
        String description;
        Task.Status status;
        Task.Priority priority;
        Long projectId;
        String projectName;
        UserSummary assignee;
        LocalDateTime dueDate;
        LocalDateTime completedAt;
        Integer estimatedHours;
        LocalDateTime createdAt;
        LocalDateTime updatedAt;

        public static TaskDetail from(Task task) {
            return TaskDetail.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .priority(task.getPriority())
                .projectId(task.getProject().getId())
                .projectName(task.getProject().getName())
                .assignee(task.getAssignee() != null ? UserSummary.from(task.getAssignee()) : null)
                .dueDate(task.getDueDate())
                .completedAt(task.getCompletedAt())
                .estimatedHours(task.getEstimatedHours())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
        }
    }

    @Value
    @Builder
    public static class ErrorResponse {
        int status;
        String error;
        String message;
        String path;
        LocalDateTime timestamp;
    }

    @Value
    @Builder
    public static class MessageResponse {
        String message;
    }
}
