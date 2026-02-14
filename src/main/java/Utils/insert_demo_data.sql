-- Script pour insérer des données de démonstration dans la base de données RH
-- Exécuter ce script si vous avez déjà créé la base de données mais qu'elle est vide

USE rh;

-- Insert demo user (Recruiter 1)
INSERT INTO `users` (`id`, `email`, `password`, `role`, `first_name`, `last_name`, `phone`, `is_active`, `created_at`)
VALUES (1, 'recruiter@talentbridge.com', 'demo123', 'RECRUITER', 'Demo', 'Recruiter', '+1234567890', 1, NOW())
ON DUPLICATE KEY UPDATE email = email;

-- Insert demo recruiter profile (Recruiter 1)
INSERT INTO `recruiter` (`id`, `company_name`, `company_location`)
VALUES (1, 'Talent Bridge Inc.', 'San Francisco, CA')
ON DUPLICATE KEY UPDATE company_name = company_name;

-- Insert demo candidate user
INSERT INTO `users` (`id`, `email`, `password`, `role`, `first_name`, `last_name`, `phone`, `is_active`, `created_at`)
VALUES (2, 'candidate@example.com', 'candidate123', 'CANDIDATE', 'John', 'Doe', '+0987654321', 1, NOW())
ON DUPLICATE KEY UPDATE email = email;

-- Insert demo candidate profile
INSERT INTO `candidate` (`id`, `location`, `education_level`, `experience_years`, `cv_path`)
VALUES (2, 'New York, NY', 'Bachelor', 3, '/cv/john_doe.pdf')
ON DUPLICATE KEY UPDATE location = location;

-- Insert a second recruiter for testing multi-user separation
INSERT INTO `users` (`id`, `email`, `password`, `role`, `first_name`, `last_name`, `phone`, `is_active`, `created_at`)
VALUES (3, 'recruiter2@talentbridge.com', 'demo123', 'RECRUITER', 'Jane', 'Smith', '+1122334455', 1, NOW())
ON DUPLICATE KEY UPDATE email = email;

INSERT INTO `recruiter` (`id`, `company_name`, `company_location`)
VALUES (3, 'TechCorp Solutions', 'New York, NY')
ON DUPLICATE KEY UPDATE company_name = company_name;

-- Afficher un message de confirmation
SELECT 'Demo data inserted successfully!' AS Status;
SELECT '3 users created: 2 recruiters (ID 1 and 3), 1 candidate (ID 2)' AS Info;

-- Vérifier les données
SELECT id, email, role, first_name, last_name FROM users;
SELECT id, company_name, company_location FROM recruiter;
SELECT id, location, education_level FROM candidate;

