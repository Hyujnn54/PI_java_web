-- Script de création de la base de données pour le projet JavaFX - Schéma Complet
-- Application de Recrutement avec Authentification Multi-Rôles

-- Créer la base de données si elle n'existe pas
CREATE DATABASE IF NOT EXISTS rh CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Utiliser la base de données
USE rh;

-- Table: users (utilisateurs de base)
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    login VARCHAR(25) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    phone VARCHAR(30),
    is_active TINYINT(1) DEFAULT 1,
    role ENUM('ADMIN', 'CANDIDATE', 'RECRUITER') NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_login (login),
    INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: candidate (candidats - liés à users.id)
CREATE TABLE IF NOT EXISTS candidate (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    location VARCHAR(255),
    education_level VARCHAR(100),
    experience_years INT,
    cv_path VARCHAR(255),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: recruiter (recruteurs - liés à users.id)
CREATE TABLE IF NOT EXISTS recruiter (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    company_name VARCHAR(255),
    company_location VARCHAR(255),
    company_description VARCHAR(255),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: recruitment_event (événements de recrutement)
CREATE TABLE IF NOT EXISTS recruitment_event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recruiter_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    event_type ENUM('JOB_FAIR', 'WEBINAR', 'INTERVIEW_DAY') NOT NULL,
    description TEXT,
    location VARCHAR(255),
    event_date DATETIME NOT NULL,
    capacity INT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (recruiter_id) REFERENCES recruiter(id) ON DELETE CASCADE,
    INDEX idx_recruiter_id (recruiter_id),
    INDEX idx_event_type (event_type),
    INDEX idx_event_date (event_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: event_registrations (inscriptions aux événements)
CREATE TABLE IF NOT EXISTS event_registrations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id BIGINT NOT NULL,
    candidate_id BIGINT NOT NULL,
    registered_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    attendance_status ENUM('REGISTERED', 'ATTENDED', 'CANCELLED', 'NO_SHOW') DEFAULT 'REGISTERED',
    FOREIGN KEY (event_id) REFERENCES recruitment_event(id) ON DELETE CASCADE,
    FOREIGN KEY (candidate_id) REFERENCES candidate(id) ON DELETE CASCADE,
    INDEX idx_event_id (event_id),
    INDEX idx_candidate_id (candidate_id),
    INDEX idx_status (attendance_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Données de test

-- Insérer des utilisateurs de test (admin, candidats, recruteurs)
INSERT INTO users (login, email, password, first_name, last_name, phone, is_active, role, created_at)
VALUES 
    ('admin', 'admin@rh.com', 'admin123', 'Admin', 'System', '+216 70 000 000', 1, 'ADMIN', NOW()),
    ('john_doe', 'john.doe@example.com', 'password123', 'John', 'Doe', '+216 20 123 456', 1, 'CANDIDATE', NOW()),
    ('jane_smith', 'jane.smith@example.com', 'password123', 'Jane', 'Smith', '+216 20 234 567', 1, 'CANDIDATE', NOW()),
    ('acme_corp', 'hr@acme.com', 'password123', 'ACME', 'Corporation', '+216 71 123 456', 1, 'RECRUITER', NOW()),
    ('tech_inc', 'contact@techinc.com', 'password123', 'Tech', 'Inc', '+216 71 234 567', 1, 'RECRUITER', NOW());

-- Insérer des candidats (user_id au lieu de login)
INSERT INTO candidate (user_id, location, education_level, experience_years, cv_path)
VALUES 
    (2, 'Tunis, Tunisia', 'Master en Informatique', 5, '/uploads/cv/john_doe.pdf'),
    (3, 'Sfax, Tunisia', 'Licence en Génie Logiciel', 2, '/uploads/cv/jane_smith.pdf');

-- Insérer des recruteurs (user_id au lieu de login)
INSERT INTO recruiter (user_id, company_name, company_location, company_description)
VALUES 
    (4, 'ACME Corporation', 'Tunis, Tunisia', 'Leader mondial en technologie et innovation'),
    (5, 'Tech Inc.', 'Sousse, Tunisia', 'Startup spécialisée en développement logiciel');

-- Insérer des événements de recrutement (avec location et capacity)
INSERT INTO recruitment_event (recruiter_id, title, event_type, description, location, event_date, capacity, created_at)
VALUES 
    (1, 'Job Fair Tech 2026', 'JOB_FAIR', 'Grande foire d\'emploi pour les professionnels IT et développeurs', 'Centre des Expositions, Tunis', '2026-03-15 09:00:00', 500, NOW()),
    (1, 'Webinar: Introduction à JavaFX', 'WEBINAR', 'Apprendre les bases du développement d\'interfaces avec JavaFX', 'En ligne', '2026-02-20 14:00:00', 100, NOW()),
    (2, 'Interview Day - Développeurs Java', 'INTERVIEW_DAY', 'Journée d\'entretiens pour développeurs Java débutants et expérimentés', 'Tech Inc. Office, Sousse', '2026-02-25 08:00:00', 30, NOW());

-- Insérer des inscriptions aux événements
INSERT INTO event_registrations (event_id, candidate_id, registered_at, attendance_status)
VALUES 
    (1, 1, NOW(), 'REGISTERED'),
    (1, 2, NOW(), 'REGISTERED'),
    (2, 1, NOW(), 'ATTENDED'),
    (3, 2, NOW(), 'REGISTERED');

-- Afficher les données
SELECT 'USERS:' as '';
SELECT * FROM users;

SELECT 'CANDIDATES:' as '';
SELECT * FROM candidate;

SELECT 'RECRUITERS:' as '';
SELECT * FROM recruiter;

SELECT 'RECRUITMENT EVENTS:' as '';
SELECT * FROM recruitment_event;

SELECT 'EVENT REGISTRATIONS:' as '';
SELECT * FROM event_registrations;
