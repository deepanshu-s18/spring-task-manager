package com.deepanshu.taskmanager.controller;

import com.deepanshu.taskmanager.dto.response.ApiResponse;
import com.deepanshu.taskmanager.exception.ResourceNotFoundException;
import com.deepanshu.taskmanager.model.User;
import com.deepanshu.taskmanager.repository.UserRepository;
import com.deepanshu.taskmanager.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Users", description = "User management (admin) and profile endpoints")
public class UserController {

    private final UserRepository userRepository;
    private final TaskService taskService;

    @GetMapping("/me")
    @Operation(summary = "Get current user's profile")
    public ResponseEntity<ApiResponse.UserSummary> getProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername())
            .orElseThrow(() -> new ResourceNotFoundException("User", "username", userDetails.getUsername()));
        return ResponseEntity.ok(ApiResponse.UserSummary.from(user));
    }

    @GetMapping("/me/tasks")
    @Operation(summary = "Get current user's active assigned tasks")
    public ResponseEntity<List<ApiResponse.TaskDetail>> getMyActiveTasks() {
        return ResponseEntity.ok(taskService.getMyActiveTasks());
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all users (ADMIN only)")
    public ResponseEntity<Page<ApiResponse.UserSummary>> getAllUsers(
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(userRepository.findAll(pageable)
            .map(ApiResponse.UserSummary::from));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get user by ID (ADMIN only)")
    public ResponseEntity<ApiResponse.UserSummary> getById(@PathVariable Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return ResponseEntity.ok(ApiResponse.UserSummary.from(user));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deactivate a user account (ADMIN only)")
    public ResponseEntity<ApiResponse.MessageResponse> deactivate(@PathVariable Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        user.setIsActive(false);
        userRepository.save(user);
        return ResponseEntity.ok(new ApiResponse.MessageResponse("User '" + user.getUsername() + "' deactivated"));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reactivate a user account (ADMIN only)")
    public ResponseEntity<ApiResponse.MessageResponse> activate(@PathVariable Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        user.setIsActive(true);
        userRepository.save(user);
        return ResponseEntity.ok(new ApiResponse.MessageResponse("User '" + user.getUsername() + "' activated"));
    }
}
