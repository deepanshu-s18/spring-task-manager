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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskService Unit Tests")
class TaskServiceTest {

    @Mock private TaskRepository taskRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private TaskService taskService;

    private User currentUser;
    private User assigneeUser;
    private Project sampleProject;
    private Task sampleTask;

    @BeforeEach
    void setUp() {
        currentUser = User.builder()
            .id(1L)
            .username("deepanshu")
            .email("deepanshuk2555@gmail.com")
            .role(User.Role.USER)
            .build();

        assigneeUser = User.builder()
            .id(2L)
            .username("assignee")
            .email("assignee@example.com")
            .role(User.Role.USER)
            .build();

        sampleProject = Project.builder()
            .id(1L)
            .name("Test Project")
            .description("A test project")
            .status(Project.Status.ACTIVE)
            .owner(currentUser)
            .build();

        sampleTask = Task.builder()
            .id(10L)
            .title("Implement Feature A")
            .description("Details of Feature A")
            .status(Task.Status.TODO)
            .priority(Task.Priority.HIGH)
            .project(sampleProject)
            .assignee(currentUser)
            .estimatedHours(5)
            .createdAt(LocalDateTime.now())
            .build();
    }

    private void mockSecurityContext(String username) {
        Authentication auth = new UsernamePasswordAuthenticationToken(username, null, List.of());
        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);
    }

    @Test
    @DisplayName("createTask - successfully creates task without assignee")
    void createTask_Success() {
        mockSecurityContext("deepanshu");
        when(userRepository.findByUsername("deepanshu")).thenReturn(Optional.of(currentUser));
        when(projectRepository.findByIdAndOwnerId(1L, 1L)).thenReturn(Optional.of(sampleProject));
        when(taskRepository.save(any(Task.class))).thenReturn(sampleTask);

        TaskRequest.Create request = TaskRequest.Create.builder()
            .title("Implement Feature A")
            .description("Details of Feature A")
            .priority(Task.Priority.HIGH)
            .estimatedHours(5)
            .build();

        ApiResponse.TaskDetail response = taskService.createTask(1L, request);

        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Implement Feature A");
        verify(taskRepository, times(1)).save(any(Task.class));
    }

    @Test
    @DisplayName("createTask - successfully creates task with assignee")
    void createTask_WithAssignee_Success() {
        mockSecurityContext("deepanshu");
        when(userRepository.findByUsername("deepanshu")).thenReturn(Optional.of(currentUser));
        when(projectRepository.findByIdAndOwnerId(1L, 1L)).thenReturn(Optional.of(sampleProject));
        when(userRepository.findById(2L)).thenReturn(Optional.of(assigneeUser));

        Task taskWithAssignee = Task.builder()
            .id(11L)
            .title("Task with assignee")
            .project(sampleProject)
            .assignee(assigneeUser)
            .status(Task.Status.TODO)
            .priority(Task.Priority.MEDIUM)
            .createdAt(LocalDateTime.now())
            .build();

        when(taskRepository.save(any(Task.class))).thenReturn(taskWithAssignee);

        TaskRequest.Create request = TaskRequest.Create.builder()
            .title("Task with assignee")
            .assigneeId(2L)
            .build();

        ApiResponse.TaskDetail response = taskService.createTask(1L, request);

        assertThat(response).isNotNull();
        assertThat(response.getAssignee()).isNotNull();
        assertThat(response.getAssignee().getId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("createTask - throws ResourceNotFoundException when project not found")
    void createTask_ProjectNotFound_ThrowsResourceNotFoundException() {
        mockSecurityContext("deepanshu");
        when(userRepository.findByUsername("deepanshu")).thenReturn(Optional.of(currentUser));
        when(projectRepository.findByIdAndOwnerId(99L, 1L)).thenReturn(Optional.empty());

        TaskRequest.Create request = TaskRequest.Create.builder()
            .title("Some task")
            .build();

        assertThatThrownBy(() -> taskService.createTask(99L, request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Project");
    }

    @Test
    @DisplayName("createTask - throws ResourceNotFoundException when assignee not found")
    void createTask_AssigneeNotFound_ThrowsResourceNotFoundException() {
        mockSecurityContext("deepanshu");
        when(userRepository.findByUsername("deepanshu")).thenReturn(Optional.of(currentUser));
        when(projectRepository.findByIdAndOwnerId(1L, 1L)).thenReturn(Optional.of(sampleProject));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        TaskRequest.Create request = TaskRequest.Create.builder()
            .title("Task with missing assignee")
            .assigneeId(999L)
            .build();

        assertThatThrownBy(() -> taskService.createTask(1L, request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("User");
    }

    @Test
    @DisplayName("getTasksByProject - returns paginated tasks for project")
    void getTasksByProject_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Task> pagedResult = new PageImpl<>(List.of(sampleTask), pageable, 1);
        when(taskRepository.findByProjectId(1L, pageable)).thenReturn(pagedResult);

        Page<ApiResponse.TaskDetail> result = taskService.getTasksByProject(1L, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Implement Feature A");
    }

    @Test
    @DisplayName("getTasksByProjectAndStatus - returns filtered tasks")
    void getTasksByProjectAndStatus_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Task> pagedResult = new PageImpl<>(List.of(sampleTask), pageable, 1);
        when(taskRepository.findByProjectIdAndStatus(1L, Task.Status.TODO, pageable)).thenReturn(pagedResult);

        Page<ApiResponse.TaskDetail> result = taskService.getTasksByProjectAndStatus(1L, Task.Status.TODO, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(Task.Status.TODO);
    }

    @Test
    @DisplayName("getTask - returns task detail successfully")
    void getTask_Success() {
        when(taskRepository.findByIdAndProjectId(10L, 1L)).thenReturn(Optional.of(sampleTask));

        ApiResponse.TaskDetail result = taskService.getTask(1L, 10L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("getTask - throws ResourceNotFoundException when task not found")
    void getTask_NotFound_ThrowsResourceNotFoundException() {
        when(taskRepository.findByIdAndProjectId(999L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getTask(1L, 999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Task");
    }

    @Test
    @DisplayName("updateTask - updates fields successfully")
    void updateTask_Success() {
        when(taskRepository.findByIdAndProjectId(10L, 1L)).thenReturn(Optional.of(sampleTask));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskRequest.Update request = TaskRequest.Update.builder()
            .title("Updated Title")
            .description("Updated Desc")
            .status(Task.Status.IN_PROGRESS)
            .priority(Task.Priority.CRITICAL)
            .estimatedHours(8)
            .build();

        ApiResponse.TaskDetail result = taskService.updateTask(1L, 10L, request);

        assertThat(result.getTitle()).isEqualTo("Updated Title");
        assertThat(result.getStatus()).isEqualTo(Task.Status.IN_PROGRESS);
        assertThat(result.getPriority()).isEqualTo(Task.Priority.CRITICAL);
        assertThat(result.getEstimatedHours()).isEqualTo(8);
    }

    @Test
    @DisplayName("updateTask - updates assignee successfully")
    void updateTask_WithAssignee_Success() {
        when(taskRepository.findByIdAndProjectId(10L, 1L)).thenReturn(Optional.of(sampleTask));
        when(userRepository.findById(2L)).thenReturn(Optional.of(assigneeUser));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskRequest.Update request = TaskRequest.Update.builder()
            .assigneeId(2L)
            .build();

        ApiResponse.TaskDetail result = taskService.updateTask(1L, 10L, request);

        assertThat(result.getAssignee()).isNotNull();
        assertThat(result.getAssignee().getId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("updateTask - throws ResourceNotFoundException when task not found")
    void updateTask_TaskNotFound_ThrowsResourceNotFoundException() {
        when(taskRepository.findByIdAndProjectId(999L, 1L)).thenReturn(Optional.empty());

        TaskRequest.Update request = TaskRequest.Update.builder().title("New Title").build();

        assertThatThrownBy(() -> taskService.updateTask(1L, 999L, request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Task");
    }

    @Test
    @DisplayName("updateTask - throws ResourceNotFoundException when assignee not found")
    void updateTask_AssigneeNotFound_ThrowsResourceNotFoundException() {
        when(taskRepository.findByIdAndProjectId(10L, 1L)).thenReturn(Optional.of(sampleTask));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        TaskRequest.Update request = TaskRequest.Update.builder().assigneeId(999L).build();

        assertThatThrownBy(() -> taskService.updateTask(1L, 10L, request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("User");
    }

    @Test
    @DisplayName("patchStatus - updates status only")
    void patchStatus_Success() {
        when(taskRepository.findByIdAndProjectId(10L, 1L)).thenReturn(Optional.of(sampleTask));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskRequest.StatusUpdate request = TaskRequest.StatusUpdate.builder()
            .status(Task.Status.DONE)
            .build();

        ApiResponse.TaskDetail result = taskService.patchStatus(1L, 10L, request);

        assertThat(result.getStatus()).isEqualTo(Task.Status.DONE);
    }

    @Test
    @DisplayName("patchStatus - throws ResourceNotFoundException when task not found")
    void patchStatus_TaskNotFound_ThrowsResourceNotFoundException() {
        when(taskRepository.findByIdAndProjectId(999L, 1L)).thenReturn(Optional.empty());

        TaskRequest.StatusUpdate request = TaskRequest.StatusUpdate.builder()
            .status(Task.Status.DONE)
            .build();

        assertThatThrownBy(() -> taskService.patchStatus(1L, 999L, request))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("deleteTask - successfully deletes task")
    void deleteTask_Success() {
        when(taskRepository.findByIdAndProjectId(10L, 1L)).thenReturn(Optional.of(sampleTask));

        taskService.deleteTask(1L, 10L);

        verify(taskRepository, times(1)).delete(sampleTask);
    }

    @Test
    @DisplayName("deleteTask - throws ResourceNotFoundException when task not found")
    void deleteTask_TaskNotFound_ThrowsResourceNotFoundException() {
        when(taskRepository.findByIdAndProjectId(999L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.deleteTask(1L, 999L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getMyActiveTasks - returns active assigned tasks for current user")
    void getMyActiveTasks_Success() {
        mockSecurityContext("deepanshu");
        when(userRepository.findByUsername("deepanshu")).thenReturn(Optional.of(currentUser));
        when(taskRepository.findActivetasksByAssignee(1L)).thenReturn(List.of(sampleTask));

        List<ApiResponse.TaskDetail> results = taskService.getMyActiveTasks();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo(10L);
    }
}
