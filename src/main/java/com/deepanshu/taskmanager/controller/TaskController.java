package com.deepanshu.taskmanager.controller;

import com.deepanshu.taskmanager.dto.request.TaskRequest;
import com.deepanshu.taskmanager.dto.response.ApiResponse;
import com.deepanshu.taskmanager.model.Task;
import com.deepanshu.taskmanager.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/tasks")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Tasks", description = "Task management within a project")
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    @Operation(summary = "Create a new task in a project")
    public ResponseEntity<ApiResponse.TaskDetail> create(
            @PathVariable Long projectId,
            @Valid @RequestBody TaskRequest.Create request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(taskService.createTask(projectId, request));
    }

    @GetMapping
    @Operation(summary = "Get all tasks in a project (paginated, filterable by status)")
    public ResponseEntity<Page<ApiResponse.TaskDetail>> getTasks(
            @PathVariable Long projectId,
            @Parameter(description = "Filter by task status") @RequestParam(required = false) Task.Status status,
            @ParameterObject @PageableDefault(size = 20, sort = "priority") Pageable pageable) {
        if (status != null) {
            return ResponseEntity.ok(taskService.getTasksByProjectAndStatus(projectId, status, pageable));
        }
        return ResponseEntity.ok(taskService.getTasksByProject(projectId, pageable));
    }

    @GetMapping("/{taskId}")
    @Operation(summary = "Get task by ID")
    public ResponseEntity<ApiResponse.TaskDetail> getTask(
            @PathVariable Long projectId,
            @PathVariable Long taskId) {
        return ResponseEntity.ok(taskService.getTask(projectId, taskId));
    }

    @PutMapping("/{taskId}")
    @Operation(summary = "Update task (full update)")
    public ResponseEntity<ApiResponse.TaskDetail> update(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @Valid @RequestBody TaskRequest.Update request) {
        return ResponseEntity.ok(taskService.updateTask(projectId, taskId, request));
    }

    @PatchMapping("/{taskId}/status")
    @Operation(summary = "Update task status only (lightweight patch)")
    public ResponseEntity<ApiResponse.TaskDetail> patchStatus(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @Valid @RequestBody TaskRequest.StatusUpdate request) {
        return ResponseEntity.ok(taskService.patchStatus(projectId, taskId, request));
    }

    @DeleteMapping("/{taskId}")
    @Operation(summary = "Delete a task")
    public ResponseEntity<ApiResponse.MessageResponse> delete(
            @PathVariable Long projectId,
            @PathVariable Long taskId) {
        taskService.deleteTask(projectId, taskId);
        return ResponseEntity.ok(new ApiResponse.MessageResponse("Task deleted successfully"));
    }
}
