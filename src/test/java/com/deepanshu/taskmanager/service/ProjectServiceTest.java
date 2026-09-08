package com.deepanshu.taskmanager.service;

import com.deepanshu.taskmanager.dto.request.ProjectRequest;
import com.deepanshu.taskmanager.dto.response.ApiResponse;
import com.deepanshu.taskmanager.exception.ResourceNotFoundException;
import com.deepanshu.taskmanager.model.Project;
import com.deepanshu.taskmanager.model.User;
import com.deepanshu.taskmanager.repository.ProjectRepository;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectService Unit Tests")
class ProjectServiceTest {

    @Mock private ProjectRepository projectRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private ProjectService projectService;

    private User currentUser;
    private Project sampleProject;

    @BeforeEach
    void setUp() {
        currentUser = User.builder()
            .id(1L)
            .username("deepanshu")
            .email("deepanshuk2555@gmail.com")
            .role(User.Role.USER)
            .build();

        sampleProject = Project.builder()
            .id(1L)
            .name("Test Project")
            .description("A test project")
            .status(Project.Status.ACTIVE)
            .owner(currentUser)
            .tasks(new ArrayList<>())
            .build();

        // Set up SecurityContext with authenticated user
        Authentication auth = new UsernamePasswordAuthenticationToken("deepanshu", null, List.of());
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    @DisplayName("create() - creates and returns project detail")
    void create_validRequest_returnsProjectDetail() {
        ProjectRequest.Create request = ProjectRequest.Create.builder()
            .name("Test Project").description("A test project").build();

        when(userRepository.findByUsername("deepanshu")).thenReturn(Optional.of(currentUser));
        when(projectRepository.save(any(Project.class))).thenReturn(sampleProject);

        ApiResponse.ProjectDetail result = projectService.create(request);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Test Project");
        assertThat(result.getOwner().getUsername()).isEqualTo("deepanshu");
        assertThat(result.getProgressPercent()).isEqualTo(0);
        verify(projectRepository).save(any(Project.class));
    }

    @Test
    @DisplayName("getMyProjects() - returns paginated list of owner's projects")
    void getMyProjects_returnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Project> projectPage = new PageImpl<>(List.of(sampleProject), pageable, 1);

        when(userRepository.findByUsername("deepanshu")).thenReturn(Optional.of(currentUser));
        when(projectRepository.findByOwnerId(1L, pageable)).thenReturn(projectPage);

        Page<ApiResponse.ProjectDetail> result = projectService.getMyProjects(pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Test Project");
    }

    @Test
    @DisplayName("getById() - project found for owner returns detail")
    void getById_ownedProject_returnsDetail() {
        when(userRepository.findByUsername("deepanshu")).thenReturn(Optional.of(currentUser));
        when(projectRepository.findByIdAndOwnerId(1L, 1L)).thenReturn(Optional.of(sampleProject));

        ApiResponse.ProjectDetail result = projectService.getById(1L);
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getById() - project not found throws ResourceNotFoundException")
    void getById_notFound_throwsException() {
        when(userRepository.findByUsername("deepanshu")).thenReturn(Optional.of(currentUser));
        when(projectRepository.findByIdAndOwnerId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.getById(99L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("99");
    }

    @Test
    @DisplayName("update() - partial update applies only non-null fields")
    void update_partialFields_updatesOnlyProvided() {
        ProjectRequest.Update request = ProjectRequest.Update.builder()
            .name("Updated Name").build();

        when(userRepository.findByUsername("deepanshu")).thenReturn(Optional.of(currentUser));
        when(projectRepository.findByIdAndOwnerId(1L, 1L)).thenReturn(Optional.of(sampleProject));

        Project updatedProject = Project.builder()
            .id(1L).name("Updated Name").description("A test project")
            .status(Project.Status.ACTIVE).owner(currentUser).tasks(new ArrayList<>()).build();
        when(projectRepository.save(any())).thenReturn(updatedProject);

        ApiResponse.ProjectDetail result = projectService.update(1L, request);
        assertThat(result.getName()).isEqualTo("Updated Name");
        assertThat(result.getDescription()).isEqualTo("A test project"); // unchanged
    }

    @Test
    @DisplayName("delete() - owned project is deleted")
    void delete_ownedProject_deletesSuccessfully() {
        when(userRepository.findByUsername("deepanshu")).thenReturn(Optional.of(currentUser));
        when(projectRepository.findByIdAndOwnerId(1L, 1L)).thenReturn(Optional.of(sampleProject));

        projectService.delete(1L);
        verify(projectRepository).delete(sampleProject);
    }
}
