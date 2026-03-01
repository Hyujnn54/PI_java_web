-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: Mar 01, 2026 at 10:28 PM
-- Server version: 10.4.32-MariaDB
-- PHP Version: 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `rh`
--

-- --------------------------------------------------------

--
-- Table structure for table `admin`
--

CREATE TABLE `admin` (
  `id` bigint(20) NOT NULL,
  `assigned_area` varchar(100) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `admin`
--

INSERT INTO `admin` (`id`, `assigned_area`) VALUES
(1, 'Global Management'),
(10, 'SUPER ADMIN');

-- --------------------------------------------------------

--
-- Table structure for table `application_status_history`
--

CREATE TABLE `application_status_history` (
  `id` bigint(20) NOT NULL,
  `application_id` bigint(20) NOT NULL,
  `status` enum('SUBMITTED','IN_REVIEW','SHORTLISTED','REJECTED','INTERVIEW','HIRED','ARCHIVED','UNARCHIVED') NOT NULL,
  `changed_at` datetime DEFAULT current_timestamp(),
  `changed_by` bigint(20) NOT NULL,
  `note` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `application_status_history`
--

INSERT INTO `application_status_history` (`id`, `application_id`, `status`, `changed_at`, `changed_by`, `note`) VALUES
(1, 1, 'IN_REVIEW', '2026-02-12 08:32:06', 2, 'Profile looks good'),
(2, 2, 'SHORTLISTED', '2026-02-12 08:32:06', 3, 'Strong experience'),
(3, 3, 'REJECTED', '2026-02-12 08:32:06', 2, 'Not enough experience'),
(12, 1, 'SHORTLISTED', '2026-02-27 08:54:55', 2, 'Candidate has been shortlisted'),
(14, 1, 'INTERVIEW', '2026-02-27 09:45:47', 2, 'Entretien planifié pour le 28/02/2026 10:00'),
(15, 3, 'INTERVIEW', '2026-02-27 10:11:21', 2, 'Entretien planifié pour le 28/02/2026 10:00'),
(17, 1, 'IN_REVIEW', '2026-02-27 14:50:09', 2, 'Le recruteur examine cette candidature'),
(18, 3, 'IN_REVIEW', '2026-02-27 14:50:09', 2, 'Le recruteur examine cette candidature'),
(19, 5, 'SUBMITTED', '2026-02-27 14:54:55', 4, 'Application submitted'),
(20, 5, 'ARCHIVED', '2026-02-27 16:05:13', 1, 'Application archivée par l\'admin'),
(21, 1, 'INTERVIEW', '2026-02-27 16:06:47', 2, 'Entretien planifié pour le 28/02/2026 16:00'),
(23, 7, 'SUBMITTED', '2026-02-27 16:23:48', 4, 'Application submitted'),
(26, 7, 'SUBMITTED', '2026-02-28 13:45:25', 4, 'Candidate changed the cover letter'),
(27, 7, 'SUBMITTED', '2026-02-28 13:46:11', 4, 'Candidate changed the cover letter'),
(28, 3, 'INTERVIEW', '2026-02-28 13:48:37', 2, 'Entretien planifié pour le 01/03/2026 14:00'),
(29, 1, 'IN_REVIEW', '2026-02-28 13:48:49', 2, 'Le recruteur a commencé l\'examen de cette candidature'),
(30, 1, 'SHORTLISTED', '2026-02-28 13:48:52', 2, 'Le recruteur apprécie ce profil et a présélectionné le candidat'),
(31, 1, 'IN_REVIEW', '2026-02-28 13:49:02', 2, 'Le recruteur examine cette candidature'),
(33, 1, 'SHORTLISTED', '2026-02-28 13:50:50', 2, 'Le candidat a été présélectionné'),
(34, 7, 'SUBMITTED', '2026-02-28 13:51:50', 4, 'Candidate changed the cover letter'),
(35, 9, 'SUBMITTED', '2026-02-28 13:53:36', 4, 'Application submitted'),
(38, 1, 'INTERVIEW', '2026-02-28 17:11:01', 2, 'Entretien planifié pour le 01/03/2026 14:00'),
(39, 2, 'SHORTLISTED', '2026-02-28 17:50:11', 1, 'Le candidat a été présélectionné'),
(40, 9, 'SHORTLISTED', '2026-02-28 17:50:11', 1, 'Le candidat a été présélectionné'),
(41, 11, 'SUBMITTED', '2026-03-01 19:22:25', 8, 'Application submitted'),
(42, 11, 'SHORTLISTED', '2026-03-01 19:23:28', 7, 'Le candidat a été présélectionné'),
(43, 11, 'INTERVIEW', '2026-03-01 19:23:41', 7, 'Entretien planifié pour le 02/03/2026 14:00'),
(44, 12, 'SUBMITTED', '2026-03-01 19:27:11', 8, 'Application submitted');

-- --------------------------------------------------------

--
-- Table structure for table `candidate`
--

CREATE TABLE `candidate` (
  `id` bigint(20) NOT NULL,
  `user_id` bigint(20) DEFAULT NULL,
  `location` varchar(255) DEFAULT NULL,
  `education_level` varchar(100) DEFAULT NULL,
  `experience_years` int(11) DEFAULT NULL,
  `cv_path` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `candidate`
--

INSERT INTO `candidate` (`id`, `user_id`, `location`, `education_level`, `experience_years`, `cv_path`) VALUES
(4, 4, 'Tunis', 'Bachelor', 2, 'cv/ahmed.pdf'),
(5, 5, 'Sousse', 'Master', 4, 'cv/sara.pdf'),
(6, 6, 'Sfax', 'Engineer', 1, 'cv/youssef.pdf'),
(8, 8, '', '', NULL, '');

-- --------------------------------------------------------

--
-- Table structure for table `candidate_skill`
--

CREATE TABLE `candidate_skill` (
  `id` bigint(20) NOT NULL,
  `candidate_id` bigint(20) NOT NULL,
  `skill_name` varchar(100) NOT NULL,
  `level` enum('BEGINNER','INTERMEDIATE','ADVANCED') NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `candidate_skill`
--

INSERT INTO `candidate_skill` (`id`, `candidate_id`, `skill_name`, `level`) VALUES
(1, 4, 'Java', 'INTERMEDIATE'),
(2, 4, 'SQL', 'ADVANCED'),
(3, 5, 'Python', 'ADVANCED'),
(4, 5, 'Machine Learning', 'INTERMEDIATE'),
(5, 6, 'C++', 'INTERMEDIATE'),
(6, 6, 'Networking', 'BEGINNER'),
(21, 8, 'AWS', 'INTERMEDIATE'),
(22, 8, 'Git', 'INTERMEDIATE'),
(23, 8, 'JavaScript Style Sheets', 'BEGINNER');

-- --------------------------------------------------------

--
-- Table structure for table `event_registration`
--

CREATE TABLE `event_registration` (
  `id` bigint(20) NOT NULL,
  `event_id` bigint(20) NOT NULL,
  `candidate_id` bigint(20) NOT NULL,
  `registered_at` datetime DEFAULT current_timestamp(),
  `attendance_status` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `event_registration`
--

INSERT INTO `event_registration` (`id`, `event_id`, `candidate_id`, `registered_at`, `attendance_status`) VALUES
(2, 1, 5, '2026-02-12 08:32:41', 'CONFIRMED'),
(3, 2, 6, '2026-02-12 08:32:41', 'REGISTERED'),
(16, 2, 4, '2026-02-19 13:03:30', 'PENDING'),
(17, 1, 4, '2026-02-19 13:15:23', 'CONFIRMED'),
(18, 4, 4, '2026-03-01 14:01:54', 'CONFIRMED'),
(19, 1, 8, '2026-03-01 18:22:35', 'PENDING'),
(20, 2, 8, '2026-03-01 18:22:40', 'PENDING'),
(21, 4, 8, '2026-03-01 18:22:41', 'PENDING'),
(22, 7, 8, '2026-03-01 18:25:39', 'PENDING'),
(23, 9, 8, '2026-03-01 18:25:42', 'PENDING'),
(24, 11, 8, '2026-03-01 18:25:43', 'REJECTED'),
(25, 6, 8, '2026-03-01 18:25:44', 'PENDING'),
(26, 8, 8, '2026-03-01 18:25:45', 'PENDING'),
(27, 10, 8, '2026-03-01 18:25:46', 'PENDING');

-- --------------------------------------------------------

--
-- Table structure for table `event_review`
--

CREATE TABLE `event_review` (
  `id` bigint(20) NOT NULL,
  `event_id` bigint(20) DEFAULT NULL,
  `candidate_id` bigint(20) DEFAULT NULL,
  `rating` int(11) DEFAULT NULL,
  `comment` text DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `event_review`
--

INSERT INTO `event_review` (`id`, `event_id`, `candidate_id`, `rating`, `comment`, `created_at`) VALUES
(2, 11, 8, 5, 'hello world', '2026-03-01 17:51:00');

-- --------------------------------------------------------

--
-- Table structure for table `interview`
--

CREATE TABLE `interview` (
  `id` bigint(20) NOT NULL,
  `application_id` bigint(20) NOT NULL,
  `recruiter_id` bigint(20) NOT NULL,
  `scheduled_at` datetime NOT NULL,
  `duration_minutes` int(11) NOT NULL,
  `mode` enum('ONLINE','ON_SITE') NOT NULL,
  `meeting_link` varchar(255) DEFAULT NULL,
  `location` varchar(255) DEFAULT NULL,
  `status` enum('SCHEDULED','CANCELLED','DONE') DEFAULT 'SCHEDULED',
  `notes` text DEFAULT NULL,
  `created_at` datetime DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `interview`
--

INSERT INTO `interview` (`id`, `application_id`, `recruiter_id`, `scheduled_at`, `duration_minutes`, `mode`, `meeting_link`, `location`, `status`, `notes`, `created_at`) VALUES
(10, 1, 2, '2026-03-01 10:52:00', 60, 'ON_SITE', NULL, 'azzzzzzzzz', 'SCHEDULED', '', '2026-02-22 10:51:09'),
(13, 2, 2, '2026-03-01 14:00:00', 60, 'ONLINE', 'https://meet.google.com/f1f-91f7-614', NULL, 'SCHEDULED', '', '2026-02-23 12:28:24'),
(14, 1, 2, '2026-02-27 10:47:00', 60, 'ON_SITE', NULL, 'qsdsqdsq', 'SCHEDULED', '', '2026-02-26 10:19:10'),
(15, 2, 2, '2026-02-27 10:50:00', 60, 'ON_SITE', NULL, 'lkkhjfgdfsd', 'SCHEDULED', '', '2026-02-26 10:48:38'),
(16, 2, 2, '2026-02-27 16:30:00', 60, 'ONLINE', 'https://meet.jit.si/Interview_1772119399639_h9KARYPYfs_715600#config.prejoinPageEnabled=false', NULL, 'SCHEDULED', '', '2026-02-26 16:23:38'),
(18, 2, 2, '2026-02-27 17:00:00', 60, 'ON_SITE', NULL, 'NBBV', 'SCHEDULED', '', '2026-02-26 16:52:54'),
(19, 1, 2, '2026-03-05 14:00:00', 54, 'ON_SITE', NULL, 'kljhgf', 'SCHEDULED', '', '2026-02-26 18:00:24'),
(20, 1, 2, '2026-02-27 17:00:00', 60, 'ONLINE', 'https://meet.jit.si/TalentBridge-Interview-1-dhrIxPe8Zo2mfd40c00', NULL, 'SCHEDULED', '', '2026-02-26 18:05:16'),
(22, 1, 2, '2026-02-28 10:00:00', 60, 'ONLINE', 'https://meet.jit.si/TalentBridge-Interview-1-vByBvaZwjJ7y379e280', NULL, 'SCHEDULED', '', '2026-02-27 09:45:47'),
(23, 3, 2, '2026-02-28 10:00:00', 60, 'ONLINE', 'https://meet.jit.si/TalentBridge-Interview-3-oqmQKBqTTtRM379e280', NULL, 'SCHEDULED', '', '2026-02-27 10:11:21'),
(24, 1, 2, '2026-02-28 16:00:00', 60, 'ONLINE', 'https://meet.jit.si/TalentBridge-Interview-1-rwhfP1vj52Hh4559c80', NULL, 'SCHEDULED', '', '2026-02-27 16:06:47'),
(26, 3, 2, '2026-03-01 14:00:00', 60, 'ONLINE', 'https://meet.jit.si/TalentBridge-Interview-3-LxaccXVhgDT097bf880', NULL, 'SCHEDULED', '', '2026-02-28 13:48:37'),
(28, 1, 2, '2026-03-01 14:00:00', 60, 'ONLINE', 'https://meet.jit.si/TalentBridge-Interview-1-P7erMRCrWyCw97bf880', NULL, 'SCHEDULED', '', '2026-02-28 17:11:01'),
(29, 11, 7, '2026-03-02 13:00:00', 50, 'ONLINE', 'https://meet.jit.si/TalentBridge-Interview-11-JJwqL9KdvMRfea25480', NULL, 'SCHEDULED', '', '2026-03-01 19:23:41');

-- --------------------------------------------------------

--
-- Table structure for table `interview_feedback`
--

CREATE TABLE `interview_feedback` (
  `id` bigint(20) NOT NULL,
  `interview_id` bigint(20) NOT NULL,
  `recruiter_id` bigint(20) NOT NULL,
  `overall_score` int(11) DEFAULT NULL,
  `decision` enum('ACCEPTED','REJECTED') NOT NULL,
  `comment` text DEFAULT NULL,
  `created_at` datetime DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `interview_feedback`
--

INSERT INTO `interview_feedback` (`id`, `interview_id`, `recruiter_id`, `overall_score`, `decision`, `comment`, `created_at`) VALUES
(15, 13, 2, 87, 'ACCEPTED', 'knbvc', '2026-02-28 17:31:12'),
(16, 29, 7, 80, 'ACCEPTED', 'lkhjgfd', '2026-03-01 19:24:12');

-- --------------------------------------------------------

--
-- Table structure for table `job_application`
--

CREATE TABLE `job_application` (
  `id` bigint(20) NOT NULL,
  `offer_id` bigint(20) NOT NULL,
  `candidate_id` bigint(20) NOT NULL,
  `phone` varchar(30) DEFAULT NULL,
  `cover_letter` text DEFAULT NULL,
  `cv_path` varchar(255) DEFAULT NULL,
  `applied_at` datetime DEFAULT current_timestamp(),
  `current_status` enum('SUBMITTED','IN_REVIEW','SHORTLISTED','REJECTED','INTERVIEW','HIRED') DEFAULT 'SUBMITTED',
  `is_archived` tinyint(1) NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `job_application`
--

INSERT INTO `job_application` (`id`, `offer_id`, `candidate_id`, `phone`, `cover_letter`, `cv_path`, `applied_at`, `current_status`, `is_archived`) VALUES
(1, 1, 4, '93346608', 'Passionate Java developer', 'cv/ahmed.pdf', '2026-02-12 08:31:56', 'INTERVIEW', 0),
(2, 2, 5, '93346608', 'Experienced data analyst', 'cv/sara.pdf', '2026-02-12 08:31:56', 'SHORTLISTED', 0),
(3, 1, 6, '93346608', 'Motivated junior developer', 'cv/youssef.pdf', '2026-02-12 08:31:56', 'INTERVIEW', 0),
(5, 2, 4, '93346608', 'Ahmed Ben Ali\n93346608\nshaco54lol@gmail.com\n\nHiring Manager\nInnovateX\n\n[Date]\n\nDear Hiring Manager,\n\nI am writing to express my enthusiastic interest in the Data Analyst position at InnovateX, as advertised on [Platform where you saw the advertisement - e.g., LinkedIn, company website]. With two years of hands-on experience in data analysis and a strong command of SQL, I am confident that my skills and dedication align perfectly with the requirements of this role and the innovative spirit of your company.\n\nDuring my tenure at [Previous Company Name - if applicable, otherwise omit], I honed my ability to extract, clean, and analyze complex datasets to derive actionable insights. My advanced proficiency in SQL has enabled me to efficiently query and manipulate large volumes of data, forming the foundation for robust reporting and trend identification. I am also adept at utilizing Java to develop custom scripts and automate data processing tasks, further enhancing efficiency and accuracy. My Bachelor\'s degree has provided me with a solid theoretical understanding of analytical principles, which I have successfully applied in practical scenarios.\n\nI am particularly drawn to InnovateX\'s reputation for [Mention something specific about InnovateX that appeals to you - e.g., its groundbreaking work in X industry, its commitment to Y]. I am eager to contribute my analytical capabilities to a forward-thinking organization like yours and to collaborate with a team that is passionate about leveraging data to drive innovation.\n\nThank you for considering my application. I have attached my resume for your review and welcome the opportunity to discuss how my skills and experience can benefit InnovateX.\n\nSincerely,\nAhmed Ben Ali', '', '2026-02-27 14:54:55', 'SUBMITTED', 1),
(7, 2, 4, '53757969', 'Dear Hiring Manager at InnovateX,\n\nI am excited to apply for the Data Analyst position at InnovateX, where I can utilize my analytical skills and technical expertise to drive business growth and informed decision-making. With a strong foundation in data analysis and a passion for working with data, I am confident that I would be a valuable addition to your team.\n\nAs a detail-oriented and organized individual with a Bachelor\'s degree and 2 years of experience in data analysis, I possess a unique blend of technical and business acumen. My advanced skills in SQL have enabled me to efficiently manage and analyze large datasets, while my intermediate proficiency in Java has allowed me to develop robust data processing applications. I am eager to leverage these skills to help InnovateX gain insights and make data-driven decisions.\n\nThroughout my career, I have demonstrated a strong ability to work with stakeholders to identify business needs and develop data-driven solutions. My experience has taught me the importance of attention to detail, effective communication, and collaboration in a fast-paced environment. I am excited about the prospect of joining a team that shares my passion for innovation and data-driven decision-making.\n\nI am particularly drawn to InnovateX\'s commitment to using data to drive innovation and growth. I am impressed by the company\'s forward-thinking approach and believe that my skills and experience would be a great fit. I would welcome the opportunity to discuss my application and how I can contribute to the success of InnovateX.\n\nThank you for considering my application. I look forward to the opportunity to discuss this position further.\n\nSincerely,\nAhmed Ben Ali', '', '2026-02-27 16:23:48', 'SUBMITTED', 0),
(9, 3, 4, '93346608', 'Dear Hiring Manager at TechCorp,\n\nI am excited to apply for the IT position at TechCorp, where I can utilize my technical skills and experience to contribute to the company\'s success. With a strong foundation in computer science and a passion for innovative technologies, I am confident that I would be a valuable addition to your team.\n\nAs a highly motivated and dedicated professional with 2 years of experience in the field, I possess a unique combination of technical and analytical skills. My expertise in SQL is advanced, allowing me to efficiently manage and analyze complex databases. Additionally, my intermediate proficiency in Java enables me to develop and implement effective software solutions. I am eager to apply my skills and knowledge to drive business growth and improvement at TechCorp.\n\nThroughout my career, I have demonstrated my ability to work collaboratively in a team environment, think critically, and adapt to new technologies and challenges. My strong work ethic and attention to detail have earned me a reputation as a reliable and results-driven professional. I am excited about the opportunity to bring my skills and experience to TechCorp and contribute to the company\'s mission to deliver innovative and effective IT solutions.\n\nI am particularly drawn to TechCorp\'s commitment to excellence and innovation, and I am impressed by the company\'s cutting-edge approach to technology. I am confident that my passion for IT, combined with my technical expertise and experience, make me an ideal candidate for this role. I would welcome the opportunity to discuss my application and how I can contribute to the success of TechCorp.\n\nThank you for considering my application. I look forward to the opportunity to discuss this further.\n\nSincerely,\nAhmed Ben Ali', '', '2026-02-28 13:53:36', 'SHORTLISTED', 0),
(11, 8, 8, '53757969', 'Dear Hiring Manager at ACTIA,\n\nI am excited to apply for the DevOps position at ACTIA, where I can utilize my technical expertise and passion for innovation to drive success. With a solid educational foundation in college and 3 years of experience in the field, I am confident in my ability to make a significant impact at your esteemed organization.\n\nAs a highly motivated and dedicated professional, I have developed a strong background in DevOps practices, with a focus on bridging the gap between development and operations teams. My experience has equipped me with a unique understanding of the importance of collaboration, automation, and continuous improvement. I am well-versed in a range of tools and technologies, and I am eager to leverage my skills to optimize your company\'s systems and processes.\n\nI am particularly drawn to ACTIA\'s commitment to innovation and customer satisfaction. I am impressed by the company\'s forward-thinking approach and its dedication to staying at the forefront of the industry. I am excited about the prospect of joining a team that shares my values and is passionate about delivering high-quality solutions.\n\nIn addition to my technical expertise, I possess excellent communication and interpersonal skills, which have been essential in my previous roles. I have a proven track record of working effectively with cross-functional teams, stakeholders, and customers to deliver results-driven solutions.\n\nThank you for considering my application. I would welcome the opportunity to discuss my qualifications further and explore how I can contribute to ACTIA\'s success. Please do not hesitate to contact me at Hamadi@gmail.com or 53757969.\n\nSincerely,\nHamadi Hamadi', '', '2026-03-01 19:22:25', 'INTERVIEW', 0),
(12, 7, 8, '53757969', 'Dear Hiring Manager at TechCorp,\n\nI am excited to apply for the Mobile Developer position at TechCorp, where I can utilize my skills and experience to contribute to the development of innovative mobile solutions. With a strong educational foundation in computer science from college and three years of hands-on experience in mobile development, I am confident in my ability to make a valuable impact at your esteemed organization.\n\nThroughout my career, I have gained a solid understanding of mobile development principles, including design patterns, programming languages, and software development methodologies. My experience has equipped me with the skills to design, develop, and deploy mobile applications that are both functional and user-friendly. I am well-versed in a range of programming languages, including Java, Swift, and Kotlin, and have a strong proficiency in developing applications for both iOS and Android platforms.\n\nAs a dedicated and passionate mobile developer, I am committed to staying up-to-date with the latest trends and technologies in the field. I am excited about the prospect of joining TechCorp\'s team of talented professionals and contributing my expertise to the development of cutting-edge mobile applications. My strong work ethic, attention to detail, and excellent problem-solving skills make me an ideal candidate for this role.\n\nI would welcome the opportunity to discuss my application and how my skills and experience align with the requirements of the Mobile Developer position. Please do not hesitate to contact me at Hamadi@gmail.com or 53757969 to arrange a convenient time for a conversation. I have attached my resume, which provides further details about my qualifications and experience. Thank you for considering my application. I look forward to the opportunity to contribute to TechCorp\'s success.', '', '2026-03-01 19:27:11', 'SUBMITTED', 0);

-- --------------------------------------------------------

--
-- Table structure for table `job_offer`
--

CREATE TABLE `job_offer` (
  `id` bigint(20) NOT NULL,
  `recruiter_id` bigint(20) NOT NULL,
  `title` varchar(255) NOT NULL,
  `description` text NOT NULL,
  `location` varchar(255) DEFAULT NULL,
  `latitude` double DEFAULT NULL,
  `longitude` double DEFAULT NULL,
  `contract_type` enum('CDI','CDD','INTERNSHIP','FREELANCE','PART_TIME','FULL_TIME') NOT NULL,
  `created_at` datetime DEFAULT current_timestamp(),
  `deadline` datetime DEFAULT NULL,
  `status` enum('OPEN','CLOSED','FLAGGED') DEFAULT 'OPEN',
  `quality_score` int(11) DEFAULT NULL,
  `ai_suggestions` text DEFAULT NULL,
  `is_flagged` tinyint(1) NOT NULL DEFAULT 0,
  `flagged_at` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `job_offer`
--

INSERT INTO `job_offer` (`id`, `recruiter_id`, `title`, `description`, `location`, `latitude`, `longitude`, `contract_type`, `created_at`, `deadline`, `status`, `quality_score`, `ai_suggestions`, `is_flagged`, `flagged_at`) VALUES
(1, 2, 'Java Developer', 'Nous recherchons un Developpeur Java pour concevoir et developper des applications logicielles robustes et efficaces. Vous serez responsable du developpement de nouvelles fonctionnalites et de la maintenance de nos applications existantes. Vous travaillerez en collaboration etroite avec les equipes produit et QA pour garantir la qualite et la performance de nos logiciels. Vous participerez aux revues de code et aux choix d\'architecture technique pour améliorer nos processus de developpement. Vous utiliserez les outils et les technologies les plus recents pour developper des solutions innovantes.', 'Tunis', NULL, NULL, 'PART_TIME', '2026-02-12 08:31:38', '2026-03-30 23:59:00', 'OPEN', NULL, NULL, 0, NULL),
(2, 3, 'Data Analyst', 'Analyze business data', 'Sfax', NULL, NULL, 'CDI', '2026-02-12 08:31:38', '2026-04-15 00:00:00', 'OPEN', NULL, NULL, 0, NULL),
(3, 2, 'IT', 'kjhgv', 'tunis', NULL, NULL, 'FREELANCE', '2026-02-27 08:18:27', '2026-03-06 23:59:00', 'CLOSED', NULL, NULL, 0, NULL),
(5, 2, 'cloud security', 'Nous recherchons un(e) cloud security motive(e) pour rejoindre notre equipe. Vous contribuerez au developpement et aux projets strategiques de l\'entreprise.', 'sfax', NULL, NULL, 'INTERNSHIP', '2026-02-28 14:55:26', NULL, 'OPEN', NULL, NULL, 0, NULL),
(6, 2, 'web dev', 'Nous recherchons un developpeur web pour concevoir et developper des applications web modernes et responsives. Vous serez responsable du developpement front-end de nos plateformes en utilisant les dernieres technologies web. Vous travaillerez en collaboration etroite avec les equipes design et produit pour créer des experiences utilisateur exceptionnelles. Vous participerez aux revues de code et aux choix d\'architecture technique pour assurer la qualité et la performance de nos applications. Vous developerez également des solutions pour améliorer l\'accessibilité et la sécurité de nos sites web.', 'Tunis', NULL, NULL, 'FULL_TIME', '2026-02-28 15:32:26', '2026-03-06 23:59:00', 'OPEN', NULL, NULL, 0, NULL),
(7, 2, 'mobile dev', 'un idiotun idiotun idiotun idiotun idiotun idiot un idiot', 'tunis', NULL, NULL, 'FREELANCE', '2026-02-28 22:12:52', '2026-03-13 23:59:00', 'OPEN', NULL, NULL, 0, NULL),
(8, 7, 'devopss', 'Nous recherchons un expert DevOps pour optimiser et automatiser nos processus de developpement et de deployment. Vous serez responsable de la mise en place et de la gestion de nos outils de continuous integration et continuous deployment. Vous travaillerez en collaboration avec les equipes de developpement pour identifier les besoins et mettre en place des solutions pour améliorer la qualité et la rapidité de nos deliveries. Vous participerez également à la supervision et à l\'optimisation de nos infrastructures cloud. Vous serez chargé de garantir la sécurité et la conformité de nos environnements de production.', 'tunis', NULL, NULL, 'FREELANCE', '2026-03-01 18:07:38', '2026-03-05 23:59:00', 'OPEN', NULL, NULL, 0, NULL);

-- --------------------------------------------------------

--
-- Table structure for table `job_offer_warning`
--

CREATE TABLE `job_offer_warning` (
  `id` bigint(20) NOT NULL,
  `job_offer_id` bigint(20) NOT NULL,
  `recruiter_id` bigint(20) NOT NULL,
  `admin_id` bigint(20) NOT NULL,
  `reason` varchar(255) NOT NULL,
  `message` text NOT NULL,
  `status` enum('SENT','SEEN','RESOLVED','DISMISSED') NOT NULL DEFAULT 'SENT',
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `seen_at` datetime DEFAULT NULL,
  `resolved_at` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `job_offer_warning`
--

INSERT INTO `job_offer_warning` (`id`, `job_offer_id`, `recruiter_id`, `admin_id`, `reason`, `message`, `status`, `created_at`, `seen_at`, `resolved_at`) VALUES
(1, 5, 2, 1, 'Offre en double', 'Nous avons détecté une offre d\'emploi en double sur notre plateforme, intitulée \"cloud security\", qui semble être une répétition d\'une offre précédente. Nous vous informons de ce problème pour que vous puissiez prendre les mesures nécessaires pour le corriger. \n\nPour résoudre ce problème, nous vous recommandons de supprimer la version dupliquée de l\'offre et de vous assurer que toutes les informations sont à jour et exactes. Il est essentiel de garantir que les candidats reçoivent des informations précises et cohérentes lors de leur recherche d\'emploi.\n\nSi cette offre en double n\'est pas corrigée, cela pourrait entraîner une confusion parmi les candidats et nuire à la réputation de votre entreprise. De plus, cela pourrait également entraîner une perte de temps et de ressources pour les candidats qui postulent à la même offre plusieurs fois. Nous sommes à votre disposition pour vous aider à résoudre ce problème et à améliorer la qualité de vos offres d\'emploi. Nous vous encourageons à prendre des mesures pour éviter les offres en double à l\'avenir.', 'RESOLVED', '2026-02-28 15:15:24', '2026-02-28 15:15:37', NULL),
(2, 6, 2, 1, 'Information incomplète', 'Nous avons examiné votre offre d\'emploi pour le poste de web dev et avons constaté que certaines informations essentielles sont manquantes. Plus précisément, la description du poste ne fournit pas de détails sur les exigences spécifiques en matière de compétences, d\'expérience et de qualifications requises pour le candidat idéal. De plus, les responsabilités du poste ne sont pas entièrement détaillées, ce qui pourrait entraîner des malentendus ou des attentes non claires pour les candidats.\n\nIl est important de corriger ces points pour garantir que les candidats soient pleinement informés et puissent évaluer leur adéquation au poste de manière efficace. Si ces informations ne sont pas fournies, cela pourrait entraîner un faible taux de réponse ou des candidatures non pertinentes, ce qui pourrait prolonger le processus de recrutement et potentiellement entraîner des coûts supplémentaires.\n\nNous vous recommandons de compléter la description du poste en incluant des détails tels que les compétences techniques requises, les expériences professionnelles souhaitées et les qualifications académiques nécessaires. Cela non seulement améliorera la qualité des candidatures, mais également renforcera la crédibilité de votre entreprise en tant qu\'employeur attractif et transparent. Nous sommes à votre disposition pour vous aider à optimiser votre offre d\'emploi et vous assurer que vous attirez les meilleurs talents pour ce poste.', 'RESOLVED', '2026-02-28 17:51:12', '2026-02-28 17:51:27', NULL),
(3, 7, 2, 1, 'Contenu potentiellement non conforme', 'L\'offre semble contenir du contenu offensant.\nToxicité: 0.77, Insulte: 0.75, Menaces: 0.01, Haine: 0.02\nMerci de corriger la description sinon elle sera supprimée.', 'RESOLVED', '2026-02-28 22:13:59', '2026-02-28 22:14:29', NULL);

-- --------------------------------------------------------

--
-- Table structure for table `offer_skill`
--

CREATE TABLE `offer_skill` (
  `id` bigint(20) NOT NULL,
  `offer_id` bigint(20) NOT NULL,
  `skill_name` varchar(100) NOT NULL,
  `level_required` enum('BEGINNER','INTERMEDIATE','ADVANCED') NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `offer_skill`
--

INSERT INTO `offer_skill` (`id`, `offer_id`, `skill_name`, `level_required`) VALUES
(3, 2, 'Python', 'INTERMEDIATE'),
(4, 2, 'Statistics', 'BEGINNER'),
(10, 5, 'Communication', 'INTERMEDIATE'),
(11, 5, 'Travail en equipe', 'INTERMEDIATE'),
(12, 5, 'Organisation', 'INTERMEDIATE'),
(13, 5, 'Adaptabilite', 'INTERMEDIATE'),
(14, 5, 'Rigueur', 'INTERMEDIATE'),
(15, 6, 'HTML5', 'INTERMEDIATE'),
(16, 6, 'CSS3', 'INTERMEDIATE'),
(17, 6, 'JavaScript', 'INTERMEDIATE'),
(18, 6, 'Angular', 'INTERMEDIATE'),
(19, 6, 'Vue.js', 'INTERMEDIATE'),
(20, 6, 'Bootstrap', 'INTERMEDIATE'),
(21, 6, 'Webpack', 'INTERMEDIATE'),
(22, 6, 'GraphQL', 'INTERMEDIATE'),
(23, 1, 'Java 11', 'INTERMEDIATE'),
(24, 1, 'Spring Boot', 'INTERMEDIATE'),
(25, 1, 'Hibernate', 'INTERMEDIATE'),
(26, 1, 'Maven', 'INTERMEDIATE'),
(27, 1, 'Git', 'INTERMEDIATE'),
(28, 1, 'JUnit', 'INTERMEDIATE'),
(29, 1, 'REST API', 'INTERMEDIATE'),
(30, 1, 'Jakarta EE', 'INTERMEDIATE'),
(39, 7, 'Java', 'INTERMEDIATE'),
(40, 7, 'Swift', 'INTERMEDIATE'),
(41, 7, 'Kotlin', 'INTERMEDIATE'),
(42, 7, 'React Native', 'INTERMEDIATE'),
(43, 7, 'Android SDK', 'INTERMEDIATE'),
(44, 7, 'iOS SDK', 'INTERMEDIATE'),
(45, 7, 'Flutter', 'INTERMEDIATE'),
(46, 7, 'Git', 'INTERMEDIATE'),
(55, 8, 'Jenkins', 'INTERMEDIATE'),
(56, 8, 'Docker', 'INTERMEDIATE'),
(57, 8, 'Kubernetes', 'INTERMEDIATE'),
(58, 8, 'Ansible', 'INTERMEDIATE'),
(59, 8, 'AWS', 'INTERMEDIATE'),
(60, 8, 'Terraform', 'INTERMEDIATE'),
(61, 8, 'Prometheus', 'INTERMEDIATE'),
(62, 8, 'Git', 'INTERMEDIATE');

-- --------------------------------------------------------

--
-- Table structure for table `recruiter`
--

CREATE TABLE `recruiter` (
  `id` bigint(20) NOT NULL,
  `user_id` bigint(20) DEFAULT NULL,
  `company_name` varchar(255) NOT NULL,
  `company_location` varchar(255) DEFAULT NULL,
  `company_description` text DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `recruiter`
--

INSERT INTO `recruiter` (`id`, `user_id`, `company_name`, `company_location`, `company_description`) VALUES
(2, 2, 'TechCorp', 'Tunis', NULL),
(3, 3, 'InnovateX', 'Sfax', NULL),
(7, 7, 'ACTIA', 'Ghazela centre, نهج الأنصار, المدينة الفاضلة, معتمدية رواد, ولاية أريانة, 2083, تونس', NULL),
(9, NULL, 'actia', 'Ghazela centre, نهج الأنصار, المدينة الفاضلة, معتمدية رواد, ولاية أريانة, 2083, تونس', NULL);

-- --------------------------------------------------------

--
-- Table structure for table `recruitment_event`
--

CREATE TABLE `recruitment_event` (
  `id` bigint(20) NOT NULL,
  `recruiter_id` bigint(20) NOT NULL,
  `title` varchar(255) NOT NULL,
  `description` text DEFAULT NULL,
  `event_type` varchar(255) DEFAULT NULL,
  `location` varchar(255) DEFAULT NULL,
  `event_date` datetime NOT NULL,
  `capacity` int(11) DEFAULT 0,
  `meet_link` varchar(255) DEFAULT NULL,
  `created_at` datetime DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `recruitment_event`
--

INSERT INTO `recruitment_event` (`id`, `recruiter_id`, `title`, `description`, `event_type`, `location`, `event_date`, `capacity`, `meet_link`, `created_at`) VALUES
(1, 2, 'Tech Job Fair', 'Meet top companies', 'JOB_FAIR', 'Tunis', '2026-03-10 09:00:00', 100, NULL, '2026-02-12 08:32:33'),
(2, 3, 'Data Webinar', 'Online data science session', 'WEBINAR', NULL, '2026-03-15 18:00:00', 200, NULL, '2026-02-12 08:32:33'),
(4, 2, 'cvcb', 'swdxfghjklmjkgjghfdfsdfgdhjkhl', 'Job_Faire', 'tunis', '2026-03-03 00:00:00', 5, '', '2026-03-01 00:38:54'),
(6, 2, 'lkjhg', 'Rejoignez-nous pour un webinaire exceptionnel à الفجة, معتمدية المرناقية, ولاية منوبة, تونس, où vous découvrirez des opportunités de carrière passionnantes et en constante évolution. Vous aurez l\'occasion de rencontrer nos équipes et de discuter de vos ambitions professionnelles dans un environnement convivial et stimulant. Nous vous proposons une expérience unique pour vous aider à atteindre vos objectifs et à évoluer dans votre carrière, avec des conseils pratiques et des conseils d\'experts pour vous aider à réussir. Cette rencontre est l\'occasion idéale pour vous de vous informer et de vous inspirer pour votre avenir professionnel.', 'WEBINAIRE', 'الفجة, معتمدية المرناقية, ولاية منوبة, تونس', '2026-03-10 00:00:00', 30, 'youtube.com', '2026-03-01 10:35:44'),
(7, 2, 'knkjbhvgcfxds', 'Rejoignez-nous pour une journée de découverte et d\'échange exceptionnelle, où vous pourrez rencontrer nos équipes et découvrir les opportunités de carrière les plus excitantes. Vous aurez l\'occasion de vous informer sur nos valeurs, notre culture d\'entreprise et nos projets innovants, tout en bénéficiant de conseils personnalisés pour réussir votre parcours professionnel. Nous vous offrons une chance unique de vous connecter avec des professionnels passionnés et de faire partie d\'une communauté dynamique et en constante évolution. Notre équipe est impatiente de vous accueillir et de partager avec vous les défis et les réussites de notre entreprise.', 'Job_Faire', 'بوعاتي محمود, الجزائر', '2026-03-24 00:00:00', 345, '', '2026-03-01 12:57:36'),
(8, 2, 'LJHUIYGTF', 'Rejoignez-nous à San Michele di Ganzaria, en Italie, pour une expérience unique et enrichissante qui vous permettra de découvrir de nouvelles opportunités de carrière et de rencontrer des professionnels passionnés. Vous aurez l\'occasion de présenter vos compétences et vos expériences, et de discuter avec nos équipes pour trouver le poste qui vous convient le mieux. Nous vous offrons un environnement accueillant et dynamique où vous pourrez vous exprimer et vous développer, avec des possibilités de croissance et d\'évolution de carrière exceptionnelles. Cette rencontre est l\'occasion idéale de faire partie d\'une équipe innovante et de contribuer à la réussite de notre entreprise.', 'Interview day', 'San Michele di Ganzaria, إيطاليا', '2026-03-18 00:00:00', 12, '', '2026-03-01 12:57:59'),
(9, 2, 'cloud job fair', 'Rejoignez-nous pour une journée de découverte et d\'opportunités professionnelles dans le domaine du cloud computing à Gèsigu/Gesico, en Italie. Vous aurez l\'occasion de rencontrer des entreprises leaders dans leur domaine et de discuter des postes vacants qui correspondent à vos compétences et à vos aspirations. Les professionnels du secteur seront également présents pour partager leurs expériences et offrir des conseils précieux pour une carrière réussie dans le cloud. Cette foire de l\'emploi est l\'endroit idéal pour établir des connections, apprendre les dernières tendances et trouver le poste de vos rêves.', 'Job_Faire', 'Gèsigu/Gesico, إيطاليا', '2026-03-12 00:00:00', 12, '', '2026-03-01 12:59:01'),
(10, 2, 'QSDQDQD', 'Rejoignez-nous pour une expérience unique et enrichissante à Saint-Amand-Montrond, en France, où vous découvrirez des opportunités de carrière exceptionnelles et rencontrerez des professionnels passionnés. Ce webinaire est l\'occasion idéale de vous informer sur les dernières tendances et innovations dans le domaine, tout en établissant des connections précieuses avec des experts et des futurs collègues. Vous aurez l\'opportunité de présenter vos compétences, vos passions et vos aspirations, et de découvrir comment vous pouvez contribuer à notre équipe dynamique et motivée. Nous vous offrons un espace de dialogue ouvert et convivial pour explorer vos objectifs de carrière et trouver le poste qui vous correspond parfaitement.', 'WEBINAIRE', 'Saint-Amand-Montrond, فرنسا', '2026-03-05 00:00:00', 123, 'youtube.com', '2026-03-01 12:59:40'),
(11, 7, 'qsqsd', 'Rejoignez-nous à Tunis pour un événement incontournable de recrutement où vous pourrez découvrir des opportunités de carrière variées et rencontrer des employeurs de premier plan. Vous aurez l\'occasion de présenter votre candidature, de discuter avec des professionnels du secteur et de vous informer sur les dernières tendances du marché du travail. Les visiteurs pourront également participer à des ateliers de développement de carrière et des séances de conseil en recherche d\'emploi pour améliorer leurs compétences et augmenter leurs chances de réussite. C\'est l\'endroit idéal pour établir des contacts, apprendre et grandir professionnellement.', 'Job_Faire', 'tunis', '2026-02-27 00:00:00', 12, '', '2026-03-01 18:59:30');

-- --------------------------------------------------------

--
-- Table structure for table `users`
--

CREATE TABLE `users` (
  `id` bigint(20) NOT NULL,
  `email` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL,
  `first_name` varchar(100) DEFAULT NULL,
  `last_name` varchar(100) DEFAULT NULL,
  `phone` varchar(30) DEFAULT NULL,
  `is_active` tinyint(1) DEFAULT 1,
  `created_at` datetime DEFAULT current_timestamp(),
  `forget_code` varchar(10) DEFAULT NULL,
  `forget_code_expires` datetime DEFAULT NULL,
  `face_person_id` varchar(128) DEFAULT NULL,
  `face_enabled` tinyint(1) NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `users`
--

INSERT INTO `users` (`id`, `email`, `password`, `first_name`, `last_name`, `phone`, `is_active`, `created_at`, `forget_code`, `forget_code_expires`, `face_person_id`, `face_enabled`) VALUES
(1, 'admin@gmail.com', 'admin123', 'Super', 'Admin', '93346608', 1, '2026-02-12 08:30:10', NULL, NULL, NULL, 0),
(2, 'recruiter1@company.com', 'rec123', 'Alice', 'Martin', '93346608', 1, '2026-02-12 08:30:10', NULL, NULL, NULL, 0),
(3, 'recruiter2@company.com', 'rec123', 'Bob', 'Durand', '93346608', 1, '2026-02-12 08:30:10', NULL, NULL, NULL, 0),
(4, 'mlkjhgf@gmail.com', 'cand123', 'Ahmed', 'Ben Ali', '93346608', 1, '2026-02-12 08:30:10', NULL, NULL, NULL, 0),
(5, 'zex54lol@gmail.com', 'cand123', 'Sara', 'Trabelsi', '93346608', 1, '2026-02-12 08:30:10', NULL, NULL, NULL, 0),
(6, 'candidate3@gmail.com', 'cand123', 'Youssef', 'Haddad', '93346608', 1, '2026-02-12 08:30:10', NULL, NULL, NULL, 0),
(7, 'faresmanai05@gmail.com', '$2a$12$ZMggIVVde2KpURkYtLV/8.ehHWUJf11pouYWLt1VHqzQlSMvyDhFW', 'hyujnn', 'hyujnn', '53757969', 1, '2026-03-01 16:57:17', NULL, NULL, NULL, 0),
(8, 'Hamadi@gmail.com', '$2a$12$wKGaGTbfzZ5YYgHnA2kmqO/jDALHJBkQLx2x8aV2.vp6uLuvZ/LIm', 'Hamadi', 'Hamadi', '53757969', 1, '2026-03-01 18:10:25', NULL, NULL, NULL, 0),
(9, 'shaco54lol@gmail.com', '$2a$12$nInDLm0e4mLukCUs9T18OeYIFbb.eHt/v0o5931jsB2zsRWLvHeZG', 'mohamed', 'ben moussa', '53757969', 1, '2026-03-01 22:04:37', '445292', '2026-03-01 21:18:05', NULL, 0),
(10, 'superadmin@rh.com', '$2a$12$wlTJMvbbsiwyoquTSeumfugPrzNGvezC90kl0NDItmeT4cMfsZkUK', 'Super', 'Admin', '00000000', 1, '2026-03-01 22:26:29', NULL, NULL, NULL, 0);

-- --------------------------------------------------------

--
-- Table structure for table `warning_correction`
--

CREATE TABLE `warning_correction` (
  `id` bigint(20) NOT NULL,
  `warning_id` bigint(20) NOT NULL,
  `job_offer_id` bigint(20) NOT NULL,
  `recruiter_id` bigint(20) NOT NULL,
  `correction_note` text DEFAULT NULL,
  `old_title` varchar(255) DEFAULT NULL,
  `new_title` varchar(255) DEFAULT NULL,
  `old_description` text DEFAULT NULL,
  `new_description` text DEFAULT NULL,
  `status` enum('PENDING','APPROVED','REJECTED') DEFAULT 'PENDING',
  `submitted_at` datetime DEFAULT current_timestamp(),
  `reviewed_at` datetime DEFAULT NULL,
  `admin_note` text DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `warning_correction`
--

INSERT INTO `warning_correction` (`id`, `warning_id`, `job_offer_id`, `recruiter_id`, `correction_note`, `old_title`, `new_title`, `old_description`, `new_description`, `status`, `submitted_at`, `reviewed_at`, `admin_note`) VALUES
(1, 1, 5, 2, 'Nous avons procede aux corrections necessaires suite au signalement (Offre en double). L\'offre a ete revue et mise a jour pour repondre aux exigences de la plateforme.', NULL, 'cloud security', NULL, 'Nous recherchons un(e) cloud security motive(e) pour rejoindre notre equipe. Vous contribuerez au developpement et aux projets strategiques de l\'entreprise.', 'APPROVED', '2026-02-28 15:15:50', '2026-02-28 15:16:25', 'Correction approuvée'),
(2, 2, 6, 2, 'Nous avons procede aux corrections necessaires suite au signalement (Information incomplète). L\'offre a ete revue et mise a jour pour repondre aux exigences de la plateforme.', NULL, 'web dev', NULL, 'Nous recherchons un developpeur web pour concevoir et developper des applications web modernes et responsives. Vous serez responsable du developpement front-end de nos plateformes en utilisant les dernieres technologies web. Vous travaillerez en collaboration etroite avec les equipes design et produit pour créer des experiences utilisateur exceptionnelles. Vous participerez aux revues de code et aux choix d\'architecture technique pour assurer la qualité et la performance de nos applications. Vous developerez également des solutions pour améliorer l\'accessibilité et la sécurité de nos sites web.', 'APPROVED', '2026-02-28 17:51:34', '2026-02-28 17:51:50', 'Correction approuvée'),
(3, 3, 7, 2, 'Nous avons procede aux corrections necessaires suite au signalement (Contenu potentiellement non conforme). L\'offre a ete revue et mise a jour pour repondre aux exigences de la plateforme.', NULL, 'mobile dev', NULL, 'un idiotun idiotun idiotun idiotun idiotun idiot un idiot', 'APPROVED', '2026-02-28 22:14:36', '2026-02-28 22:15:38', 'Correction approuvée');

--
-- Indexes for dumped tables
--

--
-- Indexes for table `admin`
--
ALTER TABLE `admin`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `application_status_history`
--
ALTER TABLE `application_status_history`
  ADD PRIMARY KEY (`id`),
  ADD KEY `application_id` (`application_id`),
  ADD KEY `changed_by` (`changed_by`);

--
-- Indexes for table `candidate`
--
ALTER TABLE `candidate`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `candidate_skill`
--
ALTER TABLE `candidate_skill`
  ADD PRIMARY KEY (`id`),
  ADD KEY `candidate_id` (`candidate_id`);

--
-- Indexes for table `event_registration`
--
ALTER TABLE `event_registration`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `event_id` (`event_id`,`candidate_id`),
  ADD KEY `candidate_id` (`candidate_id`);

--
-- Indexes for table `event_review`
--
ALTER TABLE `event_review`
  ADD PRIMARY KEY (`id`),
  ADD KEY `fk_event_review_event` (`event_id`),
  ADD KEY `fk_event_review_candidate` (`candidate_id`);

--
-- Indexes for table `interview`
--
ALTER TABLE `interview`
  ADD PRIMARY KEY (`id`),
  ADD KEY `application_id` (`application_id`),
  ADD KEY `recruiter_id` (`recruiter_id`);

--
-- Indexes for table `interview_feedback`
--
ALTER TABLE `interview_feedback`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `interview_id` (`interview_id`),
  ADD KEY `recruiter_id` (`recruiter_id`);

--
-- Indexes for table `job_application`
--
ALTER TABLE `job_application`
  ADD PRIMARY KEY (`id`),
  ADD KEY `offer_id` (`offer_id`),
  ADD KEY `candidate_id` (`candidate_id`);

--
-- Indexes for table `job_offer`
--
ALTER TABLE `job_offer`
  ADD PRIMARY KEY (`id`),
  ADD KEY `recruiter_id` (`recruiter_id`);

--
-- Indexes for table `job_offer_warning`
--
ALTER TABLE `job_offer_warning`
  ADD PRIMARY KEY (`id`),
  ADD KEY `fk_warn_offer` (`job_offer_id`),
  ADD KEY `fk_warn_recruiter` (`recruiter_id`),
  ADD KEY `fk_warn_admin` (`admin_id`);

--
-- Indexes for table `offer_skill`
--
ALTER TABLE `offer_skill`
  ADD PRIMARY KEY (`id`),
  ADD KEY `offer_id` (`offer_id`);

--
-- Indexes for table `recruiter`
--
ALTER TABLE `recruiter`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `recruitment_event`
--
ALTER TABLE `recruitment_event`
  ADD PRIMARY KEY (`id`),
  ADD KEY `recruiter_id` (`recruiter_id`);

--
-- Indexes for table `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `email` (`email`);

--
-- Indexes for table `warning_correction`
--
ALTER TABLE `warning_correction`
  ADD PRIMARY KEY (`id`),
  ADD KEY `fk_correction_warning` (`warning_id`),
  ADD KEY `fk_correction_job` (`job_offer_id`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `application_status_history`
--
ALTER TABLE `application_status_history`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=45;

--
-- AUTO_INCREMENT for table `candidate_skill`
--
ALTER TABLE `candidate_skill`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=24;

--
-- AUTO_INCREMENT for table `event_registration`
--
ALTER TABLE `event_registration`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=28;

--
-- AUTO_INCREMENT for table `event_review`
--
ALTER TABLE `event_review`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT for table `interview`
--
ALTER TABLE `interview`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=30;

--
-- AUTO_INCREMENT for table `interview_feedback`
--
ALTER TABLE `interview_feedback`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=17;

--
-- AUTO_INCREMENT for table `job_application`
--
ALTER TABLE `job_application`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=13;

--
-- AUTO_INCREMENT for table `job_offer`
--
ALTER TABLE `job_offer`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=9;

--
-- AUTO_INCREMENT for table `job_offer_warning`
--
ALTER TABLE `job_offer_warning`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT for table `offer_skill`
--
ALTER TABLE `offer_skill`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=63;

--
-- AUTO_INCREMENT for table `recruitment_event`
--
ALTER TABLE `recruitment_event`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=12;

--
-- AUTO_INCREMENT for table `users`
--
ALTER TABLE `users`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=11;

--
-- AUTO_INCREMENT for table `warning_correction`
--
ALTER TABLE `warning_correction`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `admin`
--
ALTER TABLE `admin`
  ADD CONSTRAINT `admin_ibfk_1` FOREIGN KEY (`id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `application_status_history`
--
ALTER TABLE `application_status_history`
  ADD CONSTRAINT `application_status_history_ibfk_1` FOREIGN KEY (`application_id`) REFERENCES `job_application` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `application_status_history_ibfk_2` FOREIGN KEY (`changed_by`) REFERENCES `users` (`id`);

--
-- Constraints for table `candidate`
--
ALTER TABLE `candidate`
  ADD CONSTRAINT `candidate_ibfk_1` FOREIGN KEY (`id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `candidate_skill`
--
ALTER TABLE `candidate_skill`
  ADD CONSTRAINT `candidate_skill_ibfk_1` FOREIGN KEY (`candidate_id`) REFERENCES `candidate` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `event_registration`
--
ALTER TABLE `event_registration`
  ADD CONSTRAINT `event_registration_ibfk_1` FOREIGN KEY (`event_id`) REFERENCES `recruitment_event` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `event_registration_ibfk_2` FOREIGN KEY (`candidate_id`) REFERENCES `candidate` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `event_review`
--
ALTER TABLE `event_review`
  ADD CONSTRAINT `fk_event_review_candidate` FOREIGN KEY (`candidate_id`) REFERENCES `candidate` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_event_review_event` FOREIGN KEY (`event_id`) REFERENCES `recruitment_event` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `interview`
--
ALTER TABLE `interview`
  ADD CONSTRAINT `interview_ibfk_1` FOREIGN KEY (`application_id`) REFERENCES `job_application` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `interview_ibfk_2` FOREIGN KEY (`recruiter_id`) REFERENCES `recruiter` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `interview_feedback`
--
ALTER TABLE `interview_feedback`
  ADD CONSTRAINT `interview_feedback_ibfk_1` FOREIGN KEY (`interview_id`) REFERENCES `interview` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `interview_feedback_ibfk_2` FOREIGN KEY (`recruiter_id`) REFERENCES `recruiter` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `job_application`
--
ALTER TABLE `job_application`
  ADD CONSTRAINT `job_application_ibfk_1` FOREIGN KEY (`offer_id`) REFERENCES `job_offer` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `job_application_ibfk_2` FOREIGN KEY (`candidate_id`) REFERENCES `candidate` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `job_offer`
--
ALTER TABLE `job_offer`
  ADD CONSTRAINT `job_offer_ibfk_1` FOREIGN KEY (`recruiter_id`) REFERENCES `recruiter` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `job_offer_warning`
--
ALTER TABLE `job_offer_warning`
  ADD CONSTRAINT `fk_warn_admin` FOREIGN KEY (`admin_id`) REFERENCES `admin` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_warn_offer` FOREIGN KEY (`job_offer_id`) REFERENCES `job_offer` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_warn_recruiter` FOREIGN KEY (`recruiter_id`) REFERENCES `recruiter` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `offer_skill`
--
ALTER TABLE `offer_skill`
  ADD CONSTRAINT `offer_skill_ibfk_1` FOREIGN KEY (`offer_id`) REFERENCES `job_offer` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `recruiter`
--
ALTER TABLE `recruiter`
  ADD CONSTRAINT `recruiter_ibfk_1` FOREIGN KEY (`id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `recruitment_event`
--
ALTER TABLE `recruitment_event`
  ADD CONSTRAINT `recruitment_event_ibfk_1` FOREIGN KEY (`recruiter_id`) REFERENCES `recruiter` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `warning_correction`
--
ALTER TABLE `warning_correction`
  ADD CONSTRAINT `fk_correction_job` FOREIGN KEY (`job_offer_id`) REFERENCES `job_offer` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_correction_warning` FOREIGN KEY (`warning_id`) REFERENCES `job_offer_warning` (`id`) ON DELETE CASCADE;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
