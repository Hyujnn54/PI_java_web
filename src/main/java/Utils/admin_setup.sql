-- Admin User Setup for RH Database
-- Run this script to create an admin user for testing the admin dashboard

USE rh;

-- Create Admin User
INSERT INTO users (email, password, role, first_name, last_name, phone, is_active)
VALUES ('admin@test.com', 'admin123', 'ADMIN', 'Admin', 'User', '9999999999', 1);

-- Verify the insert
SELECT * FROM users WHERE role = 'ADMIN';

