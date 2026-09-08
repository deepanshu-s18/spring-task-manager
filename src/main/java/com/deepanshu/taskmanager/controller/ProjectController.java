package com.deepanshu.taskmanager.controller;

import com.deepanshu.taskmanager.dto.request.ProjectRequest;
import com.deepanshu.taskmanager.dto.response.ApiResponse;
import com.deepanshu.taskmanager.model.Project;
import com.deepanshu.taskmanager.service.ProjectService;
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

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Projects", description = "Project management endpoints")
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    @Operation(summary = "Create a new project")
    public ResponseEntity<ApiResponse.ProjectDetail> create(
            @Valid @RequestBody ProjectRequest.Create request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(projectService.create(request));
    }

    @GetMapping
    @Operation(summary = "Get all my projects (paginated)")
    public ResponseEntity<Page<ApiResponse.ProjectDetail>> getMyProjects(
            @Parameter(description = "Filter by status") @RequestParam(required = false) Project.Status status,
            @ParameterObject @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        if (status != null) {
            return ResponseEntity.ok(projectService.getMyProjectsByStatus(status, pageable));
        }
        return ResponseEntity.ok(projectService.getMyProjects(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get project by ID")
    public ResponseEntity<ApiResponse.ProjectDetail> getById(@PathVariable Long id) {
        return ResponseEntity.ok(projectService.getById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update project (full update)")
    public ResponseEntity<ApiResponse.ProjectDetail> update(
            @PathVariable Long id,
            @Valid @RequestBody ProjectRequest.Update request) {
        return ResponseEntity.ok(projectService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete project and all its tasks")
    public ResponseEntity<ApiResponse.MessageResponse> delete(@PathVariable Long id) {
        projectService.delete(id);
        return ResponseEntity.ok(new ApiResponse.MessageResponse("Project deleted successfully"));
    }
}
