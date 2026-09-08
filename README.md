# 🗂️ Spring Task Manager — REST API

> **A production-quality RESTful Task Management API** built with Java 17 + Spring Boot 3, demonstrating enterprise-grade application engineering practices.

[![CI](https://github.com/deepanshu-s18/spring-task-manager/actions/workflows/ci.yml/badge.svg)](https://github.com/deepanshu-s18/spring-task-manager/actions)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg)](https://www.postgresql.org/)

---

## ✨ Features

| Feature | Details |
|---|---|
| **REST API** | 16 endpoints — Users, Projects, Tasks |
| **JWT Authentication** | Access (24h) + Refresh (7d) tokens, login by username or email |
| **Role-Based Access Control** | `ADMIN` / `USER` roles via Spring Security |
| **Spring Data JPA** | `@EntityGraph` to eliminate N+1 queries; Flyway schema migrations |
| **OpenAPI 3.0 / Swagger UI** | Auto-generated docs at `/swagger-ui.html` |
| **Validation** | Bean Validation on all request DTOs |
| **Testing** | JUnit 5 + Mockito; JaCoCo coverage enforced ≥ 75% |
| **Docker** | Multi-stage Dockerfile + docker-compose (app + PostgreSQL) |
| **CI/CD** | GitHub Actions — test → coverage report → Docker build → push |

---

## 🚀 Quick Start

### Option 1: Docker Compose (Recommended)
```bash
git clone https://github.com/deepanshu-s18/spring-task-manager.git
cd spring-task-manager
docker-compose up --build
```
API available at: `http://localhost:8080`
Swagger UI: `http://localhost:8080/swagger-ui.html`

### Option 2: Local with PostgreSQL
```bash
# 1. Start PostgreSQL
docker run -d -e POSTGRES_DB=taskmanager -e POSTGRES_USER=taskuser \
           -e POSTGRES_PASSWORD=taskpass -p 5432:5432 postgres:16-alpine

# 2. Run the app
mvn spring-boot:run
```

---

## 📡 API Endpoints

### Authentication
| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/v1/auth/register` | Register new user |
| `POST` | `/api/v1/auth/login` | Login (username or email) |
| `POST` | `/api/v1/auth/refresh` | Refresh access token |

### Projects (JWT required)
| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/v1/projects` | Create project |
| `GET` | `/api/v1/projects?status=ACTIVE&page=0&size=10` | List my projects (paginated, filterable) |
| `GET` | `/api/v1/projects/{id}` | Get project by ID |
| `PUT` | `/api/v1/projects/{id}` | Update project |
| `DELETE` | `/api/v1/projects/{id}` | Delete project |

### Tasks (JWT required)
| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/v1/projects/{id}/tasks` | Create task |
| `GET` | `/api/v1/projects/{id}/tasks?status=TODO&page=0` | List tasks (paginated, filterable) |
| `GET` | `/api/v1/projects/{id}/tasks/{taskId}` | Get task by ID |
| `PUT` | `/api/v1/projects/{id}/tasks/{taskId}` | Full task update |
| `PATCH` | `/api/v1/projects/{id}/tasks/{taskId}/status` | Status-only patch |
| `DELETE` | `/api/v1/projects/{id}/tasks/{taskId}` | Delete task |

---

## 🔐 Authentication Example

```bash
# Register
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"deepanshu","email":"deepanshuk2555@gmail.com","password":"Test@1234","fullName":"Deepanshu Singh"}'

# Login → get access token
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"deepanshu","password":"Test@1234"}'

# Use token
curl -X GET http://localhost:8080/api/v1/projects \
  -H "Authorization: Bearer <access_token>"
```

---

## 🧪 Running Tests

```bash
# Run all tests with coverage report
mvn clean verify

# View coverage report
open target/site/jacoco/index.html
```

---

## 🏗️ Architecture

```
src/main/java/com/deepanshu/taskmanager/
├── config/          → SecurityConfig, OpenAPIConfig
├── controller/      → AuthController, ProjectController, TaskController
├── service/         → AuthService, ProjectService, TaskService, CustomUserDetailsService
├── repository/      → UserRepository, ProjectRepository, TaskRepository (@EntityGraph)
├── model/           → User, Project, Task (JPA entities with auditing)
├── dto/
│   ├── request/     → AuthRequest, ProjectRequest, TaskRequest (Bean Validated)
│   └── response/    → ApiResponse (UserSummary, ProjectDetail, TaskDetail, ErrorResponse)
├── security/        → JwtTokenProvider, JwtAuthFilter
└── exception/       → GlobalExceptionHandler + custom exceptions
```

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.2, Spring Security, Spring Data JPA |
| Auth | JWT (JJWT 0.12), BCrypt (cost=12) |
| Database | PostgreSQL 16 + Flyway migrations |
| Documentation | SpringDoc OpenAPI 3.0 / Swagger UI |
| Testing | JUnit 5, Mockito, Spring Boot Test |
| Coverage | JaCoCo (≥75% line coverage enforced) |
| Containerization | Docker (multi-stage) + docker-compose |
| CI/CD | GitHub Actions |
