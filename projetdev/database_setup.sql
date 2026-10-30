CREATE DATABASE IF NOT EXISTS rh;
USE rh;

-- Table users
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    phone VARCHAR(20),
    is_active BOOLEAN DEFAULT TRUE,
    role ENUM('CANDIDATE', 'RECRUITER', 'ADMIN') NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table candidate
CREATE TABLE IF NOT EXISTS candidate (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    location VARCHAR(255),
    education_level VARCHAR(255),
    experience_years INT,
    cv_path VARCHAR(255),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Table recruiter
CREATE TABLE IF NOT EXISTS recruiter (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    company_name VARCHAR(255),
    company_location VARCHAR(255),
    company_description TEXT,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Table recruitment_event
CREATE TABLE IF NOT EXISTS recruitment_event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recruiter_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    event_type VARCHAR(50),
    location VARCHAR(255),
    event_date TIMESTAMP,
    capacity INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (recruiter_id) REFERENCES recruiter(id) ON DELETE CASCADE
);

-- Table event_registration
CREATE TABLE IF NOT EXISTS event_registration (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id BIGINT NOT NULL,
    candidate_id BIGINT NOT NULL,
    registered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    attendance_status ENUM('PENDING', 'CONFIRMED', 'CANCELLED', 'ATTENDED') DEFAULT 'PENDING',
    FOREIGN KEY (event_id) REFERENCES recruitment_event(id) ON DELETE CASCADE,
    FOREIGN KEY (candidate_id) REFERENCES candidate(id) ON DELETE CASCADE
);

-- Insert default admin if not exists
INSERT INTO users (email, password, role, first_name, last_name, is_active)
SELECT 'admin@rh.com', 'admin123', 'ADMIN', 'Super', 'Admin', TRUE
WHERE NOT EXISTS (SELECT * FROM users WHERE email = 'admin@rh.com');
