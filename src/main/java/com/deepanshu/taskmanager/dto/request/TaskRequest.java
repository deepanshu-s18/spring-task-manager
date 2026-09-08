package com.deepanshu.taskmanager.dto.request;

import com.deepanshu.taskmanager.model.Task;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

public class TaskRequest {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Create {
        @NotBlank(message = "Task title is required")
        @Size(min = 3, max = 200, message = "Title must be 3-200 characters")
        private String title;

        @Size(max = 1000, message = "Description must be at most 1000 characters")
        private String description;

        private Task.Priority priority = Task.Priority.MEDIUM;

        @Future(message = "Due date must be in the future")
        private LocalDateTime dueDate;

        private Long assigneeId;

        @Min(value = 1, message = "Estimated hours must be at least 1")
        @Max(value = 1000, message = "Estimated hours cannot exceed 1000")
        private Integer estimatedHours;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Update {
        @Size(min = 3, max = 200)
        private String title;

        @Size(max = 1000)
        private String description;

        private Task.Status status;
        private Task.Priority priority;
        private LocalDateTime dueDate;
        private Long assigneeId;
        private Integer estimatedHours;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusUpdate {
        @NotNull(message = "Status is required")
        private Task.Status status;
    }
}
