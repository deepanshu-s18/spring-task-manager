package com.deepanshu.taskmanager.dto.request;

import com.deepanshu.taskmanager.model.Project;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

public class ProjectRequest {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Create {
        @NotBlank(message = "Project name is required")
        @Size(min = 3, max = 100, message = "Project name must be 3-100 characters")
        private String name;

        @Size(max = 500, message = "Description must be at most 500 characters")
        private String description;

        @Future(message = "Due date must be in the future")
        private LocalDateTime dueDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Update {
        @Size(min = 3, max = 100, message = "Project name must be 3-100 characters")
        private String name;

        @Size(max = 500, message = "Description must be at most 500 characters")
        private String description;

        private Project.Status status;

        private LocalDateTime dueDate;
    }
}
