-- V2__seed_data.sql
-- Seed an admin user (password: Admin@123 — BCrypt hash below)
-- BCrypt cost=12 hash of 'Admin@123'
INSERT INTO users (username, email, password, full_name, role, is_active)
VALUES (
    'admin',
    'admin@taskmanager.dev',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/lewKyDALDR.oM.I4a',
    'System Administrator',
    'ADMIN',
    TRUE
);

-- Seed a demo user (password: User@1234)
INSERT INTO users (username, email, password, full_name, role, is_active)
VALUES (
    'deepanshu',
    'deepanshuk2555@gmail.com',
    '$2a$12$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh86',
    'Deepanshu Singh',
    'USER',
    TRUE
);
