package com.deepanshu.taskmanager.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI taskManagerOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
            .info(new Info()
                .title("Task Manager REST API")
                .description("""
                    RESTful API for a multi-user task management system.
                    
                    **Features:**
                    - JWT-based authentication (access + refresh tokens)
                    - Role-based access control (ADMIN / USER)
                    - Project and Task CRUD with status tracking
                    - Pagination, filtering, and sorting
                    - Spring Data JPA + PostgreSQL + Flyway migrations
                    """)
                .version("v1.0.0")
                .contact(new Contact()
                    .name("Deepanshu Singh")
                    .email("deepanshuk2555@gmail.com")
                    .url("https://github.com/deepanshu-s18")))
            .servers(List.of(
                new Server().url("http://localhost:8080").description("Local Development"),
                new Server().url("https://task-manager.deepanshu.dev").description("Production")))
            .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
            .components(new Components()
                .addSecuritySchemes(securitySchemeName,
                    new SecurityScheme()
                        .name(securitySchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Enter JWT access token from /api/v1/auth/login")));
    }
}
