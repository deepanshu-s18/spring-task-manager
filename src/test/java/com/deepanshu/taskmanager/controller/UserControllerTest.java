package com.deepanshu.taskmanager.controller;

import com.deepanshu.taskmanager.dto.response.ApiResponse;
import com.deepanshu.taskmanager.exception.ResourceNotFoundException;
import com.deepanshu.taskmanager.model.Task;
import com.deepanshu.taskmanager.model.User;
import com.deepanshu.taskmanager.repository.UserRepository;
import com.deepanshu.taskmanager.service.TaskService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserController Unit Tests")
class UserControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TaskService taskService;

    @InjectMocks
    private UserController userController;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
            .id(1L)
            .username("deepanshu")
            .email("deepanshuk2555@gmail.com")
            .fullName("Deepanshu Singh")
            .role(User.Role.USER)
            .isActive(true)
            .createdAt(LocalDateTime.now())
            .build();
    }

    @Test
    @DisplayName("getProfile - returns user summary successfully")
    void getProfile_Success() {
        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn("deepanshu");
        when(userRepository.findByUsername("deepanshu")).thenReturn(Optional.of(sampleUser));

        ResponseEntity<ApiResponse.UserSummary> response = userController.getProfile(userDetails);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getUsername()).isEqualTo("deepanshu");
        assertThat(response.getBody().getFullName()).isEqualTo("Deepanshu Singh");
    }

    @Test
    @DisplayName("getProfile - throws ResourceNotFoundException when user not found")
    void getProfile_UserNotFound_ThrowsException() {
        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn("missingUser");
        when(userRepository.findByUsername("missingUser")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userController.getProfile(userDetails))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("User");
    }

    @Test
    @DisplayName("getMyActiveTasks - returns active assigned tasks")
    void getMyActiveTasks_Success() {
        ApiResponse.TaskDetail taskDetail = ApiResponse.TaskDetail.builder()
            .id(10L)
            .title("Urgent Task")
            .status(Task.Status.IN_PROGRESS)
            .priority(Task.Priority.CRITICAL)
            .build();

        when(taskService.getMyActiveTasks()).thenReturn(List.of(taskDetail));

        ResponseEntity<List<ApiResponse.TaskDetail>> response = userController.getMyActiveTasks();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getTitle()).isEqualTo("Urgent Task");
    }
}
