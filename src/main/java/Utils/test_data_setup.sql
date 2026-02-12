-- Test Data Setup for RH Database
-- Run this script to create sample data for testing the interview management system

USE rh;

-- Clear existing test data (optional - comment out if you want to keep existing data)
-- DELETE FROM interview_feedback;
-- DELETE FROM interview;
-- DELETE FROM job_application;
-- DELETE FROM job_offer;
-- DELETE FROM candidate;
-- DELETE FROM recruiter;
-- DELETE FROM users WHERE email LIKE '%@test.com';

-- ======================================
-- 1. CREATE TEST USERS
-- ======================================

-- Create Recruiter User
INSERT INTO users (email, password, role, first_name, last_name, phone, is_active)
VALUES ('recruiter@test.com', 'password123', 'RECRUITER', 'John', 'Smith', '1234567890', 1);

SET @recruiter_user_id = LAST_INSERT_ID();

-- Create Recruiter Profile
INSERT INTO recruiter (id, company_name, company_location)
VALUES (@recruiter_user_id, 'TechCorp Inc', 'San Francisco, CA');

-- Create Candidate User 1
INSERT INTO users (email, password, role, first_name, last_name, phone, is_active)
VALUES ('jane.doe@test.com', 'password123', 'CANDIDATE', 'Jane', 'Doe', '0987654321', 1);

SET @candidate1_user_id = LAST_INSERT_ID();

-- Create Candidate Profile 1
INSERT INTO candidate (id, location, education_level, experience_years, cv_path)
VALUES (@candidate1_user_id, 'New York, NY', 'Bachelor', 3, '/cvs/jane_doe_cv.pdf');

-- Create Candidate User 2
INSERT INTO users (email, password, role, first_name, last_name, phone, is_active)
VALUES ('mike.johnson@test.com', 'password123', 'CANDIDATE', 'Mike', 'Johnson', '5551234567', 1);

SET @candidate2_user_id = LAST_INSERT_ID();

-- Create Candidate Profile 2
INSERT INTO candidate (id, location, education_level, experience_years, cv_path)
VALUES (@candidate2_user_id, 'Austin, TX', 'Master', 5, '/cvs/mike_johnson_cv.pdf');

-- ======================================
-- 2. CREATE JOB OFFERS
-- ======================================

-- Job Offer 1: Senior Java Developer
INSERT INTO job_offer (recruiter_id, title, description, location, contract_type, deadline, status)
VALUES (
    @recruiter_user_id,
    'Senior Java Developer',
    'We are looking for an experienced Java developer to join our backend team. Must have 3+ years of experience with Spring Boot and microservices architecture.',
    'Remote',
    'CDI',
    '2026-03-31 23:59:59',
    'OPEN'
);

SET @job_offer1_id = LAST_INSERT_ID();

-- Job Offer 2: Frontend Developer
INSERT INTO job_offer (recruiter_id, title, description, location, contract_type, deadline, status)
VALUES (
    @recruiter_user_id,
    'Frontend Developer - React',
    'Join our frontend team to build modern web applications using React and TypeScript. Experience with state management and responsive design required.',
    'San Francisco, CA',
    'CDI',
    '2026-04-15 23:59:59',
    'OPEN'
);

SET @job_offer2_id = LAST_INSERT_ID();

-- Job Offer 3: Full Stack Developer
INSERT INTO job_offer (recruiter_id, title, description, location, contract_type, deadline, status)
VALUES (
    @recruiter_user_id,
    'Full Stack Developer',
    'Looking for a versatile developer comfortable with both frontend and backend technologies. Node.js and React experience preferred.',
    'Remote',
    'CDD',
    '2026-05-01 23:59:59',
    'OPEN'
);

SET @job_offer3_id = LAST_INSERT_ID();

-- ======================================
-- 3. CREATE JOB APPLICATIONS
-- ======================================

-- Application 1: Jane applies to Senior Java Developer (SUBMITTED)
INSERT INTO job_application (offer_id, candidate_id, phone, cover_letter, cv_path, applied_at, current_status)
VALUES (
    @job_offer1_id,
    @candidate1_user_id,
    '0987654321',
    'I am very interested in the Senior Java Developer position. I have 3 years of experience working with Spring Boot and have built several microservices applications in my current role.',
    '/cvs/jane_doe_cv.pdf',
    '2026-02-01 10:30:00',
    'SUBMITTED'
);

SET @application1_id = LAST_INSERT_ID();

-- Application 2: Jane applies to Frontend Developer (IN_REVIEW)
INSERT INTO job_application (offer_id, candidate_id, phone, cover_letter, cv_path, applied_at, current_status)
VALUES (
    @job_offer2_id,
    @candidate1_user_id,
    '0987654321',
    'I would love to join your frontend team. While my primary experience is in backend, I have worked extensively with React in my side projects.',
    '/cvs/jane_doe_cv.pdf',
    '2026-02-03 14:20:00',
    'IN_REVIEW'
);

SET @application2_id = LAST_INSERT_ID();

-- Application 3: Mike applies to Senior Java Developer (SHORTLISTED)
INSERT INTO job_application (offer_id, candidate_id, phone, cover_letter, cv_path, applied_at, current_status)
VALUES (
    @job_offer1_id,
    @candidate2_user_id,
    '5551234567',
    'With 5 years of Java development experience and a Master''s degree in Computer Science, I am confident I can contribute significantly to your team. I have led the development of multiple microservices projects.',
    '/cvs/mike_johnson_cv.pdf',
    '2026-02-05 09:15:00',
    'SHORTLISTED'
);

SET @application3_id = LAST_INSERT_ID();

-- Application 4: Mike applies to Full Stack Developer (SUBMITTED)
INSERT INTO job_application (offer_id, candidate_id, phone, cover_letter, cv_path, applied_at, current_status)
VALUES (
    @job_offer3_id,
    @candidate2_user_id,
    '5551234567',
    'As a full stack developer with experience in both Node.js and React, I believe I am a perfect fit for this role. I enjoy working across the entire stack and collaborating with cross-functional teams.',
    '/cvs/mike_johnson_cv.pdf',
    '2026-02-08 16:45:00',
    'SUBMITTED'
);

SET @application4_id = LAST_INSERT_ID();

-- ======================================
-- 4. CREATE SAMPLE INTERVIEWS (Optional)
-- ======================================

-- Interview 1: Mike's interview for Senior Java Developer (SCHEDULED)
INSERT INTO interview (application_id, recruiter_id, scheduled_at, duration_minutes, mode, meeting_link, location, status, notes)
VALUES (
    @application3_id,
    @recruiter_user_id,
    '2026-02-20 14:00:00',
    60,
    'ONLINE',
    'https://zoom.us/j/123456789',
    NULL,
    'SCHEDULED',
    'Technical interview - focus on Spring Boot and microservices'
);

SET @interview1_id = LAST_INSERT_ID();

-- Interview 2: Jane's interview for Senior Java Developer (DONE - ready for feedback)
INSERT INTO interview (application_id, recruiter_id, scheduled_at, duration_minutes, mode, meeting_link, location, status, notes)
VALUES (
    @application1_id,
    @recruiter_user_id,
    '2026-02-10 10:00:00',
    45,
    'ON_SITE',
    NULL,
    'TechCorp HQ, Building A, Conference Room 301',
    'DONE',
    'Initial screening interview'
);

SET @interview2_id = LAST_INSERT_ID();

-- ======================================
-- 5. CREATE SAMPLE FEEDBACK (Optional)
-- ======================================

-- Feedback for Jane's completed interview
INSERT INTO interview_feedback (interview_id, recruiter_id, overall_score, decision, comment)
VALUES (
    @interview2_id,
    @recruiter_user_id,
    85,
    'ACCEPTED',
    'Excellent technical skills and good communication. Jane demonstrated strong understanding of Spring Boot and microservices patterns. She answered all technical questions confidently and showed enthusiasm for the role. Recommended for next round.'
);

-- ======================================
-- VERIFICATION QUERIES
-- ======================================

-- Check what was created
SELECT 'Users Created:' as Info;
SELECT id, email, role, first_name, last_name FROM users WHERE email LIKE '%@test.com';

SELECT '' as '';
SELECT 'Job Offers Created:' as Info;
SELECT id, title, location, contract_type, status FROM job_offer;

SELECT '' as '';
SELECT 'Applications Created:' as Info;
SELECT ja.id, jo.title, u.email as candidate_email, ja.current_status, ja.applied_at
FROM job_application ja
JOIN job_offer jo ON ja.offer_id = jo.id
JOIN users u ON ja.candidate_id = u.id;

SELECT '' as '';
SELECT 'Interviews Created:' as Info;
SELECT i.id, u.email as candidate_email, i.scheduled_at, i.mode, i.status
FROM interview i
JOIN job_application ja ON i.application_id = ja.id
JOIN users u ON ja.candidate_id = u.id;

SELECT '' as '';
SELECT 'Feedback Created:' as Info;
SELECT if_tbl.id, if_tbl.overall_score, if_tbl.decision, u.email as candidate_email
FROM interview_feedback if_tbl
JOIN interview i ON if_tbl.interview_id = i.id
JOIN job_application ja ON i.application_id = ja.id
JOIN users u ON ja.candidate_id = u.id;

-- Summary
SELECT '' as '';
SELECT 'Summary:' as Info;
SELECT
    (SELECT COUNT(*) FROM users WHERE email LIKE '%@test.com') as Total_Users,
    (SELECT COUNT(*) FROM recruiter) as Total_Recruiters,
    (SELECT COUNT(*) FROM candidate) as Total_Candidates,
    (SELECT COUNT(*) FROM job_offer) as Total_Job_Offers,
    (SELECT COUNT(*) FROM job_application) as Total_Applications,
    (SELECT COUNT(*) FROM interview) as Total_Interviews,
    (SELECT COUNT(*) FROM interview_feedback) as Total_Feedbacks;

-- ======================================
-- NOTES
-- ======================================
--
-- After running this script, you will have:
-- - 1 Recruiter: recruiter@test.com (ID will vary)
-- - 2 Candidates: jane.doe@test.com, mike.johnson@test.com
-- - 3 Job Offers: Senior Java Developer, Frontend Developer, Full Stack Developer
-- - 4 Job Applications: Various applications from the candidates
-- - 2 Interviews: 1 scheduled (Mike), 1 completed (Jane)
-- - 1 Feedback: For Jane's completed interview
--
-- You can now test:
-- 1. Viewing applications (recruiter sees all, candidates see their own)
-- 2. Scheduling interviews from applications
-- 3. Viewing scheduled interviews
-- 4. Adding feedback to completed interviews
-- 5. Updating/deleting interviews
--
-- Login credentials (note: authentication not implemented yet):
-- - Recruiter: recruiter@test.com / password123
-- - Candidate 1: jane.doe@test.com / password123
-- - Candidate 2: mike.johnson@test.com / password123

