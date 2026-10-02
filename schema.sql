-- ATS Resume Analyzer - Database Schema Script
-- Run this script in your MySQL instance to manually create the database and tables.
-- Note: The Spring Boot application is configured with 'spring.jpa.hibernate.ddl-auto=update', 
-- so it will automatically create these tables if they do not exist.

CREATE DATABASE IF NOT EXISTS resume_analyzer_db;
USE resume_analyzer_db;

-- 1. Users Table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    role VARCHAR(50) NOT NULL,
    created_at DATETIME NOT NULL
);

-- 2. Jobs Table (Target Job Descriptions)
CREATE TABLE IF NOT EXISTS jobs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    company VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    required_skills TEXT NOT NULL,
    location VARCHAR(255),
    experience_required VARCHAR(255),
    created_at DATETIME NOT NULL
);

-- 3. Resumes Table (Analysis Reports)
CREATE TABLE IF NOT EXISTS resumes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(255) NOT NULL,
    parsed_text LONGTEXT,
    ats_score INT,
    skills_found TEXT,
    skills_missing TEXT,
    suggestions TEXT,
    job_title_matched VARCHAR(255),
    match_percentage INT,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_resume_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
