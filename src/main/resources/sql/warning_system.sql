-- =====================================================
-- Script SQL pour le système d'avertissement des offres
-- Structure correspondant à la base de données existante
-- =====================================================

-- 1. Modifier la colonne status de job_offer pour ajouter FLAGGED
ALTER TABLE job_offer MODIFY status ENUM('OPEN','CLOSED','FLAGGED') DEFAULT 'OPEN';

-- 2. Ajouter les colonnes is_flagged et flagged_at à job_offer
ALTER TABLE job_offer
ADD COLUMN is_flagged TINYINT(1) NOT NULL DEFAULT 0,
ADD COLUMN flagged_at DATETIME NULL;

-- 3. Table job_offer_warning (déjà créée selon votre structure)
-- CREATE TABLE job_offer_warning (
--   id BIGINT AUTO_INCREMENT PRIMARY KEY,
--   job_offer_id BIGINT NOT NULL,
--   recruiter_id BIGINT NOT NULL,
--   admin_id BIGINT NOT NULL,
--   reason VARCHAR(255) NOT NULL,
--   message TEXT NOT NULL,
--   status ENUM('SENT','SEEN','RESOLVED','DISMISSED') NOT NULL DEFAULT 'SENT',
--   created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
--   seen_at DATETIME NULL,
--   resolved_at DATETIME NULL,
--   CONSTRAINT fk_warn_offer
--     FOREIGN KEY (job_offer_id) REFERENCES job_offer(id)
--     ON DELETE CASCADE ON UPDATE CASCADE,
--   CONSTRAINT fk_warn_recruiter
--     FOREIGN KEY (recruiter_id) REFERENCES recruiter(id)
--     ON DELETE CASCADE ON UPDATE CASCADE,
--   CONSTRAINT fk_warn_admin
--     FOREIGN KEY (admin_id) REFERENCES admin(id)
--     ON DELETE CASCADE ON UPDATE CASCADE
-- ) ENGINE=InnoDB;

-- =====================================================
-- Requêtes utiles
-- =====================================================

-- Voir toutes les offres signalées
-- SELECT * FROM job_offer WHERE is_flagged = 1;

-- Voir tous les avertissements en attente (SENT ou SEEN)
-- SELECT * FROM job_offer_warning WHERE status IN ('SENT', 'SEEN');

-- Compter les avertissements par recruteur
-- SELECT recruiter_id, COUNT(*) as warning_count
-- FROM job_offer_warning
-- WHERE status IN ('SENT', 'SEEN')
-- GROUP BY recruiter_id;

-- Voir les avertissements avec les détails de l'offre
-- SELECT w.*, j.title as job_title
-- FROM job_offer_warning w
-- JOIN job_offer j ON w.job_offer_id = j.id
-- ORDER BY w.created_at DESC;


