package Services.joboffers;

import Models.joboffers.WarningCorrection;
import Models.joboffers.WarningCorrection.CorrectionStatus;
import Models.joboffers.Status;
import Utils.MyDatabase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service pour gÃ©rer les corrections soumises par les recruteurs
 */
public class WarningCorrectionService {

    public WarningCorrectionService() {
        // No cached connection - always use conn() to get a live connection
        createTableIfNotExists();
    }

    private Connection conn() {
        return MyDatabase.getInstance().getConnection();
    }

    /**
     * CrÃ©e la table si elle n'existe pas
     */
    private void createTableIfNotExists() {
        String createTableSQL = """
            CREATE TABLE IF NOT EXISTS warning_correction (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                warning_id BIGINT NOT NULL,
                job_offer_id BIGINT NOT NULL,
                recruiter_id BIGINT NOT NULL,
                correction_note TEXT,
                old_title VARCHAR(255),
                new_title VARCHAR(255),
                old_description TEXT,
                new_description TEXT,
                status ENUM('PENDING', 'APPROVED', 'REJECTED') DEFAULT 'PENDING',
                submitted_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                reviewed_at DATETIME NULL,
                admin_note TEXT,
                CONSTRAINT fk_correction_warning FOREIGN KEY (warning_id) REFERENCES job_offer_warning(id) ON DELETE CASCADE,
                CONSTRAINT fk_correction_job FOREIGN KEY (job_offer_id) REFERENCES job_offer(id) ON DELETE CASCADE
            ) ENGINE=InnoDB
            """;

        try (Statement stmt = conn().createStatement()) {
            stmt.execute(createTableSQL);
        } catch (SQLException e) {
            System.err.println("Erreur crÃ©ation table warning_correction: " + e.getMessage());
        }
    }

    /**
     * Soumettre une correction
     */
    public WarningCorrection submitCorrection(WarningCorrection correction) throws SQLException {
        String query = """
            INSERT INTO warning_correction 
            (warning_id, job_offer_id, recruiter_id, correction_note, old_title, new_title, 
             old_description, new_description, status, submitted_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (PreparedStatement ps = conn().prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, correction.getWarningId());
            ps.setLong(2, correction.getJobOfferId());
            ps.setLong(3, correction.getRecruiterId());
            ps.setString(4, correction.getCorrectionNote());
            ps.setString(5, correction.getOldTitle());
            ps.setString(6, correction.getNewTitle());
            ps.setString(7, correction.getOldDescription());
            ps.setString(8, correction.getNewDescription());
            ps.setString(9, correction.getStatus().name());
            ps.setTimestamp(10, Timestamp.valueOf(correction.getSubmittedAt()));

            int affectedRows = ps.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        correction.setId(generatedKeys.getLong(1));
                    }
                }

                // Mettre Ã  jour le statut du warning Ã  SEEN (en attente de validation admin)
                updateWarningStatus(correction.getWarningId(), "SEEN");
            }

            return correction;
        }
    }

    /**
     * Met Ã  jour le statut d'un warning
     */
    private void updateWarningStatus(Long warningId, String status) throws SQLException {
        String query = "UPDATE job_offer_warning SET status = ? WHERE id = ?";
        try (PreparedStatement ps = conn().prepareStatement(query)) {
            ps.setString(1, status);
            ps.setLong(2, warningId);
            ps.executeUpdate();
        }
    }

    /**
     * Approuver une correction (admin)
     */
    public boolean approveCorrection(Long correctionId, String adminNote) throws SQLException {
        // Mettre Ã  jour la correction
        String updateCorrectionQuery = """
            UPDATE warning_correction 
            SET status = ?, reviewed_at = ?, admin_note = ?
            WHERE id = ?
            """;

        try (PreparedStatement ps = conn().prepareStatement(updateCorrectionQuery)) {
            ps.setString(1, CorrectionStatus.APPROVED.name());
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(3, adminNote);
            ps.setLong(4, correctionId);

            int updated = ps.executeUpdate();

            if (updated > 0) {
                // RÃ©cupÃ©rer la correction pour avoir les IDs
                WarningCorrection correction = getCorrectionById(correctionId);
                if (correction != null) {
                    // Marquer le warning comme rÃ©solu
                    updateWarningStatus(correction.getWarningId(), "RESOLVED");

                    // Remettre l'offre en ligne (OPEN et non flagged)
                    unflagAndReopenJobOffer(correction.getJobOfferId());
                }
                return true;
            }
            return false;
        }
    }

    /**
     * Rejeter une correction (admin)
     */
    public boolean rejectCorrection(Long correctionId, String adminNote) throws SQLException {
        String query = """
            UPDATE warning_correction 
            SET status = ?, reviewed_at = ?, admin_note = ?
            WHERE id = ?
            """;

        try (PreparedStatement ps = conn().prepareStatement(query)) {
            ps.setString(1, CorrectionStatus.REJECTED.name());
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(3, adminNote);
            ps.setLong(4, correctionId);

            return ps.executeUpdate() > 0;
        }
    }

    /**
     * DÃ©flaguer et rouvrir une offre
     */
    private void unflagAndReopenJobOffer(Long jobOfferId) throws SQLException {
        String query = "UPDATE job_offer SET is_flagged = 0, flagged_at = NULL, status = ? WHERE id = ?";
        try (PreparedStatement ps = conn().prepareStatement(query)) {
            ps.setString(1, Status.OPEN.name());
            ps.setLong(2, jobOfferId);
            ps.executeUpdate();
        }
    }

    /**
     * RÃ©cupÃ©rer une correction par ID
     */
    public WarningCorrection getCorrectionById(Long id) throws SQLException {
        String query = "SELECT * FROM warning_correction WHERE id = ?";

        try (PreparedStatement ps = conn().prepareStatement(query)) {
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapResultSetToCorrection(rs);
            }
        }
        return null;
    }

    /**
     * RÃ©cupÃ©rer toutes les corrections en attente (pour admin)
     */
    public List<WarningCorrection> getPendingCorrections() throws SQLException {
        List<WarningCorrection> corrections = new ArrayList<>();
        String query = "SELECT * FROM warning_correction WHERE status = ? ORDER BY submitted_at DESC";

        try (PreparedStatement ps = conn().prepareStatement(query)) {
            ps.setString(1, CorrectionStatus.PENDING.name());
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                corrections.add(mapResultSetToCorrection(rs));
            }
        }
        return corrections;
    }

    /**
     * Compter les corrections en attente
     */
    public int countPendingCorrections() throws SQLException {
        String query = "SELECT COUNT(*) FROM warning_correction WHERE status = ?";

        try (PreparedStatement ps = conn().prepareStatement(query)) {
            ps.setString(1, CorrectionStatus.PENDING.name());
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    /**
     * RÃ©cupÃ©rer les corrections pour un warning spÃ©cifique
     */
    public List<WarningCorrection> getCorrectionsByWarningId(Long warningId) throws SQLException {
        List<WarningCorrection> corrections = new ArrayList<>();
        String query = "SELECT * FROM warning_correction WHERE warning_id = ? ORDER BY submitted_at DESC";

        try (PreparedStatement ps = conn().prepareStatement(query)) {
            ps.setLong(1, warningId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                corrections.add(mapResultSetToCorrection(rs));
            }
        }
        return corrections;
    }

    /**
     * RÃ©cupÃ©rer les corrections pour une offre spÃ©cifique
     */
    public List<WarningCorrection> getCorrectionsByJobOfferId(Long jobOfferId) throws SQLException {
        List<WarningCorrection> corrections = new ArrayList<>();
        String query = "SELECT * FROM warning_correction WHERE job_offer_id = ? ORDER BY submitted_at DESC";

        try (PreparedStatement ps = conn().prepareStatement(query)) {
            ps.setLong(1, jobOfferId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                corrections.add(mapResultSetToCorrection(rs));
            }
        }
        return corrections;
    }

    /**
     * Mapper ResultSet vers WarningCorrection
     */
    private WarningCorrection mapResultSetToCorrection(ResultSet rs) throws SQLException {
        WarningCorrection correction = new WarningCorrection();
        correction.setId(rs.getLong("id"));
        correction.setWarningId(rs.getLong("warning_id"));
        correction.setJobOfferId(rs.getLong("job_offer_id"));
        correction.setRecruiterId(rs.getLong("recruiter_id"));
        correction.setCorrectionNote(rs.getString("correction_note"));
        correction.setOldTitle(rs.getString("old_title"));
        correction.setNewTitle(rs.getString("new_title"));
        correction.setOldDescription(rs.getString("old_description"));
        correction.setNewDescription(rs.getString("new_description"));
        correction.setStatus(CorrectionStatus.valueOf(rs.getString("status")));

        Timestamp submittedAt = rs.getTimestamp("submitted_at");
        if (submittedAt != null) {
            correction.setSubmittedAt(submittedAt.toLocalDateTime());
        }

        Timestamp reviewedAt = rs.getTimestamp("reviewed_at");
        if (reviewedAt != null) {
            correction.setReviewedAt(reviewedAt.toLocalDateTime());
        }

        correction.setAdminNote(rs.getString("admin_note"));

        return correction;
    }
}

