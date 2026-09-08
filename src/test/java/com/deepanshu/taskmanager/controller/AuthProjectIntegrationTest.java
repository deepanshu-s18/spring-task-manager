package com.deepanshu.taskmanager.controller;

import com.deepanshu.taskmanager.model.User;
import com.deepanshu.taskmanager.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Full integration tests using real H2 in-memory DB (test profile).
 * Tests the complete HTTP request→response cycle including:
 * - Spring Security filter chain
 * - JWT token generation and validation
 * - Service and repository layers
 * - Response serialization
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Auth + Project Integration Tests")
class AuthProjectIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private static String accessToken;
    private static Long createdProjectId;

    @BeforeEach
    void cleanupUsers() {
        // Ensure clean state — delete test user if exists
        userRepository.findByUsername("integrationuser").ifPresent(userRepository::delete);
    }

    @Test
    @Order(1)
    @DisplayName("POST /auth/register → 201, returns JWT tokens and user summary")
    void register_success_returns201WithTokens() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "username", "integrationuser",
                    "email", "integration@test.com",
                    "password", "Test@1234",
                    "fullName", "Integration User"
                ))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.refreshToken").isNotEmpty())
            .andExpect(jsonPath("$.user.username").value("integrationuser"))
            .andExpect(jsonPath("$.user.role").value("USER"))
            .andReturn();

        String body = result.getResponse().getContentAsString();
        accessToken = objectMapper.readTree(body).get("accessToken").asText();
        assertThat(accessToken).isNotBlank();
        assertThat(accessToken.split("\\.")).hasSize(3); // valid JWT
    }

    @Test
    @Order(2)
    @DisplayName("POST /auth/register → 409 on duplicate username")
    void register_duplicateUsername_returns409() throws Exception {
        // Create user first
        userRepository.save(User.builder()
            .username("integrationuser")
            .email("integration@test.com")
            .password(passwordEncoder.encode("Test@1234"))
            .role(User.Role.USER).build());

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "username", "integrationuser",
                    "email", "other@test.com",
                    "password", "Test@1234"
                ))))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.message").value(containsString("integrationuser")));
    }

    @Test
    @Order(3)
    @DisplayName("POST /auth/login → 200 with valid credentials")
    void login_validCredentials_returns200() throws Exception {
        userRepository.save(User.builder()
            .username("integrationuser")
            .email("integration@test.com")
            .password(passwordEncoder.encode("Test@1234"))
            .role(User.Role.USER).build());

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "usernameOrEmail", "integrationuser",
                    "password", "Test@1234"
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andReturn();

        accessToken = objectMapper.readTree(result.getResponse().getContentAsString())
            .get("accessToken").asText();
    }

    @Test
    @Order(4)
    @DisplayName("POST /auth/login → 401 with wrong password")
    void login_wrongPassword_returns401() throws Exception {
        userRepository.save(User.builder()
            .username("integrationuser")
            .email("integration@test.com")
            .password(passwordEncoder.encode("Test@1234"))
            .role(User.Role.USER).build());

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "usernameOrEmail", "integrationuser",
                    "password", "WrongPass99"
                ))))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(5)
    @DisplayName("GET /projects → 401 without token")
    void getProjects_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/projects"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(6)
    @DisplayName("POST /projects → 201 with valid token (full request lifecycle)")
    void createProject_withValidToken_returns201() throws Exception {
        // Register to get a fresh token
        MvcResult regResult = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "username", "integrationuser",
                    "email", "integration@test.com",
                    "password", "Test@1234"
                ))))
            .andExpect(status().isCreated())
            .andReturn();

        String token = objectMapper.readTree(regResult.getResponse().getContentAsString())
            .get("accessToken").asText();

        MvcResult projectResult = mockMvc.perform(post("/api/v1/projects")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "name", "Integration Test Project",
                    "description", "Created via integration test"
                ))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Integration Test Project"))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.owner.username").value("integrationuser"))
            .andExpect(jsonPath("$.progressPercent").value(0))
            .andReturn();

        createdProjectId = objectMapper.readTree(projectResult.getResponse().getContentAsString())
            .get("id").asLong();

        // Immediately fetch it to verify @EntityGraph + readOnly path
        mockMvc.perform(get("/api/v1/projects/" + createdProjectId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(createdProjectId));
    }

    @Test
    @Order(7)
    @DisplayName("Bean validation → 400 on blank project name")
    void createProject_blankName_returns400() throws Exception {
        MvcResult regResult = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "username", "integrationuser",
                    "email", "integration@test.com",
                    "password", "Test@1234"
                ))))
            .andExpect(status().isCreated())
            .andReturn();

        String token = objectMapper.readTree(regResult.getResponse().getContentAsString())
            .get("accessToken").asText();

        mockMvc.perform(post("/api/v1/projects")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("name", ""))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").isNotEmpty());
    }
}
