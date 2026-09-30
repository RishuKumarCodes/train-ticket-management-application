-- ==============================================================================
-- RailFlow: Train Ticket Management System - Database Schema
-- Dialect: MySQL 8.x / ANSI SQL Compatible
-- Note: Tables will be designed and added iteratively as we implement each module.
-- ==============================================================================

CREATE DATABASE IF NOT EXISTS train_ticket_db;
USE train_ticket_db;

-- ==============================================================================
-- 1. Authentication & Role-Based Access Control (RBAC)
-- ==============================================================================

CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NULL UNIQUE,
    phone VARCHAR(20) NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    salt VARCHAR(64) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role ENUM('PASSENGER', 'ADMIN') NOT NULL DEFAULT 'PASSENGER',
    status ENUM('ACTIVE', 'SUSPENDED') NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_users_username (username),
    INDEX idx_users_email (email),
    INDEX idx_users_phone (phone),
    INDEX idx_users_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Pre-seeded default Fixed Administrator (admin / admin)
INSERT IGNORE INTO users (id, username, email, phone, password_hash, salt, full_name, role)
VALUES (1, 'admin', 'admin@railflow.internal', '+910000000000', 
        '463d121d09680f21241659d31b0389901d0479bcf389c231258d6c3f5c36050f', 
        '0123456789abcdef0123456789abcdef', 'Station Master Admin', 'ADMIN');

