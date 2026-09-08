-- V1__init_schema.sql
-- Initial schema: users, projects, tasks

CREATE TABLE users (
    id          BIGSERIAL PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    email       VARCHAR(100) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    full_name   VARCHAR(100),
    role        VARCHAR(20)  NOT NULL DEFAULT 'USER',
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email    ON users(email);
CREATE INDEX idx_users_username ON users(username);

CREATE TABLE projects (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100)  NOT NULL,
    description VARCHAR(500),
    status      VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    owner_id    BIGINT        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    due_date    TIMESTAMP,
    created_at  TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_projects_owner  ON projects(owner_id);
CREATE INDEX idx_projects_status ON projects(status);

CREATE TABLE tasks (
    id               BIGSERIAL PRIMARY KEY,
    title            VARCHAR(200)  NOT NULL,
    description      VARCHAR(1000),
    status           VARCHAR(20)   NOT NULL DEFAULT 'TODO',
    priority         VARCHAR(10)   NOT NULL DEFAULT 'MEDIUM',
    project_id       BIGINT        NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    assignee_id      BIGINT        REFERENCES users(id) ON DELETE SET NULL,
    due_date         TIMESTAMP,
    completed_at     TIMESTAMP,
    estimated_hours  INTEGER,
    created_at       TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_tasks_project  ON tasks(project_id);
CREATE INDEX idx_tasks_assignee ON tasks(assignee_id);
CREATE INDEX idx_tasks_status   ON tasks(status);
CREATE INDEX idx_tasks_priority ON tasks(priority);
